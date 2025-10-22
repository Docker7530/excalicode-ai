package com.excalicode.platform.core.support;

import java.time.Duration;

/**
 * Simple token bucket rate limiter that guards AI calls against provider quotas.
 */
public final class TokenBucketRateLimiter {

    private static final double MIN_PERMIT = 1.0d;

    private final Object monitor = new Object();
    private final double capacity;
    private final double refillTokensPerNano;

    private double availableTokens;
    private long lastRefillNanos;

    public TokenBucketRateLimiter(int permitsPerPeriod, Duration period) {
        if (permitsPerPeriod <= 0) {
            throw new IllegalArgumentException("permitsPerPeriod must be greater than 0");
        }
        if (period == null || period.isNegative() || period.isZero()) {
            throw new IllegalArgumentException("period must be a positive duration");
        }

        this.capacity = permitsPerPeriod;
        this.refillTokensPerNano = permitsPerPeriod / (double) period.toNanos();
        if (Double.isNaN(refillTokensPerNano) || Double.isInfinite(refillTokensPerNano)
                || refillTokensPerNano <= 0) {
            throw new IllegalArgumentException("Invalid rate limiter configuration");
        }

        this.availableTokens = permitsPerPeriod;
        this.lastRefillNanos = System.nanoTime();
    }

    public void acquire() {
        synchronized (monitor) {
            refill();
            while (availableTokens < MIN_PERMIT) {
                long nanosToWait = nanosUntilNextPermit();
                if (nanosToWait <= 0) {
                    refill();
                    continue;
                }
                try {
                    long millis = nanosToWait / 1_000_000;
                    int nanos = (int) (nanosToWait % 1_000_000);
                    monitor.wait(millis, nanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Interrupted while waiting for AI rate limit permit", e);
                }
                refill();
            }
            availableTokens -= MIN_PERMIT;
        }
    }

    private void refill() {
        long now = System.nanoTime();
        double tokensToAdd = (now - lastRefillNanos) * refillTokensPerNano;
        if (tokensToAdd <= 0) {
            return;
        }
        availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
        lastRefillNanos = now;
        monitor.notifyAll();
    }

    private long nanosUntilNextPermit() {
        double missingTokens = MIN_PERMIT - availableTokens;
        if (missingTokens <= 0) {
            return 0L;
        }
        return (long) Math.ceil(missingTokens / refillTokensPerNano);
    }
}

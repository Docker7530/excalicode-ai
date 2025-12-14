package com.excalicode.platform.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** JWT 工具类 - 生成和验证 Token */
@Slf4j
@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration}")
  private Long expiration;

  /** 生成密钥 */
  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 生成 JWT Token
   *
   * @param username 用户名
   * @return JWT Token
   */
  public String generateToken(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .subject(username)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * 从 Token 中提取用户名
   *
   * @param token JWT Token
   * @return 用户名
   */
  public String getUsernameFromToken(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
      return claims.getSubject();
    } catch (Exception e) {
      log.error("解析Token失败: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 验证 Token 是否有效
   *
   * @param token JWT Token
   * @return 是否有效
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      log.error("Token验证失败: {}", e.getMessage());
      return false;
    }
  }
}

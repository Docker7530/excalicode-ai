package com.excalicode.platform.core.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

/** Redis VectorStore 初始化，为 Spring AI 提供 Jedis 和 VectorStore Bean 配置。 */
@Configuration
@EnableConfigurationProperties(RedisVectorStoreProperties.class)
public class RedisVectorStoreConfig {

  @Bean
  JedisPooled jedisPooled(RedisProperties redisProperties) {
    HostAndPort hostAndPort = new HostAndPort(redisProperties.getHost(), redisProperties.getPort());

    boolean sslEnabled =
        redisProperties.getSsl() != null
            && Boolean.TRUE.equals(redisProperties.getSsl().isEnabled());
    DefaultJedisClientConfig.Builder builder =
        DefaultJedisClientConfig.builder().database(redisProperties.getDatabase()).ssl(sslEnabled);

    if (StringUtils.hasText(redisProperties.getUsername())) {
      builder.user(redisProperties.getUsername());
    }
    if (StringUtils.hasText(redisProperties.getPassword())) {
      builder.password(redisProperties.getPassword());
    }

    JedisClientConfig clientConfig = builder.build();
    return new JedisPooled(hostAndPort, clientConfig);
  }

  @Bean
  VectorStore redisVectorStore(
      JedisPooled jedisPooled,
      EmbeddingModel embeddingModel,
      RedisVectorStoreProperties properties) {
    RedisVectorStore.Builder builder = RedisVectorStore.builder(jedisPooled, embeddingModel);
    if (StringUtils.hasText(properties.getIndexName())) {
      builder.indexName(properties.getIndexName());
    }
    if (StringUtils.hasText(properties.getPrefix())) {
      builder.prefix(properties.getPrefix());
    }

    builder.metadataFields(
        RedisVectorStore.MetadataField.tag("documentId"),
        RedisVectorStore.MetadataField.text("title"),
        RedisVectorStore.MetadataField.tag("tags"),
        RedisVectorStore.MetadataField.numeric("chunkIndex"));

    builder.initializeSchema(properties.isInitializeSchema());
    return builder.build();
  }
}

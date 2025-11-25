package com.excalicode.platform.web.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/** Jackson JSON 全局配置 统一管理整个应用的JSON序列化和反序列化行为 */
@Configuration
public class JacksonConfig {

  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
  public static final String DATE_PATTERN = "yyyy-MM-dd";
  public static final String TIME_PATTERN = "HH:mm:ss";

  /** 配置全局ObjectMapper */
  @Bean
  @Primary
  ObjectMapper objectMapper() {
    return Jackson2ObjectMapperBuilder.json()
        .serializationInclusion(JsonInclude.Include.NON_NULL) // 排除空值
        .modules(javaTimeModule()) // 时间处理模块
        .featuresToDisable(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, // 禁用时间戳
            SerializationFeature.FAIL_ON_EMPTY_BEANS) // 允许空Bean
        .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 忽略未知属性
        .featuresToEnable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT) // 空字符串转null
        .build();
  }

  /** Java 8 时间模块配置 */
  private JavaTimeModule javaTimeModule() {
    JavaTimeModule module = new JavaTimeModule();

    // 配置LocalDateTime
    module.addSerializer(
        LocalDateTime.class,
        new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
    module.addDeserializer(
        LocalDateTime.class,
        new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));

    // 配置LocalDate
    module.addSerializer(
        LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
    module.addDeserializer(
        LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));

    // 配置LocalTime
    module.addSerializer(
        LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));
    module.addDeserializer(
        LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

    return module;
  }
}

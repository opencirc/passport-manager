package com.opencirc.api.passport.service;

import com.opencirc.api.passport.exception.CacheSerializationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class CacheService {

  /** Injecting RedisTemplate class. */
  private final RedisTemplate<String, String> redisTemplate;

  /** Injecting ObjectMapper bean. */
  private final ObjectMapper objectMapper;

  /** Initialising CacheService bean. */
  public CacheService(
      @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplateParam,
      ObjectMapper objectMapperParam) {
    this.redisTemplate = redisTemplateParam;
    this.objectMapper = objectMapperParam;
  }

  /** Caches the template under the given key as a JSON string. */
  public <T> void cacheTemplate(String key, T template) {
    if (key == null || key.trim().isEmpty()) {
      throw new IllegalArgumentException("Cache key must not be null or blank");
    }
    try {
      String json = objectMapper.writeValueAsString(template);
      redisTemplate.opsForValue().set(key, json);
      log.debug("Cached template for key={}", key);
    } catch (JacksonException e) {
      log.warn("Failed to serialize template for cache key={}", key, e);
      throw new CacheSerializationException(
          "Failed to serialize template for cache key: " + key, e);
    }
  }

  /** Retrieves the cached template. */
  public <T> T getCachedTemplate(String key, Class<T> valueType) {
    if (key == null || key.trim().isEmpty()) {
      throw new IllegalArgumentException("Cache key must not be null or blank");
    }
    String cachedJson = redisTemplate.opsForValue().get(key);
    if (cachedJson == null) {
      log.debug("Cache miss for key={}", key);
      return null;
    }
    try {
      T value = objectMapper.readValue(cachedJson, valueType);
      log.debug("Cache hit for key={}, type={}", key, valueType.getSimpleName());
      return value;
    } catch (JacksonException e) {
      log.warn("Failed to deserialize cached template for key={}", key, e);
      throw new CacheSerializationException(
          "Failed to deserialize cached template for key: " + key, e);
    }
  }
}

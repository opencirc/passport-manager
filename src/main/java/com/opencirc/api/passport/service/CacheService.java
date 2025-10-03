package com.opencirc.api.passport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.exception.CacheSerializationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheService {

  /** Injecting RedisTemplate class. */
  private final RedisTemplate<String, String> redisTemplate;

  /** Injecting ObjectMapper bean. */
  private final ObjectMapper objectMapper;

  /**
   * Initialising CacheService bean.
   *
   * @param redisTemplateParam
   * @param objectMapperParam
   */
  public CacheService(
      @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplateParam,
      ObjectMapper objectMapperParam) {
    this.redisTemplate = redisTemplateParam;
    this.objectMapper = objectMapperParam;
  }

  /**
   * Caches the template under the given key as a JSON string.
   *
   * @param key cache key
   * @param template the template
   * @param <T> type of the template being cached
   */
  public <T> void cacheTemplate(String key, T template) {
    if (key == null || key.trim().isEmpty()) {
      throw new IllegalArgumentException("Cache key must not be null or blank");
    }
    try {
      String json = objectMapper.writeValueAsString(template);
      redisTemplate.opsForValue().set(key, json);
      log.debug("Cached template for key={}", key);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize template for cache key={}", key, e);
      throw new CacheSerializationException(
          "Failed to serialize template for cache key: " + key, e);
    }
  }

  /**
   * Retrieves the cached template.
   *
   * @param key cache key
   * @param <T> expected template type
   * @param valueType class of the expected type
   * @return the template instance
   */
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
    } catch (JsonProcessingException e) {
      log.warn("Failed to deserialize cached template for key={}", key, e);
      throw new CacheSerializationException(
          "Failed to deserialize cached template for key: " + key, e);
    }
  }
}

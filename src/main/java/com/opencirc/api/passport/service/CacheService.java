package com.opencirc.api.passport.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CacheService {

    /**
     * Injecting RedisTemplate class.
     */
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Injecting ObjectMapper bean.
     */
    private final ObjectMapper objectMapper;

    /**
     * Initialising CacheService bean.
     * @param redisTemplateParam
     * @param objectMapperParam
     */
    public CacheService(RedisTemplate<String, String> redisTemplateParam,
            ObjectMapper objectMapperParam) {
        this.redisTemplate = redisTemplateParam;
        this.objectMapper = objectMapperParam;
    }

    /**
     * Caches the template for the given uri.
     *
     * @param key
     * @param template
     * @param <T>
     */
    public <T> void cacheTemplate(String key, T template) {
        try {
            String json = objectMapper.writeValueAsString(template);
            redisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to serialize template for cache key: " + key, e);
        }
    }

    /**
     * Retrieves the cached template.
     *
     * @param key
     * @param <T>
     * @param valueType
     * @return the template
     */
    public <T> T getCachedTemplate(String key, Class<T> valueType) {
        String cachedJson = redisTemplate.opsForValue().get(key);
        if (cachedJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(cachedJson, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to deserialize cached template for key: " + key, e);
        }
    }
 }

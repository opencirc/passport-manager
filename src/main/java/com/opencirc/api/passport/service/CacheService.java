package com.opencirc.api.passport.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.enums.DataDictionary;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CacheService {

    /**
     * Injecting RedisTemplate class.
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Injecting ObjectMapper bean.
     */
    private final ObjectMapper objectMapper;

    /**
     * Initialising CacheService bean.
     * @param redisTemplate
     * @param objectMapper
     */
    public CacheService(RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Saves the data to cache.
     *
     * @param key
     * @param value
     */
    public void save(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * Retrieves the data from cache.
     *
     * @param key
     * @return Object - the data
     */
    public Object find(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Searches the data for specific search text in respective dictionary.
     *
     * @param dictionary
     * @param searchText
     * @return list of data
     */
    public List<Map<String, String>> searchProperties(DataDictionary dictionary,
            String searchText) {

        List<Map<String, String>> propertyList = new ArrayList<>();
        String pattern = "(?i).*" + java.util.regex.Pattern.quote(searchText) + ".*";

        Set<String> keys = redisTemplate.keys(dictionary.getValue() + "#*");

        if (keys != null) {
            propertyList = keys.stream().filter(
                    key -> key.toLowerCase().startsWith(dictionary.getValue() + "#"))
                    .filter(key -> {
                        String[] parts = key.split("#", AppConstants.NUM_THREE);
                        return parts.length == AppConstants.NUM_THREE
                                && Pattern.matches(pattern, parts[1]);
                    }).map(key -> {
                        Map<String, String> property = new HashMap<>();
                        String uri = (String) redisTemplate.opsForValue().get(key);
                        String[] parts = key.split("#", AppConstants.NUM_THREE);
                        if (parts.length == AppConstants.NUM_THREE) {
                            property.put("name", parts[1]);
                            property.put("code", parts[2]);
                            property.put("uri", uri);
                        }
                        return property;
                    }).collect(Collectors.toList());
        }

        return propertyList;
    }

    /**
     * Retrieves URI for the given code.
     *
     * @param dictionary
     * @param code
     * @return uri
     */
    public String getUriFromCode(DataDictionary dictionary, String code) {
        String dict = dictionary.toString();
        String quotedDict = java.util.regex.Pattern.quote(dict);
        String quotedCode = java.util.regex.Pattern.quote(code);
        String regexPattern = "^" + quotedDict + "#[^#]*#" + quotedCode + "$";

        ScanOptions scanOptions = ScanOptions.scanOptions().match(dict + "#*#")
                .count(AppConstants.NUM_HUNDRED)
                .build();

        return redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(scanOptions)) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next(), StandardCharsets.UTF_8);
                    if (key.matches(regexPattern)) {
                        return (String) redisTemplate.opsForValue().get(key);
                    }
                }
            }
            return null;
        });
    }


    /**
     * Stores properties  in cache.
     *
     * @param dictionary
     * @param propertyList
     */
    public void storePropertiesInRedis(DataDictionary dictionary,
            List<Map<String, String>> propertyList) {
        propertyList.forEach(property -> {
            String name = property.get("name");
            String code = property.get("code");
            String uri = property.get("uri");

            String redisKey = dictionary.getValue() + "#" + name + "#" + code;

            redisTemplate.opsForValue().set(redisKey, uri);
        });
    }

    /**
     * Stores the class template in cache.
     *
     * @param uri
     * @param template
     */
    public void storeClassTemplateInCache(String uri, Object template)
            throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(template);
        redisTemplate.opsForValue().set(uri, json);
    }

    /**
     * Retrieves the cached class template.
     *
     * @param uri
     * @param <T> The specific dictionary type
     * @param valueType
     * @return the template
     */
    public <T> T getClassTemplateFromCache(String uri, Class<T> valueType) {
        try {
            String json = (String) redisTemplate.opsForValue().get(uri);
            if (json == null) {
                return null;
            }

            return objectMapper.readValue(json, valueType);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis is not available: {}", e.getMessage(), e);
            throw new RuntimeException("Redis connection failed", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }

    }



    /**
     * Clears the data from cache.
     */
    public void clearCache() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }


    /**
     * Looks up all the data.
     * @return map of data
     */
    public Map<String, Object> lookCache() {
        Set<String> keys = redisTemplate.keys("*");
        Map<String, Object> resultMap = new HashMap<>();
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                Object value = redisTemplate.opsForValue().get(key);
                resultMap.put(key, value);
            }
        }
        return resultMap;
    }

}

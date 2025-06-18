package com.opencirc.api.passport.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.enums.DataDictionary;

@Service
public class CacheService {

    /**
     * Injecting RedisTemplate class.
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
        String pattern = "(?i).*" + searchText + ".*";

        Set<String> keys = redisTemplate
                .keys(dictionary.getValue() + "#*");

        if (keys != null) {
            propertyList = keys.stream()
                    .filter(key -> key.toLowerCase()
                            .startsWith(dictionary.getValue() + "#"))
                    .filter(key -> {
                        String[] parts = key.split("#");
                        return parts.length > 1
                                && Pattern.matches(pattern, parts[1]);
                    }).map(key -> {
                        Map<String, String> property = new HashMap<>();
                        String uri = (String) redisTemplate.opsForValue()
                                .get(key);
                        String[] parts = key.split("#");
                        if (parts.length > 1) {
                            String name = parts[1];
                            String code = parts[2];
                            property.put("name", name);
                            property.put("name", code);
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
    public String getUriFomCode(DataDictionary dictionary, String code) {

        String pattern = "^" + dictionary + "#[^#]*#" + code + "$";
        System.out.println(redisTemplate.keys("*"));
        Set<String> keys = redisTemplate.keys(dictionary + "#*#" + code);
        String uri = null;
        for (String key : keys) {
            if (key.matches(pattern)) {
                uri = (String) redisTemplate.opsForValue().get(key);
            }
        }

        return uri;

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

            String redisKey = dictionary + "#" + name + "#" + code;

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
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(template);
        redisTemplate.opsForValue().set(uri, json);
    }

    /**
     * Retrieves the cached class template.
     *
     * @param uri
     * @return the template
     */
    public <T> T getClassTemplateFromCache(String uri, Class<T> valueType) {
        try {
            String json = (String) redisTemplate.opsForValue().get(uri);
            if (json == null) {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, valueType);
        } catch (RedisConnectionFailureException e) {
            System.err.println("Redis is not available: " + e.getMessage());
            throw new RuntimeException("Redis connection failed", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }

    }


    /**
     * Loads cache with test data.
     *
     * @param dictionary
     */
    public void populateCacheWithTestData(DataDictionary dictionary) {
        setTestDataInCache(dictionary);

    }

    /**
     * Populates cache with test data.
     *
     * @param dictionary
     */
    private void setTestDataInCache(DataDictionary dictionary) {
        clearCache();

        Map<String, String> sampleProperties = new HashMap<>();
        sampleProperties.put(
                dictionary + "#EN 318_0b205c05-881f-44f3-83ca-568927baacfe",
                "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/"
                + "0b205c05-881f-44f3-83ca-568927baacfe");
        sampleProperties.put(dictionary
                + "#abrasion resistance class according"
                + "EN 14322_0b53e2f7-ddbd-4d5a-8eeb-5755267778fb",
                "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/"
                + "0b53e2f7-ddbd-4d5a-8eeb-5755267778fb");
        sampleProperties.put(
                dictionary + "#EN 338_0b44b46f-03ac-42c4-a500-408d5d175c0f",
                "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/"
                + "0b44b46f-03ac-42c4-a500-408d5d175c0f");
        sampleProperties.put(dictionary
                + "#spanLength-EN 1533_06917584-7982-4c67-8bb0-086a1433d4e8",
                "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/"
                + "06917584-7982-4c67-8bb0-086a1433d4e8");
        sampleProperties.put(dictionary
                + "#characteristic racking strength-"
                + "EN 594_06678d85-f8e2-4579-8ef4-d2599a2802e5",
                "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/"
                + "06678d85-f8e2-4579-8ef4-d2599a2802e5");
        sampleProperties.put(dictionary
                + "#grading of strips and square edged-EN "
                + "975-1_056f35d7-f741-402f-9006-07ca3286b2c4",
                "https://identifier.buildingsmart.org/uri/cei-bois.org/wood"
                + "/1.0.0/prop/056f35d7-f741-402f-9006-07ca3286b2c4");
        sampleProperties.put(dictionary
                + "#sound absorption coefficient-ISO 354_074cd12f-61b5-468b-"
                + "98f4-728804d2cce6",
                "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/"
                + "074cd12f-61b5-468b-98f4-728804d2cce6");
        sampleProperties.put(
                dictionary
                        + "#ISO 9001_1b9871dc-fe79-4b25-92f5-c127165e84e8",
                "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/"
                + "prop/1b9871dc-fe79-4b25-92f5-c127165e84e8");

        sampleProperties.forEach((key, uri) -> {
            redisTemplate.opsForValue().set(key, uri);
        });
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

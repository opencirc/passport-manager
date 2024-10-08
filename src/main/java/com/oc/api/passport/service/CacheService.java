package com.oc.api.passport.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void save(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public Object find(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    
    
	public List<Map<String, String>> searchProperties(String searchText, String dictionaryName) {

		// (dictionaryName);

		List<Map<String, String>> propertyList = new ArrayList<>();
		String pattern = "(?i).*" + searchText + ".*";

		Set<String> keys = redisTemplate.keys(dictionaryName.toLowerCase() + "#*");

		if (keys != null) {
			propertyList = keys.stream().filter(key -> key.toLowerCase().startsWith(dictionaryName.toLowerCase() + "#"))
					.filter(key -> {
						String[] parts = key.split("#");
						return parts.length > 1 && Pattern.matches(pattern, parts[1]);
					}).map(key -> {
						Map<String, String> property = new HashMap<>();
						String uri = (String) redisTemplate.opsForValue().get(key);
						String[] parts = key.split("#");
						if (parts.length > 1) {
							String name = parts[1];
							property.put("propertyName", name);
							property.put("uri", uri);
						}
						return property;
					}).collect(Collectors.toList());
		}

		return propertyList;
	}
    
	public void storePropertiesInRedis(String dictionaryName, List<Map<String, String>> propertyList) {
		propertyList.forEach(property -> {
			String name = property.get("name");
			String code = property.get("code");
			String uri = property.get("uri");

			String redisKey = dictionaryName + "#" + name + "#" + code;

			redisTemplate.opsForValue().set(redisKey, uri);
		});
	}

	
    public void populateCacheWithTestData(String dictionaryName) {
    	setTestDataInCache(dictionaryName);

    }

	private void setTestDataInCache(String dictionaryName) {
		clearCache();
    	
    	  Map<String, String> sampleProperties = new HashMap<>();
          sampleProperties.put(dictionaryName+"#EN 318_0b205c05-881f-44f3-83ca-568927baacfe", "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/0b205c05-881f-44f3-83ca-568927baacfe");
          sampleProperties.put(dictionaryName+"#abrasion resistance class according EN 14322_0b53e2f7-ddbd-4d5a-8eeb-5755267778fb", "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/0b53e2f7-ddbd-4d5a-8eeb-5755267778fb");
          sampleProperties.put(dictionaryName+"#EN 338_0b44b46f-03ac-42c4-a500-408d5d175c0f", "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/0b44b46f-03ac-42c4-a500-408d5d175c0f");
          sampleProperties.put(dictionaryName+"#spanLength-EN 1533_06917584-7982-4c67-8bb0-086a1433d4e8", "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/06917584-7982-4c67-8bb0-086a1433d4e8");
          sampleProperties.put(dictionaryName+"#characteristic racking strength-EN 594_06678d85-f8e2-4579-8ef4-d2599a2802e5", "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/06678d85-f8e2-4579-8ef4-d2599a2802e5");
          sampleProperties.put(dictionaryName+"#grading of strips and square edged-EN 975-1_056f35d7-f741-402f-9006-07ca3286b2c4", "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/056f35d7-f741-402f-9006-07ca3286b2c4");
          sampleProperties.put(dictionaryName+"#sound absorption coefficient-ISO 354_074cd12f-61b5-468b-98f4-728804d2cce6", "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/074cd12f-61b5-468b-98f4-728804d2cce6");
          sampleProperties.put(dictionaryName+"#ISO 9001_1b9871dc-fe79-4b25-92f5-c127165e84e8", "https://identifier.buildingsmart.org/uri/cei-bois.org/wood/1.0.0/prop/1b9871dc-fe79-4b25-92f5-c127165e84e8");
		
          sampleProperties.forEach((key, uri) -> {
              redisTemplate.opsForValue().set(key, uri);  
          });
    }

    
    
    private void clearCache() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }
    
    
    public void storePropertiesInCache(List<Map<String, String>> details, String dataDictionaryName) {
    	
    }
}

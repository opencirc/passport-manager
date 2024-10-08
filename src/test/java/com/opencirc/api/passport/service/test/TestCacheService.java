/*package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class TestCacheService {
	
	@Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Map<String, String> sampleProperties;
    
    
    @BeforeEach
    public void setUp() {
        sampleProperties = new HashMap<>();
        sampleProperties.put("window", "/uri/window");
        sampleProperties.put("WindowFrame", "/uri/window-frame");
        sampleProperties.put("door", "/uri/door");
        sampleProperties.put("doorHandle", "/uri/door-handle");
        
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }
    
    
    @Test
    public void testPopulateRedisCache() {
       
    	sampleProperties.forEach((key, value) -> redisTemplate.opsForValue().set(key, value));

        sampleProperties.forEach((key, value) -> {
            String redisValue = redisTemplate.opsForValue().get(key);
            assertEquals(value, redisValue, "The value in Redis should match the expected URI");
        });
    }

}*/

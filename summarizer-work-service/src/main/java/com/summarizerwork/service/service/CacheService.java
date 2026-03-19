package com.summarizerwork.service.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {
	
	private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "summary:cache:";
    private static final long TTL_HOURS = 24;

    // Store summary in Redis with 24-hour expiry
    public void cacheSummary(String contentHash, String summary) {
        String key = KEY_PREFIX + contentHash;
        redisTemplate.opsForValue().set(key, summary, TTL_HOURS, TimeUnit.HOURS);
        log.info("Cached summary for hash {}", contentHash);
    }

    // Check if summary already exists in cache
    public String getCachedSummary(String contentHash) {
        String key = KEY_PREFIX + contentHash;
        return redisTemplate.opsForValue().get(key);
    }
}

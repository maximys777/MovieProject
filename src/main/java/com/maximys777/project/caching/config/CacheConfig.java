package com.maximys777.project.caching.config;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Custom cache with custom time
        cacheManager.registerCustomCache("movies", buildCache(100, 6, TimeUnit.HOURS));
        cacheManager.registerCustomCache("tvShows", buildCache(100, 6, TimeUnit.HOURS));
        cacheManager.registerCustomCache("tvShowDetails", buildCache(1000, 3, TimeUnit.HOURS));

        cacheManager.setAsyncCacheMode(true);
        return cacheManager;
    }

    private AsyncCache<Object, Object> buildCache(int maximumSize, int duration, TimeUnit unit) {
        return Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(duration, unit)
                .recordStats()
                .buildAsync();
    }
}

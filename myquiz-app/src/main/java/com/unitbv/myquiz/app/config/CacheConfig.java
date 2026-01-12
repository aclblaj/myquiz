package com.unitbv.myquiz.app.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for MyQuiz application.
 * Uses in-memory ConcurrentHashMap-based caching for development.
 * For production, consider replacing with Redis or Caffeine cache.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure a simple in-memory cache manager.
     * Cache names are defined by @Cacheable annotations in service classes.
     *
     * @return CacheManager instance
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "courseNames",
            "allAuthorsBasic",
            "authorsByCourse",
            "allQuizInfo",
            "quizInfoByCourse"
        );
    }
}


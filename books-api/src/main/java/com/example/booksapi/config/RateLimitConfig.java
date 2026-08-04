package com.example.booksapi.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> listBuckets = new ConcurrentHashMap<>();

    public Bucket getLoginBucket(String key) {
        return loginBuckets.computeIfAbsent(key, k -> newBucket(5, Duration.ofMinutes(1)));
    }

    public Bucket getListBucket(String key) {
        return listBuckets.computeIfAbsent(key, k -> newBucket(30, Duration.ofMinutes(1)));
    }

    private Bucket newBucket(long capacity, Duration period) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, period));
        return Bucket.builder().addLimit(limit).build();
    }
}

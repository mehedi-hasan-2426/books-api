package com.example.booksapi.config;

import com.example.booksapi.dto.ApiProblem;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long LOGIN_RATE_LIMIT = 5;
    private static final long LIST_RATE_LIMIT = 30;

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    private record RateLimit(Bucket bucket, long cap) {}

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String ip = clientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();

        RateLimit rl = resolveLimit(method, path, ip);
        if (rl == null) return true;

        ConsumptionProbe probe = rl.bucket().tryConsumeAndReturnRemaining(1);
        long retryAfterSecs = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

        response.setHeader("X-RateLimit-Limit", String.valueOf(rl.cap()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        response.setHeader("X-RateLimit-Reset",
                String.valueOf(System.currentTimeMillis() / 1000 + retryAfterSecs));

        if (!probe.isConsumed()) {
            response.setHeader("Retry-After", String.valueOf(retryAfterSecs));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/problem+json");
            objectMapper.writeValue(response.getWriter(), ApiProblem.builder()
                    .type("/problems/rate-limit-exceeded")
                    .title("Too Many Requests")
                    .status(429)
                    .detail("Rate limit exceeded. Retry in " + retryAfterSecs + " seconds.")
                    .instance(path)
                    .build());
            return false;
        }

        return true;
    }

    private RateLimit resolveLimit(String method, String path, String ip) {
        if ("POST".equals(method) && "/auth/login".equals(path))
            return new RateLimit(rateLimitConfig.getLoginBucket(ip), LOGIN_RATE_LIMIT);
        if ("GET".equals(method) && "/books".equals(path))
            return new RateLimit(rateLimitConfig.getListBucket(ip), LIST_RATE_LIMIT);
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}

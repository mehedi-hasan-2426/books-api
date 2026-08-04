package com.example.booksapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void loginSuccess(String username, String ip) {
        log.info("event=LOGIN_SUCCESS username={} ip={}", username, sanitize(ip));
    }

    public void loginFailure(String username, String ip) {
        log.warn("event=LOGIN_FAILURE username={} ip={}", sanitize(username), sanitize(ip));
    }

    public void registrationSuccess(String username, String ip) {
        log.info("event=REGISTRATION username={} ip={}", username, sanitize(ip));
    }

    public void tokenRefreshed(String username) {
        log.info("event=TOKEN_REFRESHED username={}", username);
    }

    public void tokenRefreshDenied(String ip) {
        log.warn("event=TOKEN_REFRESH_DENIED ip={}", sanitize(ip));
    }

    public void accessDenied(String username, String resource, String ip) {
        log.warn("event=ACCESS_DENIED username={} resource={} ip={}", username, resource, sanitize(ip));
    }

    public void invalidToken(String ip) {
        log.warn("event=INVALID_TOKEN ip={}", sanitize(ip));
    }

    // Prevent log injection via CR/LF/tab in user-controlled values
    private String sanitize(String value) {
        if (value == null) return "null";
        return value.replaceAll("[\r\n\t]", "_");
    }
}

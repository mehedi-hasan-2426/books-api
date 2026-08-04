package com.example.booksapi.service;

import com.example.booksapi.dto.AuthRequest;
import com.example.booksapi.dto.AuthResponse;
import com.example.booksapi.dto.RegisterRequest;
import com.example.booksapi.exception.ConflictException;
import com.example.booksapi.exception.UnauthorizedException;
import com.example.booksapi.model.RefreshToken;
import com.example.booksapi.model.Role;
import com.example.booksapi.model.User;
import com.example.booksapi.repository.RefreshTokenRepository;
import com.example.booksapi.repository.UserRepository;
import com.example.booksapi.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityAuditService auditService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public void register(RegisterRequest request, String clientIp) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists: " + request.getUsername());
        }
        userRepository.save(User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .createdAt(Instant.now())
                .build());
        auditService.registrationSuccess(request.getUsername(), clientIp);
    }

    @Transactional
    public AuthResponse login(AuthRequest request, String clientIp) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        auditService.loginSuccess(user.getUsername(), clientIp);
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user.getUsername()))
                .refreshToken(createRefreshToken(user))
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse refresh(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException("Refresh token expired");
        }

        String username = stored.getUser().getUsername();
        String newAccess = jwtService.generateToken(username);
        // Rotate the refresh token on each use (prevents token replay)
        refreshTokenRepository.delete(stored);
        String newRefresh = createRefreshToken(stored.getUser());
        auditService.tokenRefreshed(username);
        return AuthResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .username(username)
                .role(stored.getUser().getRole().name())
                .build();
    }

    private String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .build());
        return token;
    }
}

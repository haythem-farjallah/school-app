package com.example.school_management.commons.configs;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;                 // raw text OR base64 – see init()

    @Value("${jwt.expiration.ms}")
    private long accessTtlMs;

    @Value("${jwt.refresh.expiration.ms}")
    private long refreshTtlMs;

    private SecretKey  key;                   // final after @PostConstruct
    private JwtParser  parser;                // thread–safe, reuse

    /* ----------------------------------------------------------- */
    @jakarta.annotation.PostConstruct
    void init() {
        this.key = resolveKey(jwtSecret);
        this.parser = Jwts.parserBuilder()
                .setSigningKey(key)
                .build();
        log.info("JWT provider initialized (alg=HS256, accessTtl={}ms)", accessTtlMs);
    }

    /* Helper: choose raw vs. Base64 */
    private static SecretKey resolveKey(String secret) {
        String trimmed = secret.trim();
        byte[] keyBytes;
        if (trimmed.matches("^[A-Za-z0-9+/=]+$") && trimmed.length() % 4 == 0) {
            // looks like Base64
            keyBytes = Decoders.BASE64.decode(trimmed);
        } else {
            keyBytes = trimmed.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {           // 256-bit for HS256
            throw new IllegalArgumentException(
                    "JWT secret key must be at least 256 bits (32 ASCII chars)");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /* ----------------------------------------------------------- */
    public String generateAccessToken(UserDetails user) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)  // ROLE_ADMIN …
                .toList();

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", roles)                    // ← NEW
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessTtlMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserDetails user) {

            List<String> roles = user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)  // ROLE_ADMIN …
                    .toList();

            long now = System.currentTimeMillis();
            return Jwts.builder()
                    .setSubject(user.getUsername())
                    .claim("roles", roles)                    // ← NEW
                    .setIssuedAt(new Date(now))
                    .setExpiration(new Date(now + accessTtlMs))
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

    }

    private String buildToken(String subject, long ttlMs) {

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /* ----------------------------------------------------------- */
    public boolean validateToken(String rawHeaderToken) {
        String token = stripPrefix(rawHeaderToken);
        if (token == null) return false;

        try {
            parser.parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT: {}", ex.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String rawHeaderToken) {
        String token = stripPrefix(rawHeaderToken);
        if (token == null) throw new IllegalArgumentException("Empty token");
        return parser.parseClaimsJws(token).getBody().getSubject();
    }

    /* Accept "Bearer x.y.z" or raw token */
    private static String stripPrefix(String header) {
        if (header == null || header.isBlank()) return null;
        return header.startsWith("Bearer ") ? header.substring(7).trim()
                : header.trim();
    }

    public String issue(String email, List<String> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", roles)          // ROLE_WORKER, …
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessTtlMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}

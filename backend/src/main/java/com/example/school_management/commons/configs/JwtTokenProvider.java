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
import java.util.Optional;

/**
 * Issues and validates the two kinds of JWT the API uses. Every token carries a signed
 * {@value #TOKEN_TYPE_CLAIM} claim, so an access token is never accepted where a refresh
 * token is expected and the other way round. Tokens without that claim are invalid.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    static final String TOKEN_TYPE_CLAIM = "tokenType";

    enum TokenType { ACCESS, REFRESH }

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
        log.info("JWT provider initialized (alg=HS256, accessTtl={}ms, refreshTtl={}ms)", accessTtlMs, refreshTtlMs);
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
                .claim("roles", roles)
                .claim(TOKEN_TYPE_CLAIM, TokenType.ACCESS.name())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessTtlMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * A refresh token only identifies the account: the refresh flow reloads its current
     * authorities from the database before issuing a new access token.
     */
    public String generateRefreshToken(UserDetails user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim(TOKEN_TYPE_CLAIM, TokenType.REFRESH.name())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + refreshTtlMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /* ----------------------------------------------------------- */
    /** Returns the email of a validly signed, unexpired ACCESS token; empty for anything else. */
    public Optional<String> validateAccessToken(String token) {
        return subjectOf(token, TokenType.ACCESS);
    }

    /** Returns the email of a validly signed, unexpired REFRESH token; empty for anything else. */
    public Optional<String> validateRefreshToken(String token) {
        return subjectOf(token, TokenType.REFRESH);
    }

    private Optional<String> subjectOf(String token, TokenType expectedType) {
        try {
            Claims claims = parser.parseClaimsJws(token).getBody();
            if (!expectedType.name().equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                log.debug("JWT rejected: not an {} token", expectedType);
                return Optional.empty();
            }
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}

package com.event_management_system.service;

import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


@Service
public class JwtService {

    @Autowired
    private ApplicationLoggerService log;

    
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    
    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    
    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    
    public String generateAccessToken(Long userId) {
        return generateToken(userId, accessTokenExpiration);
    }
    
    public String generateAccessToken(Long userId, String roleName, Long roleId) {
        return generateToken(userId, roleName, roleId, accessTokenExpiration);
    }

   
    public String generateRefreshToken(Long userId) {
        return generateToken(userId, null, null, refreshTokenExpiration);
    }

   
    private String generateToken(Long userId, long duration) {
        return generateToken(userId, null, null, duration);
    }
    
    private String generateToken(Long userId, String roleName, Long roleId, long duration) {
        String tokenUuid = UUID.randomUUID().toString();
        log.debug("Generated token UUID: {} for user: {}", tokenUuid, userId);

        Date now = new Date();
        
        Date expirationDate = new Date(now.getTime() + duration);

        var builder = Jwts.builder()
                .subject(userId.toString())
                
                .claim("tokenUuid", tokenUuid)
                
                .issuedAt(now)
                
                .expiration(expirationDate)
                
                .signWith(getSigningKey());
        
        // Add role information if provided (for access tokens)
        if (roleName != null) {
            builder.claim("role", roleName);
            builder.claim("roleId", roleId);
        }

        String token = builder.compact();

        log.info("Generated token for user: {} with UUID: {} and role: {}", userId, tokenUuid, roleName);
        return token;
    }

    
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token); 

            log.debug("Token validated successfully");
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Token is expired: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            return false;
        }
    }

   
    public Long getUserIdFromToken(String token) {
        Claims claims = getTokenClaims(token);
        String userId = claims.getSubject();
        return Long.valueOf(userId);
    }

    
    public String getTokenUuidFromToken(String token) {
        Claims claims = getTokenClaims(token);
        return claims.get("tokenUuid", String.class);
    }

    
    private Claims getTokenClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}

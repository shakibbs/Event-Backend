package com.event_management_system.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class TokenCacheService {

    @Autowired
    private ApplicationLoggerService log;

    
    private final ConcurrentHashMap<String, CacheEntry> tokenCache = new ConcurrentHashMap<>();

   
    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

 
     
    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    
    public void cacheAccessToken(String tokenUuid, Long userId) {
        long expirationTime = System.currentTimeMillis() + accessTokenExpiration;
        CacheEntry entry = new CacheEntry(userId, expirationTime);
        tokenCache.put(tokenUuid, entry);
        log.debug("Cached access token UUID: {} for user: {} (expires in {}ms)", 
                  tokenUuid, userId, accessTokenExpiration);
    }

    
    public void cacheRefreshToken(String tokenUuid, Long userId) {
        long expirationTime = System.currentTimeMillis() + refreshTokenExpiration;
        CacheEntry entry = new CacheEntry(userId, expirationTime);
        tokenCache.put(tokenUuid, entry);
        log.debug("Cached refresh token UUID: {} for user: {} (expires in {}ms)", 
                  tokenUuid, userId, refreshTokenExpiration);
    }

    
    public Long getUserIdFromCache(String tokenUuid) {
        CacheEntry entry = tokenCache.get(tokenUuid);

        if (entry == null) {
            log.warn("Token UUID not found in cache: {} (user logged out or cache cleared)", tokenUuid);
            return null;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime > entry.getExpirationTime()) {
            tokenCache.remove(tokenUuid);
            log.warn("Token UUID expired and removed from cache: {}", tokenUuid);
            return null;
        }

        log.debug("Token UUID validated from cache: {} with user ID: {}", tokenUuid, entry.getUserId());
        return entry.getUserId();
    }

   
    public void removeTokenFromCache(String tokenUuid) {
        tokenCache.remove(tokenUuid);
        log.info("Token UUID removed from cache (user logged out): {}", tokenUuid);
    }

    
    public void clearAllTokens() {
        tokenCache.clear();
        log.warn("All tokens cleared from cache - all users logged out");
    }

    
    public int getTokenCacheSize() {
        return tokenCache.size();
    }

    
    private static class CacheEntry {
        private final Long userId;
        private final long expirationTime;

        
        CacheEntry(Long userId, long expirationTime) {
            this.userId = userId;
            this.expirationTime = expirationTime;
        }

        
        Long getUserId() {
            return userId;
        }

        long getExpirationTime() {
            return expirationTime;
        }
    }
}

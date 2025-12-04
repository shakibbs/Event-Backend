package com.event_management_system.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * TokenCacheService: Manages token UUID caching for server-side logout
 * 
 * PURPOSE:
 * Store mapping: tokenUuid → userId
 * 
 * This enables:
 * 1. Server-side logout (delete UUID from cache, token becomes invalid)
 * 2. Token consistency check (verify: cached_userId == token_userId)
 * 3. Token expiration on cache (auto-delete after timeout)
 * 
 * FLOW from Diagrams:
 * 
 * LOGIN (Diagram 3, Steps 8-9):
 * 1. JwtService creates accessToken with UUID
 * 2. JwtService creates refreshToken with UUID
 * 3. TokenCacheService.cacheAccessToken(uuid, userId, expirationMs)
 *    → Stores: "uuid" → userId in cache with auto-expiration
 * 4. TokenCacheService.cacheRefreshToken(uuid, userId, expirationMs)
 *    → Stores: "uuid" → userId in cache with auto-expiration
 * 5. Return tokens to client
 * 
 * REQUEST VALIDATION (Diagram 4, Step 3):
 * 1. Client sends JWT in Authorization header
 * 2. JwtAuthenticationFilter extracts UUID from token
 * 3. TokenCacheService.getUserIdFromCache(uuid)
 *    → Looks up UUID in cache
 *    → Returns userId if found, null if not found/expired
 * 4. Compare: cached_userId == token.getUserId()
 *    → If match → User not logged out, continue
 *    → If no match → Token invalid, return HTTP 401
 * 
 * LOGOUT (Diagram 3, Step 10):
 * 1. Client: POST /api/auth/logout with token
 * 2. AuthController extracts UUID from token
 * 3. TokenCacheService.removeTokenFromCache(uuid)
 *    → Deletes UUID from cache
 * 4. Even if client uses same JWT later:
 *    → UUID not in cache
 *    → getUserIdFromCache() returns null
 *    → HTTP 401 "Token expired or invalid"
 * 
 * IMPLEMENTATION NOTES:
 * 
 * Current: Using ConcurrentHashMap (in-memory, thread-safe)
 * - Simple, fast, works for development/testing
 * - All data lost on restart
 * - Suitable for single-server deployment
 * 
 * Later: Switch to Redis (Distributed cache)
 * - Survives server restart
 * - Shared across multiple servers
 * - Automatic expiration with TTL
 * 
 * MIGRATION PATH (when you're ready):
 * 1. Add spring-boot-starter-data-redis to pom.xml
 * 2. Add Redis config to application.properties
 * 3. Change @Service to use RedisTemplate<String, Long>
 * 4. Rest of code remains same (interface stays same)
 */
@Slf4j
@Service
public class TokenCacheService {

    /**
     * In-memory cache for token UUIDs
     * 
     * Key: Token UUID (String)
     * Value: User ID (Long)
     * 
     * Example:
     * "550e8400-e29b-41d4-a716-446655440000" → 1L (user with ID 1)
     * 
     * ConcurrentHashMap:
     * - Thread-safe: Multiple threads can access simultaneously
     * - Fast: HashMap implementation, O(1) average access time
     * - No automatic expiration: We handle expiration manually
     * 
     * Manual expiration:
     * - Store: tokenUuid → new CacheEntry(userId, expirationTime)
     * - On retrieve: Check if current_time > expirationTime
     * - If expired: Remove entry and return null
     */
    private final ConcurrentHashMap<String, CacheEntry> tokenCache = new ConcurrentHashMap<>();

    /**
     * Access token expiration time in milliseconds
     * 
     * Injected from application.properties: app.jwt.access-token-expiration
     * 
     * Example: 2700000 = 45 minutes
     * 
     * Used to:
     * 1. Set cache entry expiration time
     * 2. Calculate when to auto-remove from cache
     */
    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    /**
     * Refresh token expiration time in milliseconds
     * 
     * Injected from application.properties: app.jwt.refresh-token-expiration
     * 
     * Example: 604800000 = 7 days
     * 
     * Used to:
     * 1. Set refresh token cache entry expiration
     */
    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Cache access token UUID
     * 
     * WHEN CALLED: After generating access token (Diagram 3, Step 8)
     * 
     * WHAT IT DOES:
     * 1. Create cache entry with:
     *    - userId: Who owns this token
     *    - expirationTime: When should this entry auto-delete
     * 2. Store in cache: uuid → entry
     * 3. Log the operation
     * 
     * EXAMPLE:
     * tokenUuid = "550e8400-e29b-41d4-a716-446655440000"
     * userId = 1L
     * 
     * Cache state after call:
     * {
     *   "550e8400-e29b-41d4-a716-446655440000": {
     *     "userId": 1,
     *     "expirationTime": 1632340550000 (45 minutes from now)
     *   }
     * }
     * 
     * @param tokenUuid UUID from JWT token
     * @param userId User ID who owns this token
     */
    public void cacheAccessToken(String tokenUuid, Long userId) {
        long expirationTime = System.currentTimeMillis() + accessTokenExpiration;
        CacheEntry entry = new CacheEntry(userId, expirationTime);
        tokenCache.put(tokenUuid, entry);
        log.debug("Cached access token UUID: {} for user: {} (expires in {}ms)", 
                  tokenUuid, userId, accessTokenExpiration);
    }

    /**
     * Cache refresh token UUID
     * 
     * WHEN CALLED: After generating refresh token (Diagram 3, Step 9)
     * 
     * WHAT IT DOES:
     * 1. Create cache entry with:
     *    - userId: Who owns this token
     *    - expirationTime: When should this entry auto-delete (7 days)
     * 2. Store in cache: uuid → entry
     * 3. Log the operation
     * 
     * Same as cacheAccessToken(), but with longer expiration (7 days vs 45 min)
     * 
     * @param tokenUuid UUID from JWT token
     * @param userId User ID who owns this token
     */
    public void cacheRefreshToken(String tokenUuid, Long userId) {
        long expirationTime = System.currentTimeMillis() + refreshTokenExpiration;
        CacheEntry entry = new CacheEntry(userId, expirationTime);
        tokenCache.put(tokenUuid, entry);
        log.debug("Cached refresh token UUID: {} for user: {} (expires in {}ms)", 
                  tokenUuid, userId, refreshTokenExpiration);
    }

    /**
     * Retrieve user ID from cache using token UUID
     * 
     * WHEN CALLED: On every protected request (Diagram 4, Step 3)
     * 
     * WHAT IT DOES:
     * 1. Look up UUID in cache
     * 2. If not found → return null
     * 3. If found, check expiration:
     *    - If expired → remove from cache and return null
     *    - If not expired → return cached userId
     * 
     * RETURN VALUES:
     * - Long (e.g., 1L): Token is valid, user ID is 1
     * - null: Token is invalid/expired/logged out
     * 
     * EXAMPLE:
     * 
     * Scenario 1: Valid token (not expired, not logged out)
     * UUID: "550e8400-e29b-41d4-a716-446655440000"
     * Cache: {"550e8400-...": { userId: 1, expirationTime: 1632340550000 }}
     * Current time: 1632340000000 (token still valid)
     * Returns: 1L ✓
     * 
     * Scenario 2: Expired token
     * UUID: "550e8400-e29b-41d4-a716-446655440000"
     * Cache: {"550e8400-...": { userId: 1, expirationTime: 1632340550000 }}
     * Current time: 1632340600000 (after expiration)
     * Action: Remove from cache
     * Returns: null → HTTP 401 "Token expired"
     * 
     * Scenario 3: Logged out token
     * UUID: "550e8400-e29b-41d4-a716-446655440000"
     * Cache: {} (empty, we deleted it on logout)
     * Returns: null → HTTP 401 "Token invalid"
     * 
     * @param tokenUuid UUID from JWT token
     * @return User ID if valid, null if expired/logged out/invalid
     */
    public Long getUserIdFromCache(String tokenUuid) {
        CacheEntry entry = tokenCache.get(tokenUuid);

        // Token UUID not in cache
        if (entry == null) {
            log.warn("Token UUID not found in cache: {} (user logged out or cache cleared)", tokenUuid);
            return null;
        }

        // Check if token has expired
        long currentTime = System.currentTimeMillis();
        if (currentTime > entry.getExpirationTime()) {
            // Token expired, remove from cache
            tokenCache.remove(tokenUuid);
            log.warn("Token UUID expired and removed from cache: {}", tokenUuid);
            return null;
        }

        // Token is valid and not expired
        log.debug("Token UUID validated from cache: {} with user ID: {}", tokenUuid, entry.getUserId());
        return entry.getUserId();
    }

    /**
     * Remove token UUID from cache (Logout)
     * 
     * WHEN CALLED: When user logs out (Diagram 3, Step 10)
     * 
     * WHAT IT DOES:
     * 1. Remove UUID from cache
     * 2. Even if client uses same JWT later:
     *    → UUID not found in cache
     *    → getUserIdFromCache() returns null
     *    → JwtAuthenticationFilter returns HTTP 401
     * 
     * EXAMPLE:
     * 
     * Before logout:
     * Cache: {"550e8400-...": { userId: 1, expirationTime: 1632340550000 }}
     * 
     * Call: removeTokenFromCache("550e8400-...")
     * 
     * After logout:
     * Cache: {} (empty, entry removed)
     * 
     * Client tries to use same token:
     * → getUserIdFromCache() finds nothing
     * → Returns null
     * → User gets HTTP 401 "Token invalid"
     * 
     * SECURITY NOTE:
     * This is why tokenUuid is important!
     * Without it, we'd need:
     * - Token blacklist (database)
     * - Database lookup on every request
     * - Complex management
     * 
     * With tokenUuid:
     * - Simple cache delete
     * - O(1) operation
     * - Instant logout
     * 
     * @param tokenUuid UUID to remove from cache
     */
    public void removeTokenFromCache(String tokenUuid) {
        tokenCache.remove(tokenUuid);
        log.info("Token UUID removed from cache (user logged out): {}", tokenUuid);
    }

    /**
     * Clear all tokens from cache (Admin function)
     * 
     * WHEN CALLED: 
     * - Admin wants to logout all users
     * - Server shutdown/maintenance
     * - Cache cleanup
     * 
     * WHAT IT DOES: Removes all entries from cache
     * 
     * EFFECT:
     * All users logged out immediately
     * All valid tokens become invalid
     * All clients must login again
     */
    public void clearAllTokens() {
        tokenCache.clear();
        log.warn("All tokens cleared from cache - all users logged out");
    }

    /**
     * Get cache size (for monitoring/debugging)
     * 
     * WHEN CALLED: For monitoring, logging, debugging
     * 
     * EXAMPLE:
     * int activeTokens = getTokenCacheSize();
     * log.info("Active tokens in cache: {}", activeTokens);
     * 
     * @return Number of valid tokens in cache
     */
    public int getTokenCacheSize() {
        return tokenCache.size();
    }

    /**
     * Inner class: Represents a cached token entry
     * 
     * Structure:
     * {
     *   "userId": 1,
     *   "expirationTime": 1632340550000
     * }
     * 
     * userId: Who owns this token
     * expirationTime: When should this entry expire (milliseconds since epoch)
     */
    private static class CacheEntry {
        private final Long userId;
        private final long expirationTime;

        /**
         * Constructor
         * 
         * @param userId User ID who owns this token
         * @param expirationTime When this entry expires (milliseconds)
         */
        CacheEntry(Long userId, long expirationTime) {
            this.userId = userId;
            this.expirationTime = expirationTime;
        }

        /**
         * Get user ID
         */
        Long getUserId() {
            return userId;
        }

        /**
         * Get expiration time
         */
        long getExpirationTime() {
            return expirationTime;
        }
    }
}

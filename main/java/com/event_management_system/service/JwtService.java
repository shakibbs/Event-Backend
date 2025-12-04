package com.event_management_system.service;

import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JwtService: Handles JWT token generation, validation, and parsing
 * 
 * JWT (JSON Web Token) Structure:
 * 
 * A JWT has 3 parts separated by dots: Header.Payload.Signature
 * 
 * Example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIn0.signature...
 * 
 * Part 1 - Header (auto-generated):
 * {
 *   "alg": "HS256",    // Algorithm: HMAC SHA-256
 *   "typ": "JWT"       // Type: JSON Web Token
 * }
 * 
 * Part 2 - Payload (Claims - user data):
 * {
 *   "sub": "1",                    // Subject (user ID)
 *   "tokenUuid": "abc-123-def",    // Token unique identifier for logout
 *   "iat": 1632339650,             // Issued at (timestamp)
 *   "exp": 1632340550              // Expiration (timestamp)
 * }
 * 
 * Part 3 - Signature:
 * HMACSHA256(
 *   base64UrlEncode(header) + "." + base64UrlEncode(payload),
 *   secret_key
 * )
 * 
 * Security:
 * - If anyone modifies the payload → signature won't match → token rejected
 * - Only server with secret_key can create valid tokens
 * - Client cannot forge tokens or modify their contents
 * 
 * FLOW from Diagrams:
 * 
 * LOGIN (Diagram 3):
 * 1. AuthService calls JwtService.generateAccessToken(userId)
 * 2. JwtService creates UUID
 * 3. JwtService creates JWT with userId + uuid in payload
 * 4. Signs JWT with server secret
 * 5. Returns JWT string to AuthService
 * 
 * REQUEST VALIDATION (Diagram 4):
 * 1. JwtAuthenticationFilter extracts token from Authorization header
 * 2. JwtAuthenticationFilter calls JwtService.validateToken(token)
 * 3. JwtService verifies signature (hasn't been tampered with)
 * 4. JwtService verifies expiration (not expired)
 * 5. If valid, returns true
 * 
 * EXTRACT USER DATA (Diagram 4):
 * 1. JwtAuthenticationFilter calls JwtService.getTokenClaims(token)
 * 2. JwtService extracts and returns payload claims
 * 3. AuthenticationFilter extracts userId and tokenUuid from claims
 * 4. AuthenticationFilter looks up userId in cache using tokenUuid
 */
@Slf4j
@Service
public class JwtService {

    /**
     * Secret key for signing tokens
     * 
     * Injected from application.properties: app.jwt.secret
     * 
     * Requirements:
     * - Minimum 32 characters (256 bits) for HS256
     * - Never expose to client
     * - Store in environment variables in production
     * - Same key used for both signing and verification
     * 
     * Example value: "my-super-secret-key-must-be-at-least-32-characters-long"
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * Access Token expiration time in milliseconds
     * 
     * Injected from application.properties: app.jwt.access-token-expiration
     * 
     * Example: 900000 = 15 minutes
     * 
     * Why short-lived?
     * - If access token is stolen, damage is limited (only 15 min)
     * - User won't be stuck with compromised token for days
     * - Forces regular token refresh, keeps sessions fresh
     */
    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    /**
     * Refresh Token expiration time in milliseconds
     * 
     * Injected from application.properties: app.jwt.refresh-token-expiration
     * 
     * Example: 604800000 = 7 days
     * 
     * Why longer-lived?
     * - Allows user to stay logged in for extended period
     * - Used only when access token expires
     * - If refresh token stolen, user can logout manually
     */
    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Generate an Access Token
     * 
     * WHEN CALLED: After user successfully logs in (password matches)
     * 
     * WHAT IT DOES:
     * 1. Generate UUID (unique identifier for this token)
     * 2. Create JWT with:
     *    - sub (subject): userId
     *    - tokenUuid (custom claim): the UUID
     *    - iat (issued at): current time
     *    - exp (expiration): current time + 15 minutes
     * 3. Sign JWT with secret key
     * 4. Return JWT as string
     * 
     * JWT Signature:
     * - HMAC-SHA256 is used (HS256)
     * - Both signing and verification use same secret key
     * - If token is modified → signature won't match → token rejected
     * 
     * Example return value:
     * "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidG9rZW5VdWlkIjoiYWJjLTEyMyIsImlhdCI6MTYzMjMzOTY1MCwiZXhwIjoxNjMyMzQwNTUwfQ.signature..."
     * 
     * @param userId User's database ID (who owns this token)
     * @return JWT token as string
     */
    public String generateAccessToken(Long userId) {
        return generateToken(userId, accessTokenExpiration);
    }

    /**
     * Generate a Refresh Token
     * 
     * WHEN CALLED: Same time as access token (during login)
     * 
     * WHAT IT DOES: Same as access token, but with longer expiration (7 days)
     * 
     * PURPOSE: When access token expires, client uses this to get new access token
     * without re-entering password
     * 
     * @param userId User's database ID (who owns this token)
     * @return JWT token as string
     */
    public String generateRefreshToken(Long userId) {
        return generateToken(userId, refreshTokenExpiration);
    }

    /**
     * Private helper method to generate tokens
     * 
     * This does the actual work for both access and refresh tokens
     * 
     * STEPS:
     * 1. Create UUID: "550e8400-e29b-41d4-a716-446655440000"
     * 2. Create JWT builder
     * 3. Set subject: convert userId to String
     * 4. Add custom claim: tokenUuid
     * 5. Set issued at: now
     * 6. Set expiration: now + duration
     * 7. Sign with secret: creates signature
     * 8. Compact: convert to string format (Header.Payload.Signature)
     * 
     * @param userId User ID to embed in token
     * @param duration How long token should live (milliseconds)
     * @return JWT token as string
     */
    private String generateToken(Long userId, long duration) {
        // Step 1: Generate UUID for this token
        String tokenUuid = UUID.randomUUID().toString();
        log.debug("Generated token UUID: {} for user: {}", tokenUuid, userId);

        // Step 2: Get current time
        Date now = new Date();
        
        // Step 3: Calculate expiration time
        Date expirationDate = new Date(now.getTime() + duration);

        // Step 4: Build and sign JWT
        String token = Jwts.builder()
                // Subject: Who this token belongs to (user ID)
                .subject(userId.toString())
                
                // Custom claim: Token UUID for server-side logout
                .claim("tokenUuid", tokenUuid)
                
                // Issued at: When was this token created
                .issuedAt(now)
                
                // Expiration: When will this token become invalid
                .expiration(expirationDate)
                
                // Signing algorithm and key
                // HS256 = HMAC with SHA-256
                // Uses getSigningKey() to get the secret key
                .signWith(getSigningKey())
                
                // Compact: Convert to JWT string format
                // Result: "Header.Payload.Signature"
                .compact();

        log.info("Generated token for user: {} with UUID: {}", userId, tokenUuid);
        return token;
    }

    /**
     * Validate a JWT token
     * 
     * WHEN CALLED: When request comes in with Authorization header
     * On each request to protected endpoint (Diagram 4)
     * 
     * WHAT IT DOES:
     * 1. Try to parse token using secret key
     * 2. If signature doesn't match → ParseException (tampered token)
     * 3. If expiration date passed → ExpiredJwtException (token expired)
     * 4. If all checks pass → return true
     * 
     * Security checks performed automatically:
     * - Signature verification: Token hasn't been modified
     * - Expiration check: Token isn't too old
     * - Format check: Token is valid JWT format
     * 
     * EXCEPTIONS THROWN:
     * - JwtException: If signature invalid or token malformed
     * - ExpiredJwtException: If expiration date has passed
     * - IllegalArgumentException: If token is empty
     * 
     * @param token JWT token string from Authorization header
     * @return true if token is valid, false if expired
     * @throws Exception if token is malformed or tampered
     */
    public boolean validateToken(String token) {
        try {
            // Parse token using secret key
            // If signature doesn't match secret → throws JwtException
            // If token is expired → throws ExpiredJwtException
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);  // parseSignedClaims validates signature

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

    /**
     * Extract user ID from token
     * 
     * WHEN CALLED: After token validation passes (Diagram 4)
     * 
     * WHAT IT DOES:
     * 1. Parse token (WITHOUT verifying signature - assumes already validated)
     * 2. Extract Claims (payload)
     * 3. Get "sub" claim: the user ID
     * 4. Convert from String to Long and return
     * 
     * Why we don't validate here?
     * - validateToken() already checked signature
     * - We know token is safe to parse
     * - Parsing is faster than validation
     * 
     * @param token JWT token string
     * @return User ID as Long
     * @throws Exception if token is malformed
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getTokenClaims(token);
        String userId = claims.getSubject();
        return Long.valueOf(userId);
    }

    /**
     * Extract token UUID from token
     * 
     * WHEN CALLED: After token validation passes (Diagram 4, Step 2)
     * 
     * WHAT IT DOES:
     * 1. Parse token
     * 2. Extract Claims (payload)
     * 3. Get "tokenUuid" custom claim
     * 4. Return as String
     * 
     * This UUID is used to:
     * 1. Look up in Redis/Cache: tokenUuid → userId
     * 2. Verify consistency: cached_userId == token_userId
     * 3. Implement logout: delete UUID from cache
     * 
     * @param token JWT token string
     * @return Token UUID as String
     */
    public String getTokenUuidFromToken(String token) {
        Claims claims = getTokenClaims(token);
        return claims.get("tokenUuid", String.class);
    }

    /**
     * Extract all claims (payload) from token
     * 
     * INTERNAL USE: Called by other methods to extract user ID and token UUID
     * 
     * WHAT IT DOES:
     * 1. Create parser with signing key
     * 2. Parse JWT
     * 3. Extract and return Claims object
     * 
     * Claims object is like a Map that contains:
     * - Standard claims: sub, iat, exp
     * - Custom claims: tokenUuid
     * 
     * Example claims:
     * {
     *   "sub": "1",
     *   "tokenUuid": "abc-123-def",
     *   "iat": 1632339650,
     *   "exp": 1632340550
     * }
     * 
     * Note: This doesn't validate signature/expiration
     * Use validateToken() first to ensure token is safe
     * 
     * @param token JWT token string
     * @return Claims object containing all payload data
     */
    private Claims getTokenClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get the signing key from secret
     * 
     * INTERNAL USE: Used for signing and verifying tokens
     * 
     * WHAT IT DOES:
     * 1. Take jwtSecret string from properties
     * 2. Convert to SecretKey using HS256 algorithm
     * 3. Return SecretKey object
     * 
     * Why not just use string?
     * - JJWT requires SecretKey object
     * - JJWT validates key length (minimum 32 chars for HS256)
     * - Ensures we're using correct algorithm
     * 
     * Keys.hmacShaKeyFor():
     * - Takes byte array (secret string as bytes)
     * - Returns SecretKey for HMAC-SHA256
     * - Validates key is long enough
     * 
     * @return SecretKey for signing/verifying
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}

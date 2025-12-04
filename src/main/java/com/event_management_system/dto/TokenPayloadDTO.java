package com.event_management_system.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TokenPayloadDTO: Internal representation of data inside a JWT token
 * 
 * This DTO represents the "payload" (claims) section of a JWT token.
 * It's NOT sent to client, but is created when generating token and read when validating token.
 * 
 * JWT Structure (3 parts separated by dots):
 * Header.Payload.Signature
 * 
 * This DTO represents the Payload section decoded as JSON:
 * 
 * Example JWT Token:
 * eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidG9rZW5VdWlkIjoiYWJjLTEyMy1kZWYiLCJpYXQiOjE2MzIzMzk2NTAsImV4cCI6MTYzMjM0MDU1MH0.signature...
 * 
 * Decoded Payload (this DTO):
 * {
 *     "sub": "1",
 *     "tokenUuid": "abc-123-def",
 *     "iat": 1632339650,
 *     "exp": 1632340550
 * }
 * 
 * FLOW (from Diagrams 3 & 4):
 * 
 * Step 1: LOGIN (Diagram 3)
 * - User sends email & password
 * - Server creates UUID (unique identifier for this login session)
 * - Server creates TokenPayload with:
 *   - sub: user's ID
 *   - tokenUuid: the UUID we just created
 *   - iat: issued at (current time)
 *   - exp: expiration (current time + 15 minutes)
 * - Server signs this payload with secret → creates JWT
 * - Server caches: tokenUuid → userId mapping in Redis
 * 
 * Step 2: SUBSEQUENT REQUEST (Diagram 4)
 * - Client sends JWT in Authorization header
 * - Server extracts and parses JWT → gets TokenPayload
 * - Server retrieves cached userId using tokenUuid
 * - Server compares: cached_userId == payload.sub (must match)
 * - If match → User is authenticated ✓
 * - If no match → Token tampering detected → HTTP 401
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenPayloadDTO {

    /**
     * Subject: User ID (as string)
     * 
     * Standard JWT claim: "sub"
     * 
     * Contains the user's ID who owns this token
     * 
     * Example: "1" (user with ID 1)
     * 
     * Used to:
     * 1. Know which user is making the request
     * 2. Load user's roles & permissions from database
     * 3. Compare with cached user_id from tokenUuid
     */
    private String subject;

    /**
     * Token UUID: Unique identifier for this login session
     * 
     * Custom claim: "tokenUuid"
     * 
     * Purpose: Allow server to invalidate tokens (logout)
     * 
     * Example UUID: "550e8400-e29b-41d4-a716-446655440000"
     * 
     * How it works:
     * 1. User logs in → Server generates UUID
     * 2. Server stores: UUID → userId mapping in Redis
     * 3. User logs out → Server removes: UUID from Redis
     * 4. Even if token is valid → Redis lookup returns nothing → HTTP 401
     * 
     * This allows server-side logout without modifying token
     * (Tokens are stateless, but UUID cache makes it stateful)
     * 
     * Extra security:
     * - Compare cached_userId == token.subject
     * - If someone tries to modify token → signature breaks (caught by HMAC validation)
     * - If someone steals token UUID → can't create fake token (needs secret key)
     */
    private String tokenUuid;

    /**
     * Issued At: Time when token was created (Unix timestamp in seconds)
     * 
     * Standard JWT claim: "iat"
     * 
     * Example: 1632339650 (represents: 2021-09-22 14:00:50 UTC)
     * 
     * Used to:
     * 1. Verify token wasn't issued in the future (clock skew protection)
     * 2. Verify "sub" is recent enough (not a token from months ago)
     */
    private Date issuedAt;

    /**
     * Expiration Time: Time when token becomes invalid (Unix timestamp in seconds)
     * 
     * Standard JWT claim: "exp"
     * 
     * Example: 1632340550 (15 minutes after iat)
     * 
     * Validation:
     * - If current_time > exp_time → Token expired → HTTP 401
     * - Server automatically rejects token without needing database lookup
     * 
     * This is why we have access token (15 min) and refresh token (7 days):
     * - Access token: Short-lived, frequent validation
     * - Refresh token: Long-lived, used only when access token expires
     */
    private Date expiration;
}

package com.event_management_system.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.event_management_system.service.CustomUserDetailsService;
import com.event_management_system.service.JwtService;
import com.event_management_system.service.TokenCacheService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * JwtAuthenticationFilter: Validate JWT token on every request
 * 
 * IMPLEMENTS DIAGRAM 4: Token Verification Flow
 * 
 * PURPOSE:
 * Intercept every HTTP request
 * Extract and validate JWT token from Authorization header
 * Load user from database and populate SecurityContext
 * 
 * WHEN CALLED:
 * On every request to the API (part of Spring Security filter chain)
 * Before reaching the controller
 * 
 * FLOW (from Diagram 4):
 * 
 * 1. Extract token from Authorization header
 *    Header format: "Authorization: Bearer eyJhbGc..."
 *    Extract: "eyJhbGc..."
 * 
 * 2. Parse token & extract user ID and access token UUID
 *    JWT payload contains:
 *    - "sub": user ID (e.g., "1")
 *    - "tokenUuid": UUID for logout (e.g., "550e8400-...")
 * 
 * 3. Retrieve user ID from cache using access token UUID
 *    Redis/Cache lookup: "550e8400-..." → user ID: 1
 *    If not found → Token is invalid/expired/logged out
 * 
 * 4. Match: cached user ID == extracted user ID
 *    Must match exactly
 *    If no match → Token tampering detected
 * 
 * 5. Retrieve user from database by user ID
 *    Query: SELECT * FROM app_users WHERE id = 1
 *    Load user's role and permissions
 * 
 * 6. Check for permission (optional, or done in controller)
 *    Can be done here or using @PreAuthorize in controller
 * 
 * 7. Save user in SecurityContext
 *    SecurityContextHolder.getContext().setAuthentication(authentication)
 *    User is now authenticated for this request
 * 
 * REQUEST CONTINUES TO CONTROLLER ✓
 * 
 * ERROR SCENARIOS:
 * 
 * Scenario 1: No Authorization header
 * → No token in request
 * → Continue without authentication (public endpoint)
 * → If endpoint requires auth → Spring returns 401
 * 
 * Scenario 2: Invalid token format
 * → Header doesn't contain "Bearer "
 * → Log warning, continue without authentication
 * 
 * Scenario 3: Token signature invalid
 * → JwtService.validateToken() returns false
 * → Someone modified the token
 * → Continue without authentication → 401
 * 
 * Scenario 4: Token expired
 * → validateToken() throws ExpiredJwtException
 * → Continue without authentication → 401
 * 
 * Scenario 5: Token UUID not in cache
 * → User has logged out
 * → Continue without authentication → 401
 * 
 * Scenario 6: User ID mismatch
 * → cached_userId != token_userId
 * → Token tampering suspected
 * → Continue without authentication → 401
 * 
 * Scenario 7: User not found in database
 * → User was deleted or ID is invalid
 * → Continue without authentication → 401
 * 
 * WHY OncePerRequestFilter?
 * - Ensures this filter runs exactly once per request
 * - Handles servlet dispatch requests correctly
 * - Thread-safe
 * 
 * WHY NOT return 401 on error?
 * - Let Spring Security's ExceptionHandler deal with authentication errors
 * - Cleaner error responses
 * - Consistent with security best practices
 * - Public endpoints don't require authentication
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenCacheService tokenCacheService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    /**
     * Filter method: Called on every HTTP request
     * 
     * STEPS:
     * 1. Try to extract JWT token from Authorization header
     * 2. If token exists, validate it
     * 3. If valid, load user and set in SecurityContext
     * 4. If any error, continue without authentication
     * 5. Continue to next filter/controller
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Chain of filters to continue to
     */
    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // STEP 1: Extract JWT token from Authorization header (Diagram 4, Step 1)
            String jwt = extractTokenFromHeader(request);

            // If no token found, continue without authentication
            if (jwt == null) {
                log.debug("No JWT token found in request");
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("JWT token extracted from request header");

            // STEP 2: Validate token (signature and expiration) (Diagram 4, Step 1)
            if (!jwtService.validateToken(jwt)) {
                log.warn("JWT token is invalid or expired");
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("JWT token validated successfully");

            // STEP 3: Extract user ID and token UUID from JWT (Diagram 4, Step 2)
            Long userIdFromToken = jwtService.getUserIdFromToken(jwt);
            String tokenUuid = jwtService.getTokenUuidFromToken(jwt);

            log.debug("Extracted from token - User ID: {}, Token UUID: {}", userIdFromToken, tokenUuid);

            // STEP 4: Retrieve user ID from cache using token UUID (Diagram 4, Step 3)
            Long cachedUserId = tokenCacheService.getUserIdFromCache(tokenUuid);

            // If token UUID not in cache → token invalid/expired/logged out
            if (cachedUserId == null) {
                log.warn("Token UUID not found in cache (user may have logged out): {}", tokenUuid);
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("Token UUID found in cache with user ID: {}", cachedUserId);

            // STEP 5: Match cached user ID with extracted user ID (Diagram 4, Step 4)
            if (!userIdFromToken.equals(cachedUserId)) {
                log.error("User ID mismatch! Token: {}, Cache: {} (possible token tampering)", 
                          userIdFromToken, cachedUserId);
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("User ID consistency check passed");

            // STEP 6: Retrieve user from database (Diagram 4, Step 5)
            UserDetails userDetails = customUserDetailsService.loadUserDetailsById(userIdFromToken);

            log.info("User loaded from database: {}", userDetails.getUsername());

            // STEP 7: Save user in SecurityContext (Diagram 4, Step 7)
            // Create authentication token with user details and authorities
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                            userDetails,           // Principal: User object
                            null,                  // Credentials: null (already validated)
                            userDetails.getAuthorities()  // Authorities: Roles + Permissions
                    );

            // Add request details (IP address, session ID, etc.)
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // Store in SecurityContext (accessible throughout this request)
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("User authenticated and stored in SecurityContext");
            log.info("Request authenticated for user: {} with {} authorities", 
                     userDetails.getUsername(), userDetails.getAuthorities().size());

        } catch (JwtException | IllegalArgumentException | NullPointerException | UsernameNotFoundException | IOException | ServletException e) {
            if (e instanceof IOException || e instanceof ServletException) {
                log.error("IO or Servlet error in JWT filter: {}", e.getMessage());
                throw (IOException) e;
            }
            log.error("JWT processing error: {}", e.getMessage());
            // Continue without authentication
        }

        // Continue to next filter or controller
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     * 
     * WHAT IT DOES:
     * 1. Get Authorization header from request
     * 2. Check if header starts with "Bearer "
     * 3. Extract token part (everything after "Bearer ")
     * 4. Validate token is not empty
     * 5. Return token or null
     * 
     * HEADER FORMAT:
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI...
     * 
     * EXTRACTION:
     * Split by space: ["Bearer", "eyJhbGc..."]
     * Return: "eyJhbGc..."
     * 
     * INVALID FORMATS:
     * - No Authorization header → return null
     * - "Bearer" without token → return null
     * - "eyJhbGc..." (no Bearer prefix) → return null
     * - "Basic xyz..." (wrong auth type) → return null
     * 
     * @param request HTTP request
     * @return JWT token string, or null if not found
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        // Get Authorization header
        String authHeader = request.getHeader("Authorization");

        // Check if header exists and has "Bearer " prefix
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            // Extract token: remove "Bearer " prefix (7 characters)
            String token = authHeader.substring(7);
            
            // Validate token is not empty
            if (StringUtils.hasText(token)) {
                return token;
            }
        }

        return null;
    }
}

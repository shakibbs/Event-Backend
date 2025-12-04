package com.event_management_system.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * JwtAuthenticationFilter: Validate JWT tokens on every request
 * 
 * IMPLEMENTS DIAGRAM 4: Token Verification Flow
 * 
 * FLOW from Diagram 4:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Request comes in with Authorization: Bearer <token>         │
 * └────────────────┬────────────────────────────────────────────┘
 *                  │
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Step 1: Extract token from Authorization header             │
 * │ - Check header exists                                       │
 * │ - Verify starts with "Bearer "                              │
 * │ - Extract token substring                                   │
 * └────────────────┬────────────────────────────────────────────┘
 *                  │
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Step 2: Validate token (signature + expiration)             │
 * │ - Call JwtService.validateToken(token)                      │
 * │ - If invalid/expired: return false → continue without auth  │
 * │ - If valid: continue to Step 3                              │
 * └────────────────┬────────────────────────────────────────────┘
 *                  │
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Step 3: Extract user ID and token UUID from token           │
 * │ - Call JwtService.getUserIdFromToken(token)                 │
 * │ - Call JwtService.getTokenUuidFromToken(token)              │
 * └────────────────┬────────────────────────────────────────────┘
 *                  │
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Step 4: Verify token UUID in cache (logout check)           │
 * │ - Call TokenCacheService.getUserIdFromCache(uuid)           │
 * │ - If not found: user was logged out → continue without auth │
 * │ - If found: continue to Step 5                              │
 * └────────────────┬────────────────────────────────────────────┘
 *                  │
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Step 5: Consistency check                                   │
 * │ - Compare token_userId with cache_userId                   │
 * │ - If mismatch: continue without auth (error condition)      │
 * │ - If match: continue to Step 6                              │
 * └────────────────┬────────────────────────────────────────────┘
 *                  │
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Step 6: Load user from database                             │
 * │ - Call CustomUserDetailsService.loadUserDetailsById(userId) │
 * │ - Get UserDetails with authorities (ROLE_* and PERMISSION_*) │
 * └────────────────┬────────────────────────────────────────────┘
 *                  │
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Step 7: Store in SecurityContext                            │
 * │ - Create UsernamePasswordAuthenticationToken                │
 * │ - Set principal = UserDetails                               │
 * │ - Set authorities = loaded from database                    │
 * │ - Store in SecurityContextHolder                            │
 * └────────────────┬────────────────────────────────────────────┘
 *                  │
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Step 8: Continue filter chain                               │
 * │ - filterChain.doFilter(request, response)                   │
 * │ - Request proceeds to controller                            │
 * │ - Controller can access SecurityContext                     │
 * │ - Spring Security can verify endpoint authorization         │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * ERROR HANDLING:
 * - Any error (invalid token, user not found, etc.) → do NOT throw exception
 * - Simply continue without authentication
 * - Let Spring Security's authorization filter handle 401 responses
 * - If endpoint is public: request succeeds
 * - If endpoint is protected: Spring returns 401 Unauthorized
 * 
 * WHY CONTINUE WITHOUT THROWING?
 * - This filter shouldn't reject requests
 * - Only populate SecurityContext if token is valid
 * - Let Spring Security's AuthorizationFilter handle authorization
 * - Makes filter more resilient and configurable
 * 
 * EXTENDS OncePerRequestFilter:
 * - Guarantees filter runs exactly once per request
 * - Prevents multiple execution in RequestDispatcher scenarios
 * - Better than Filter interface which could run multiple times
 * 
 * @Component: Spring will instantiate and register this filter
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private TokenCacheService tokenCacheService;

    /**
     * Main filter method: Intercept every request and validate JWT token
     * 
     * CALLED BY: Spring Security filter chain on every request
     * 
     * FLOW:
     * 1. Try to extract token from Authorization header
     * 2. If token found:
     *    a. Validate token (signature + expiration)
     *    b. Extract user ID and token UUID
     *    c. Verify UUID in cache (not logged out)
     *    d. Consistency check (token_userId == cache_userId)
     *    e. Load user from database with authorities
     *    f. Store in SecurityContext
     * 3. Continue filter chain (doFilter)
     * 4. On any error: log and continue without auth
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Filter chain
     * @throws ServletException if servlet error
     * @throws IOException if I/O error
     */
    @Override
    @SuppressWarnings("all")
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // STEP 1: Extract token from Authorization header
            String token = extractTokenFromHeader(request);
            
            // If no token found, continue without authentication
            if (token == null) {
                log.debug("No JWT token found in Authorization header");
                filterChain.doFilter(request, response);
                return;
            }
            
            log.debug("JWT token found in Authorization header");
            
            // STEP 2: Validate token signature and expiration
            if (!jwtService.validateToken(token)) {
                log.warn("JWT token validation failed (invalid or expired)");
                filterChain.doFilter(request, response);
                return;
            }
            
            log.debug("JWT token validation passed");
            
            // STEP 3: Extract user ID and token UUID from token
            Long userId = jwtService.getUserIdFromToken(token);
            String tokenUuid = jwtService.getTokenUuidFromToken(token);
            
            log.debug("Extracted userId: {} and tokenUuid: {} from JWT token", userId, tokenUuid);
            
            // STEP 4: Verify token UUID is in cache (logout verification)
            Long cachedUserId = tokenCacheService.getUserIdFromCache(tokenUuid);
            
            if (cachedUserId == null) {
                log.warn("Token UUID not found in cache (user may have logged out)");
                filterChain.doFilter(request, response);
                return;
            }
            
            log.debug("Token UUID found in cache, cached userId: {}", cachedUserId);
            
            // STEP 5: Consistency check - ensure token userId matches cached userId
            if (!userId.equals(cachedUserId)) {
                log.error("Consistency check failed: token userId ({}) != cached userId ({})", userId, cachedUserId);
                filterChain.doFilter(request, response);
                return;
            }
            
            log.debug("Consistency check passed");
            
            // STEP 6: Load user from database with authorities
            UserDetails userDetails = customUserDetailsService.loadUserDetailsById(userId);
            
            if (userDetails == null) {
                log.warn("User not found in database: {}", userId);
                filterChain.doFilter(request, response);
                return;
            }
            
            log.debug("User loaded from database: {}, authorities: {}", userId, userDetails.getAuthorities());
            
            // STEP 7: Create and store authentication in SecurityContext
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                            userDetails,                           // principal (who is authenticated)
                            null,                                  // credentials (password - not needed for JWT)
                            userDetails.getAuthorities()           // authorities (roles and permissions)
                    );
            
            // Set request details (helpful for debugging and auditing)
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // Store in SecurityContext - available to rest of request
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("Authentication stored in SecurityContext for user: {}", userId);
            
        } catch (NumberFormatException | UsernameNotFoundException e) {
            // Error handling: Log error but don't throw
            // This allows filter to be resilient
            log.error("Error processing JWT token: {}", e.getMessage(), e);
        } catch (Exception e) {
            // Catch-all for unexpected exceptions
            // Suppress warning: can be replaced with multicatch
            log.error("Unexpected error processing JWT token: {}", e.getMessage(), e);
        }
        
        // STEP 8: Continue filter chain
        // Whether authentication was successful or failed,
        // continue to next filter and controller
        filterChain.doFilter(request, response);
    }

    /**
     * Helper method: Extract JWT token from Authorization header
     * 
     * HEADER FORMAT:
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * 
     * EXTRACTION STEPS:
     * 1. Get "Authorization" header value
     * 2. Check if header exists and is not empty (StringUtils.hasText)
     * 3. Verify header starts with "Bearer " prefix
     * 4. Extract substring after "Bearer " (7 characters)
     * 5. Verify token is not empty
     * 6. Return token string
     * 7. If any check fails: return null
     * 
     * SECURITY:
     * - Only accepts "Bearer " scheme (standard for JWT)
     * - Doesn't accept "Basic ", "Digest ", or custom schemes
     * - Validates token is not empty (no "Bearer " without token)
     * 
     * EXAMPLE:
     * Header: "Authorization: Bearer abc123def456"
     * → Extracted token: "abc123def456"
     * 
     * @param request HTTP request
     * @return JWT token string, or null if not found
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        
        return null;
    }
}

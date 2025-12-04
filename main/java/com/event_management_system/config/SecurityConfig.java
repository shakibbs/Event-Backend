package com.event_management_system.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.event_management_system.service.CustomUserDetailsService;
import com.event_management_system.security.JwtAuthenticationFilter;
import com.event_management_system.service.JwtService;
import com.event_management_system.service.CustomUserDetailsService;

/**
 * SecurityConfig: Configure Spring Security for JWT-based authentication
 * 
 * ARCHITECTURE (From Diagram 1: Spring Security Filter Chain):
 * 
 * Request → JwtAuthenticationFilter → Spring Security Filter Chain
 *          ↓
 *          1. Extract JWT token from Authorization header
 *          2. Validate token (signature, expiration)
 *          3. Verify token UUID in cache
 *          4. Load user from database
 *          5. Store in SecurityContext
 *          ↓
 * Security Context → Authorization Check (public/protected)
 *          ↓
 *          If public: Allow access
 *          If protected: Check authentication in SecurityContext
 *          ↓
 * Request → Controller → Response
 * 
 * KEY POINTS:
 * - Stateless authentication: No sessions, pure JWT
 * - SessionCreationPolicy.STATELESS: No HttpSession created
 * - JwtAuthenticationFilter: Custom filter before UsernamePasswordAuthenticationFilter
 * - Public endpoints: /api/auth/login, /api/auth/refresh, /swagger-ui.html, /api-docs/**
 * - Protected endpoints: Everything else requires authentication
 * - Password encoding: BCrypt (automatically compares on login)
 * - Exception handling: 401 Unauthorized, 403 Forbidden
 * 
 * JWT FLOW IN THIS CONFIG:
 * 1. Client: POST /api/auth/login with email/password
 * 2. AuthController.login() calls AuthService.authenticate()
 * 3. AuthService uses PasswordEncoder to verify password (BCrypt compare)
 * 4. AuthService generates JWT tokens with UUID
 * 5. Client receives tokens, stores in localStorage
 * 6. Client: Next request with Authorization: Bearer <token>
 * 7. JwtAuthenticationFilter intercepts:
 *    - Extracts token from header
 *    - Validates signature/expiration
 *    - Verifies UUID in cache
 *    - Loads user from database
 *    - Stores in SecurityContext
 * 8. Spring Security checks if endpoint is protected:
 *    - If public: Allow access
 *    - If protected: Check SecurityContext (should have authentication)
 * 9. If authentication needed but missing: Return 401 Unauthorized
 * 10. If role/permission denied: Return 403 Forbidden
 * 
 * BCRYPT PASSWORD ENCODER:
 * - One-way hashing (can't decrypt)
 * - Uses salt (random per password)
 * - Takes ~100ms per comparison (intentional slowdown for brute force protection)
 * - Login flow:
 *   1. User enters password: "password123"
 *   2. PasswordEncoder.matches(plaintext, hashedPassword)
 *   3. Hash "password123" and compare with stored hash
 *   4. If match: Authentication successful
 *   5. If no match: Authentication failed
 * 
 * FILTER CHAIN ORDER (Important!):
 * Request → JwtAuthenticationFilter (FIRST - extract & validate token)
 *        → UsernamePasswordAuthenticationFilter (login/logout form-based)
 *        → AuthorizationFilter (check if authorized for endpoint)
 *        → ... other filters ...
 * 
 * WHY BEFORE UsernamePasswordAuthenticationFilter?
 * - UsernamePasswordAuthenticationFilter expects form-based login
 * - JwtAuthenticationFilter handles bearer token login
 * - Must run first to populate SecurityContext for downstream filters
 * 
 * @EnableWebSecurity: Enable Spring Security web security
 * @EnableMethodSecurity: Enable @PreAuthorize, @PostAuthorize on methods
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Define password encoder bean: BCryptPasswordEncoder
     * 
     * SECURITY:
     * - Uses BCrypt algorithm with strength 12
     * - Automatically handles salt generation
     * - One-way hashing (cannot decrypt)
     * - Protects against brute force attacks
     * 
     * @return BCryptPasswordEncoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication provider using UserDetailsService and PasswordEncoder
     * 
     * This bean configures how authentication is performed:
     * 1. UserDetailsService: Loads user from database
     * 2. PasswordEncoder: Verifies password using BCrypt
     * 
     * Used during AuthenticationManager initialization.
     * 
     * @return DaoAuthenticationProvider
     */
    @SuppressWarnings("deprecation")
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(customUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    /**
     * Define authentication manager bean
     * 
     * AUTHENTICATION MANAGER:
     * - Central component that authenticates users
     * - Uses AuthenticationProvider (configured above)
     * - AuthenticationProvider uses UserDetailsService + PasswordEncoder
     * 
     * WHEN USED:
     * 1. During Spring Security configuration
     * 2. For form-based login (if needed)
     * 3. NOT used for JWT (JwtAuthenticationFilter handles it)
     * 
     * FLOW:
     * 1. AuthenticationManager.authenticate(token)
     * 2. Passes to AuthenticationProvider
     * 3. AuthenticationProvider loads user and verifies password
     * 4. Returns authenticated token
     * 
     * JWT FLOW (doesn't use AuthenticationManager):
     * - JwtAuthenticationFilter directly loads user from database
     * - Doesn't use PasswordEncoder (already authenticated via token signature)
     * - Creates authentication manually and stores in SecurityContext
     * 
     * @param authenticationConfiguration AuthenticationConfiguration
     * @return AuthenticationManager bean
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configure security filter chain
     * 
     * FILTER CHAIN CONFIGURATION:
     * 1. Register JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
     * 2. Set session policy to STATELESS (no HttpSession)
     * 3. Configure public endpoints (no authentication required)
     * 4. Configure protected endpoints (authentication required)
     * 5. Configure exception handling (401, 403 responses)
     * 6. Configure CORS (if needed)
     * 
     * PUBLIC ENDPOINTS (permitAll):
     * - POST /api/auth/login → login without token
     * - POST /api/auth/refresh → refresh token without access token
     * - GET /swagger-ui.html → view API documentation
     * - GET /api-docs/** → Swagger JSON
     * - GET /swagger-ui/** → Swagger UI resources
     * - POST /api/users → register new user (optional, for user registration)
     * 
     * PROTECTED ENDPOINTS (authenticate):
     * - POST /api/events → create event (requires ROLE_ADMIN or ROLE_EVENT_MANAGER)
     * - GET /api/events → list events (requires authentication)
     * - PUT /api/events/{id} → update event
     * - DELETE /api/events/{id} → delete event
     * - GET /api/users → list users
     * - GET /api/roles → list roles
     * - etc.
     * 
     * EXECUTION ORDER:
     * 1. JwtAuthenticationFilter (extract & validate token)
     * 2. Spring Security checks if endpoint is public
     * 3. If public: Allow access
     * 4. If protected: Check SecurityContext for authentication
     * 5. If authenticated: Allow access
     * 6. If not authenticated: Return 401 Unauthorized
     * 7. AuthorizationFilter checks roles/permissions
     * 8. If authorized: Allow access
     * 9. If not authorized: Return 403 Forbidden
     * 
     * STATELESS AUTHENTICATION:
     * - No session cookie stored
     * - No HttpSession created
     * - Each request must include Authorization header
     * - Suitable for REST APIs and mobile apps
     * - Scalable across multiple servers (no session affinity needed)
     * 
     * @param httpSecurity HttpSecurity builder
     * @return SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC ENDPOINTS
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        
                        // Swagger documentation
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        
                        // User registration endpoint
                        .requestMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                        
                        // ALL OTHER ENDPOINTS - Require authentication
                        .anyRequest().authenticated()
                )
                
                // Add JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                
                // Exception handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(401, "Unauthorized");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendError(403, "Forbidden");
                        })
                );

        return httpSecurity.build();
    }
}

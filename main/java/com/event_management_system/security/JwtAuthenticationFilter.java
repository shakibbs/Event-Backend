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


@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenCacheService tokenCacheService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;


    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = extractTokenFromHeader(request);

            if (jwt == null) {
                log.debug("No JWT token found in request");
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("JWT token extracted from request header");

            if (!jwtService.validateToken(jwt)) {
                log.warn("JWT token is invalid or expired");
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("JWT token validated successfully");

            Long userIdFromToken = jwtService.getUserIdFromToken(jwt);
            String tokenUuid = jwtService.getTokenUuidFromToken(jwt);

            log.debug("Extracted from token - User ID: {}, Token UUID: {}", userIdFromToken, tokenUuid);

            Long cachedUserId = tokenCacheService.getUserIdFromCache(tokenUuid);

            if (cachedUserId == null) {
                log.warn("Token UUID not found in cache (user may have logged out): {}", tokenUuid);
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("Token UUID found in cache with user ID: {}", cachedUserId);

            if (!userIdFromToken.equals(cachedUserId)) {
                log.error("User ID mismatch! Token: {}, Cache: {} (possible token tampering)", 
                          userIdFromToken, cachedUserId);
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("User ID consistency check passed");

            UserDetails userDetails = customUserDetailsService.loadUserDetailsById(userIdFromToken);
            
            log.info("User loaded from database: {}", userDetails.getUsername());
            
            if (userDetails != null && userDetails instanceof com.event_management_system.entity.User) {
                log.debug("User is of correct type: {}", userDetails.getClass().getName());
            } else {
                log.warn("User is not of expected type: {}", userDetails != null ? userDetails.getClass().getName() : "null");
            }

            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                            userDetails,           // Principal: User object
                            null,                  // Credentials: null (already validated)
                            userDetails.getAuthorities()  // Authorities: Roles + Permissions
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("User authenticated and stored in SecurityContext");
            log.info("Request authenticated for user: {} with {} authorities",
                      userDetails.getUsername(), userDetails.getAuthorities().size());
            
            if (userDetails != null && userDetails instanceof com.event_management_system.entity.User) {
                log.debug("User is of correct type: {}", userDetails.getClass().getName());
            } else {
                log.warn("User is not of expected type: {}", userDetails != null ? userDetails.getClass().getName() : "null");
            }

        } catch (JwtException | IllegalArgumentException | NullPointerException | UsernameNotFoundException | IOException | ServletException e) {
            if (e instanceof IOException || e instanceof ServletException) {
                log.error("IO or Servlet error in JWT filter: {}", e.getMessage());
                throw (IOException) e;
            }
            log.error("JWT processing error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }


    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            if (StringUtils.hasText(token)) {
                return token;
            }
        }

        return null;
    }
}

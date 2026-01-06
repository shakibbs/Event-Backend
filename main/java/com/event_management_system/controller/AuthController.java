package com.event_management_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.event_management_system.dto.AuthResponseDTO;
import com.event_management_system.dto.ChangePasswordRequestDTO;
import com.event_management_system.dto.LoginRequestDTO;
import com.event_management_system.dto.RefreshTokenRequestDTO;
import com.event_management_system.entity.User;
import com.event_management_system.repository.UserRepository;
import com.event_management_system.service.ApplicationLoggerService;
import com.event_management_system.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ApplicationLoggerService logger;

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticate user with email and password. Returns access token, refresh token, and user details."
    )
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequest,
            jakarta.servlet.http.HttpServletRequest request) {
        
        try {
            logger.traceWithContext("AuthController", "login() called with email={}, timestamp={}", loginRequest.getEmail(), System.currentTimeMillis());
            logger.debugWithContext("AuthController", "POST /api/auth/login - Login attempt: email={}", loginRequest.getEmail());
            AuthResponseDTO authResponse = authService.authenticate(loginRequest, request);
            logger.infoWithContext("AuthController", "Login successful: userId={}, email={}", authResponse.getUser().getId(), loginRequest.getEmail());
            
            return new ResponseEntity<>(authResponse, HttpStatus.OK);
            
        } catch (RuntimeException e) {
            logger.warnWithContext("AuthController", "Login failed: Invalid credentials for email={}, error={}", loginRequest.getEmail(), e.getMessage());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            logger.errorWithContext("AuthController", "Failed to login: email={}", e);
            throw e;
        }
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Get a new access token using a valid refresh token. Refresh token remains the same."
    )
    public ResponseEntity<AuthResponseDTO> refreshAccessToken(
            @Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequest) {
        
        try {
            logger.traceWithContext("AuthController", "refreshAccessToken() called with timestamp={}", System.currentTimeMillis());
            logger.debugWithContext("AuthController", "POST /api/auth/refresh - Refresh token request received");
            AuthResponseDTO authResponse = authService.refreshAccessToken(refreshTokenRequest.getRefreshToken());
            logger.infoWithContext("AuthController", "Access token refreshed successfully: userId={}", authResponse.getUser().getId());
            
            return new ResponseEntity<>(authResponse, HttpStatus.OK);
            
        } catch (RuntimeException e) {
            logger.warnWithContext("AuthController", "Token refresh failed: Invalid or expired token, error={}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            logger.errorWithContext("AuthController", "Failed to refresh access token", e);
            throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(
            summary = "User logout",
            description = "Logout user and invalidate their tokens. Requires valid access token in Authorization header."
    )
    public ResponseEntity<?> logout(
            @Parameter(description = "Authorization header with Bearer token")
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            jakarta.servlet.http.HttpServletRequest request) {
        
        try {
            logger.traceWithContext("AuthController", "logout() called with timestamp={}", System.currentTimeMillis());
            logger.debugWithContext("AuthController", "POST /api/auth/logout - Logout request received");
            String token = extractTokenFromHeader(authorizationHeader);
            
            if (token == null) {
                logger.warnWithContext("AuthController", "Logout failed: No token in Authorization header");
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
            
            authService.logout(token, request);
            logger.infoWithContext("AuthController", "User logged out successfully");
            
            return new ResponseEntity<>(
                    new LogoutResponseDTO("Logged out successfully"),
                    HttpStatus.OK
            );
           
        } catch (RuntimeException e) {
            logger.warnWithContext("AuthController", "Logout failed: error={}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            logger.errorWithContext("AuthController", "Failed to logout", e);
            throw e;
        }
    }

    private String extractTokenFromHeader(String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        return null;
    }

    public static class LogoutResponseDTO {
        public String message;

        public LogoutResponseDTO(String message) {
            this.message = message;
        }
    }
    

    @PostMapping("/change-password")
    @Operation(
        summary = "Change user password",
        description = "Allows authenticated user to change their own password. Requires old password verification."
    )
    public ResponseEntity<MessageResponseDTO> changePassword(
            @Parameter(description = "Password change request with old password, new password, and confirmation")
            @Valid @RequestBody ChangePasswordRequestDTO changePasswordRequest) {
        
        try {
            logger.traceWithContext("AuthController", "changePassword() called with timestamp={}", System.currentTimeMillis());
            logger.debugWithContext("AuthController", "POST /api/auth/change-password - Password change request received");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warnWithContext("AuthController", "Password change failed: User not authenticated");
                return new ResponseEntity<>(
                    new MessageResponseDTO("User not authenticated"),
                    HttpStatus.UNAUTHORIZED
                );
            }
            
            org.springframework.security.core.userdetails.UserDetails userDetails =
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            
            logger.debugWithContext("AuthController", "User authenticated: userId={}, email={}", user.getId(), email);
            authService.changePassword(
                user.getId(),
                changePasswordRequest.getOldPassword(),
                changePasswordRequest.getNewPassword(),
                changePasswordRequest.getConfirmPassword()
            );
            logger.infoWithContext("AuthController", "Password changed successfully: userId={}, email={}", user.getId(), email);
            
            return new ResponseEntity<>(
                new MessageResponseDTO("Password changed successfully"),
                HttpStatus.OK
            );
           
        } catch (RuntimeException e) {
            logger.warnWithContext("AuthController", "Password change failed: Invalid password, error={}", e.getMessage());
            return new ResponseEntity<>(
                new MessageResponseDTO(e.getMessage()),
                HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            logger.errorWithContext("AuthController", "Failed to change password", e);
            return new ResponseEntity<>(
                new MessageResponseDTO("An error occurred while changing password"),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    public static class MessageResponseDTO {
        public String message;

        public MessageResponseDTO(String message) {
            this.message = message;
        }
    }
}

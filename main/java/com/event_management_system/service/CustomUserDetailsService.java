package com.event_management_system.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.entity.Permission;
import com.event_management_system.entity.Role;
import com.event_management_system.entity.RolePermission;
import com.event_management_system.entity.User;
import com.event_management_system.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * CustomUserDetailsService: Load user and build authorities (roles + permissions)
 * 
 * PURPOSE:
 * Implement Spring Security's UserDetailsService interface
 * Load user from database and build complete authority list (roles + permissions)
 * 
 * FLOW from Diagrams:
 * 
 * AUTHENTICATION (Spring Security Filter):
 * When user logs in or request comes with valid token:
 * 1. JwtAuthenticationFilter extracts user ID from token
 * 2. JwtAuthenticationFilter calls UserDetailsService.loadUserByUsername(userId)
 * 3. CustomUserDetailsService:
 *    - Query database: SELECT user FROM users WHERE id = ?
 *    - Load user's role
 *    - Load role's permissions via RolePermission junction table
 *    - Build authorities: ROLE_ADMIN, PERMISSION_CREATE_EVENT, etc.
 *    - Return UserDetails object (contains user info + authorities)
 * 4. Spring Security stores in SecurityContextHolder
 * 5. Controller can access: SecurityContextHolder.getContext().getAuthentication()
 * 
 * AUTHORIZATION (Method-level security):
 * When controller method has @PreAuthorize("hasRole('ADMIN')"):
 * 1. Spring checks: user.getAuthorities() contains "ROLE_ADMIN"?
 * 2. If yes → Allow access
 * 3. If no → Throw AccessDeniedException (HTTP 403)
 * 
 * DATABASE STRUCTURE:
 * 
 * ┌──────────────────────────┐
 * │ User (user_id=1)         │
 * │ - email: admin@email.com │
 * │ - password: bcrypt_hash  │
 * │ - role_id: 1             │
 * └──────────────┬───────────┘
 *                │ (ManyToOne)
 *                ▼
 * ┌──────────────────────────┐
 * │ Role (role_id=1)         │
 * │ - name: "ADMIN"          │
 * └──────────────┬───────────┘
 *                │ (OneToMany)
 *                ▼
 * ┌──────────────────────────────────┐
 * │ RolePermission (junction table)  │
 * │ - role_id: 1                     │
 * │ - permission_id: 1 → CREATE_EVENT│
 * │ - permission_id: 2 → DELETE_USER │
 * │ - permission_id: 3 → EDIT_ROLE   │
 * └──────────────┬────────────────────┘
 *                │
 *                ▼
 * ┌──────────────────────────────┐
 * │ Permission                   │
 * │ - id: 1 → CREATE_EVENT       │
 * │ - id: 2 → DELETE_USER        │
 * │ - id: 3 → EDIT_ROLE          │
 * └──────────────────────────────┘
 * 
 * AUTHORITIES BUILT FROM DATABASE:
 * 1. Role ADMIN → Authority: "ROLE_ADMIN"
 * 2. Permission CREATE_EVENT → Authority: "PERMISSION_CREATE_EVENT"
 * 3. Permission DELETE_USER → Authority: "PERMISSION_DELETE_USER"
 * 4. Permission EDIT_ROLE → Authority: "PERMISSION_EDIT_ROLE"
 * 
 * Final UserDetails.authorities:
 * [
 *   ROLE_ADMIN,
 *   PERMISSION_CREATE_EVENT,
 *   PERMISSION_DELETE_USER,
 *   PERMISSION_EDIT_ROLE
 * ]
 * 
 * AUTHORIZATION CHECKS IN CONTROLLERS:
 * @PreAuthorize("hasRole('ADMIN')")
 * → Spring checks: authorities.contains("ROLE_ADMIN") ✓
 * 
 * @PreAuthorize("hasAuthority('PERMISSION_CREATE_EVENT')")
 * → Spring checks: authorities.contains("PERMISSION_CREATE_EVENT") ✓
 * 
 * NAMING CONVENTION:
 * - Roles: Prefix with "ROLE_" → "ROLE_ADMIN", "ROLE_USER"
 * - Permissions: Prefix with "PERMISSION_" → "PERMISSION_CREATE_EVENT"
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Load user by username (In our case, username = user ID as string)
     * 
     * WHEN CALLED:
     * 1. During login: AuthService.authenticate() calls this
     * 2. During request: JwtAuthenticationFilter calls this
     * 3. When Spring needs to rebuild authentication
     * 
     * WHY "USERNAME"?
     * Spring Security interface uses "username" parameter
     * We use user ID converted to String (userId = "1")
     * Could also use email, but we chose ID for token consistency
     * 
     * FLOW:
     * 1. Try to find user by ID: userRepository.findById(Long.parseLong(userId))
     * 2. If not found → throw UsernameNotFoundException
     * 3. If found → Build UserDetails with:
     *    - User entity data (email, password, account status)
     *    - Authorities (roles + permissions)
     * 4. Return UserDetails
     * 
     * @param userId User ID as string (converted from token or login)
     * @return UserDetails with user data and authorities
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        try {
            // Convert userId string to Long
            log.debug("Loading user details for user ID: {}", userId);

            // Query database: SELECT * FROM app_users WHERE id = ?
            Long parsedUserId = Long.valueOf(userId);
            if (parsedUserId == null) {
                throw new UsernameNotFoundException("Invalid user ID: " + userId);
            }
            User user = userRepository.findById(parsedUserId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

            log.info("User found: {}", user.getEmail());

            // Build authorities from roles and permissions
            Collection<GrantedAuthority> authorities = buildAuthorities(user);

            // Create Spring Security UserDetails object
            return org.springframework.security.core.userdetails.User.builder()
                    // Username: Use email for display (easier to read in logs)
                    .username(user.getEmail())
                    
                    // Password: BCrypt hash from database (for authentication)
                    .password(user.getPassword())
                    
                    // Authorities: Roles and permissions
                    .authorities(authorities)
                    
                    .build();

        } catch (NumberFormatException e) {
            log.error("Invalid user ID format: {}", userId);
            throw new UsernameNotFoundException("Invalid user ID format: " + userId);
        }
    }

    /**
     * Build authorities from user's roles and permissions
     * 
     * STEPS:
     * 1. Get user's role (ManyToOne relationship)
     * 2. Add role as authority with "ROLE_" prefix
     * 3. Get role's permissions (via RolePermission junction table)
     * 4. Add each permission as authority with "PERMISSION_" prefix
     * 5. Return complete authority list
     * 
     * EXAMPLE:
     * User → Role "ADMIN"
     *         ├─ Permission "CREATE_EVENT"
     *         ├─ Permission "DELETE_USER"
     *         └─ Permission "EDIT_ROLE"
     * 
     * Returns:
     * [
     *   SimpleGrantedAuthority("ROLE_ADMIN"),
     *   SimpleGrantedAuthority("PERMISSION_CREATE_EVENT"),
     *   SimpleGrantedAuthority("PERMISSION_DELETE_USER"),
     *   SimpleGrantedAuthority("PERMISSION_EDIT_ROLE")
     * ]
     * 
     * NAMING CONVENTION:
     * - Roles: Prefix with "ROLE_" (Spring Security standard)
     * - Permissions: Prefix with "PERMISSION_" (our convention)
     * 
     * WHY PREFIX?
     * Helps distinguish between role-based and permission-based checks:
     * @PreAuthorize("hasRole('ADMIN')")        → Looks for "ROLE_ADMIN"
     * @PreAuthorize("hasAuthority('PERMISSION_CREATE_EVENT')")  → Looks for exact match
     * 
     * @param user User entity with role relationship loaded
     * @return Collection of GrantedAuthority objects
     */
    private Collection<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Step 1: Get user's role
        Role role = user.getRole();
        if (role != null) {
            // Step 2: Add role as authority with "ROLE_" prefix
            String roleName = "ROLE_" + role.getName().toUpperCase();
            authorities.add(new SimpleGrantedAuthority(roleName));
            log.debug("Added authority: {}", roleName);

            // Step 3: Get role's permissions via RolePermission junction table
            Set<RolePermission> rolePermissions = role.getRolePermissions();
            if (rolePermissions != null && !rolePermissions.isEmpty()) {
                // Step 4: Add each permission as authority with "PERMISSION_" prefix
                for (RolePermission rolePermission : rolePermissions) {
                    Permission permission = rolePermission.getPermission();
                    if (permission != null) {
                        String permissionName = "PERMISSION_" + permission.getName().toUpperCase();
                        authorities.add(new SimpleGrantedAuthority(permissionName));
                        log.debug("Added authority: {}", permissionName);
                    }
                }
            }
        } else {
            log.warn("User {} has no role assigned", user.getId());
        }

        // Step 5: Return complete authority list
        log.debug("Built {} authorities for user {}", authorities.size(), user.getId());
        return authorities;
    }

    /**
     * Load user by ID (Convenience method)
     * 
     * WHEN CALLED: When you need UserDetails by ID directly
     * 
     * EXAMPLE:
     * UserDetails userDetails = loadUserDetailsById(1L);
     * 
     * @param userId User ID
     * @return UserDetails
     * @throws UsernameNotFoundException if user not found
     */
    public UserDetails loadUserDetailsById(Long userId) throws UsernameNotFoundException {
        return loadUserByUsername(userId.toString());
    }

    /**
     * Load user by email (Alternative method)
     * 
     * WHEN CALLED: When you need to look up user by email (login)
     * 
     * EXAMPLE:
     * UserDetails userDetails = loadUserByEmail("user@example.com");
     * 
     * @param email User email
     * @return UserDetails
     * @throws UsernameNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        log.debug("Loading user details by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return loadUserByUsername(user.getId().toString());
    }
}

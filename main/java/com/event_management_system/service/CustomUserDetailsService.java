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


@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private ApplicationLoggerService log;

    @Autowired
    private UserRepository userRepository;

    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        try {
            log.debug("Loading user details for user ID: {}", userId);

            Long parsedUserId = Long.valueOf(userId);
            if (parsedUserId == null) {
                throw new UsernameNotFoundException("Invalid user ID: " + userId);
            }
            User user = userRepository.findById(parsedUserId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

            log.info("User found: {}", user.getEmail());

            Collection<GrantedAuthority> authorities = buildAuthorities(user);

            return org.springframework.security.core.userdetails.User.builder()
                
                    .username(user.getEmail())
                    
                    .password(user.getPassword())
                    
                   .authorities(authorities)
                    
                    .build();

        } catch (NumberFormatException e) {
            log.error("Invalid user ID format: {}", userId);
            throw new UsernameNotFoundException("Invalid user ID format: " + userId);
        }
    }


    private Collection<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        Role role = user.getRole();
        if (role != null) {
            String roleName = "ROLE_" + role.getName().toUpperCase();
            authorities.add(new SimpleGrantedAuthority(roleName));
            log.debug("Added authority: {}", roleName);

            Set<RolePermission> rolePermissions = role.getRolePermissions();
            if (rolePermissions != null && !rolePermissions.isEmpty()) {
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

        log.debug("Built {} authorities for user {}", authorities.size(), user.getId());
        return authorities;
    }

    
    public UserDetails loadUserDetailsById(Long userId) throws UsernameNotFoundException {
        return loadUserByUsername(userId.toString());
    }


    @Transactional(readOnly = true)
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        log.debug("Loading user details by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return loadUserByUsername(user.getId().toString());
    }
}

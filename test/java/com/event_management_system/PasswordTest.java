package com.event_management_system;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String rawPassword = "SuperAdmin@123";
        String storedHash = "$2a$12$xZKX2eIrTrZe3M9laYB33e/aMrPOw73URDjFKExrKUHOU60.ANdXi";
        
        boolean matches = encoder.matches(rawPassword, storedHash);
        
        System.out.println("Password matches: " + matches);
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Stored hash: " + storedHash);
        
        // Generate a new hash
        String newHash = encoder.encode(rawPassword);
        System.out.println("New hash: " + newHash);
    }
}

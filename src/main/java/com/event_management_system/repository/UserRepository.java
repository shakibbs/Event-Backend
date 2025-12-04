package com.event_management_system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email (unique constraint)
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if user exists by ID
     */
    @Override
    boolean existsById(@NonNull Long id);
}
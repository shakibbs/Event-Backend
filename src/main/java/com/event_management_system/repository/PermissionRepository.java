package com.event_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    List<Permission> findAllByDeletedFalse();
    
    Optional<Permission> findByName(String name);
}
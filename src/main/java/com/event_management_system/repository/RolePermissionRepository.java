package com.event_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.Permission;
import com.event_management_system.entity.Role;
import com.event_management_system.entity.RolePermission;


@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.RolePermissionId> {

  
    List<RolePermission> findByRole(Role role);

  
    Optional<RolePermission> findByRoleAndPermission(Role role, Permission permission);

   
    void deleteByRoleAndPermission(Role role, Permission permission);

   
    boolean existsByRoleAndPermission(Role role, Permission permission);

   
    List<RolePermission> findByPermission(Permission permission);

    List<RolePermission> findByIdRoleId(Long roleId);

   
    List<RolePermission> findByIdPermissionId(Long permissionId);
}

package com.event_management_system.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseEntity {

    @NotBlank(message = "Role name is required")
    @Size(min = 3, max = 50, message = "Role name must be between 3 and 50 characters")
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    /**
     * Get permissions through the RolePermission junction table
     */
    public Set<Permission> getPermissions() {
        Set<Permission> permissions = new HashSet<>();
        if (rolePermissions != null) {
            for (RolePermission rp : rolePermissions) {
                if (rp.getPermission() != null) {
                    permissions.add(rp.getPermission());
                }
            }
        }
        return permissions;
    }

    /**
     * Add a permission to this role
     */
    public void addPermission(Permission permission) {
        if (rolePermissions == null) {
            rolePermissions = new HashSet<>();
        }
        RolePermission rp = new RolePermission(this, permission);
        rolePermissions.add(rp);
    }

    /**
     * Remove a permission from this role
     */
    public void removePermission(Permission permission) {
        if (rolePermissions != null) {
            rolePermissions.removeIf(rp -> rp.getPermission() != null &&
                                         rp.getPermission().getId().equals(permission.getId()));
        }
    }
}

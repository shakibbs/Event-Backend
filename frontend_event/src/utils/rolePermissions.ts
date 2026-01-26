import { User } from '../types';

/**
 * Get all permission names for a user
 * Dynamically reads from user's role permissions from backend
 */
export function getUserPermissions(user: User | null): string[] {
  if (!user || !user.role) return [];
  
  // Handle both string role and object role with permissions
  if (typeof user.role === 'string') return [];
  
  if (user.role && typeof user.role === 'object' && 'permissions' in user.role) {
    const permissions = user.role.permissions;
    if (Array.isArray(permissions)) {
      return permissions.map((p: any) => p.name || p);
    }
  }
  
  return [];
}

/**
 * Check if user has a specific permission
 * This is dynamic - no hardcoded permission checks
 */
export function hasPermission(user: User | null, permissionName: string): boolean {
  const permissions = getUserPermissions(user);
  return permissions.includes(permissionName);
}

/**
 * Check multiple permissions (user must have at least one)
 */
export function hasAnyPermission(user: User | null, permissionNames: string[]): boolean {
  const permissions = getUserPermissions(user);
  return permissionNames.some(name => permissions.includes(name));
}

/**
 * Check if user has all permissions
 */
export function hasAllPermissions(user: User | null, permissionNames: string[]): boolean {
  const permissions = getUserPermissions(user);
  return permissionNames.every(name => permissions.includes(name));
}

// Helper functions for role names (still useful for logging/display)
export function getRoleName(user: User | null): string {
  if (!user) return '';
  
  if (typeof user.role === 'string') {
    return user.role.toLowerCase();
  }
  
  if (user.role && typeof user.role === 'object' && 'name' in user.role) {
    return user.role.name.toLowerCase();
  }
  
  return '';
}

export function isSuperAdmin(user: User | null): boolean {
  return getRoleName(user) === 'superadmin';
}

export function isAdmin(user: User | null): boolean {
  const role = getRoleName(user);
  return role === 'admin' || role === 'superadmin';
}

export function isAttendee(user: User | null): boolean {
  return getRoleName(user) === 'attendee';
}

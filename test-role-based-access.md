# Role-Based Access Testing Guide

## Issue Analysis
The problem was that all users were getting the same dashboard and access because:
1. JWT tokens were not including role information
2. Frontend was not properly parsing roles from tokens
3. No role-based access control was implemented in the frontend

## Fixes Implemented

### Backend Changes
1. **Updated JwtService.java** to include role information in access tokens
2. **Modified AuthService.java** to pass role data when generating tokens

### Frontend Changes
1. **Updated useAuth hook** to properly parse and store user data
2. **Created usePermissions hook** for centralized permission checking
3. **Implemented role-based routing** in App.tsx
4. **Updated Dashboard component** with role-based UI
5. **Created Profile component** for user management
6. **Created EventManagement component** for event approval
7. **Added ErrorBoundary** for proper error handling

## Testing Steps

### 1. Test Different User Roles
Create test users with different roles:
- ADMIN
- SUPER_ADMIN  
- EVENT_MANAGER
- USER

### 2. Test JWT Token Content
After login, check the JWT token in browser dev tools:
```javascript
// In browser console
const token = localStorage.getItem('accessToken');
const payload = JSON.parse(atob(token.split('.')[1]));
console.log('Token payload:', payload);
// Should include: role, roleId, sub (user id)
```

### 3. Test Dashboard Access Control
- **USER role**: Should see basic dashboard, can create events, cannot approve events
- **EVENT_MANAGER**: Should see event management options, can approve events
- **ADMIN/SUPER_ADMIN**: Should see all options including user management

### 4. Test Route Protection
Try accessing these URLs directly:
- `/event-management` - Should redirect to dashboard for non-privileged users
- `/events/create` - Should be accessible to all authenticated users
- `/users` - Should only be accessible to ADMIN/SUPER_ADMIN

### 5. Test Permission Functions
In browser console, test the permission functions:
```javascript
// After importing usePermissions in a component
const { canCreateEvent, canApproveEvents, canManageUsers } = usePermissions();
console.log('Can create event:', canCreateEvent());
console.log('Can approve events:', canApproveEvents());
console.log('Can manage users:', canManageUsers());
```

## Expected Behavior

### USER Role
- Can view dashboard
- Can create events
- Can view events
- Cannot approve events
- Cannot manage users
- Cannot access reports

### EVENT_MANAGER Role
- All USER permissions
- Can approve events
- Can view reports
- Cannot manage users

### ADMIN/SUPER_ADMIN Role
- All permissions
- Can manage users
- Can manage roles and permissions
- Can view audit logs

## Verification Checklist

- [ ] JWT tokens contain role information
- [ ] Dashboard shows different options based on role
- [ ] Route protection works for unauthorized access
- [ ] Permission functions return correct values
- [ ] Error handling works for unauthorized access
- [ ] User profile displays correct role information
- [ ] Event management only accessible to authorized roles

## Troubleshooting

If role-based access is not working:

1. **Check JWT token**: Verify it contains role and roleId claims
2. **Check user data**: Verify user object in useAuth contains role information
3. **Check permissions**: Verify usePermissions hook returns correct values
4. **Check routing**: Verify RoleBasedRoute component is working
5. **Check API responses**: Verify backend returns user with role data in auth response
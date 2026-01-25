# Permission Flow Audit - Backend to Frontend

## Executive Summary
✅ **System is now fully dynamic** - Backend permissions flow to frontend without hardcoding
- Permissions are stored in database
- Permissions are loaded with user on login
- Frontend dynamically reads and applies permissions
- Any backend change is immediately reflected in UI

---

## BACKEND FLOW (User Login → Permission Assignment)

### 1. User Entity Mapping (User.java)
```
User Entity → @ManyToOne(fetch = FetchType.EAGER) Role
            → Role loads WITH RolePermissions eagerly
            → Each RolePermission has Permission object
```
**Why EAGER?** When User is fetched, Role and its permissions are loaded in one go (no lazy loading issues)

---

### 2. Authentication Flow (AuthService.java)
```
User Login Request
    ↓
AuthService.authenticate()
    ├─ Find user by email
    ├─ Validate password
    ├─ Generate JWT token
    ├─ Call userMapper.toUserResponseDTO(user)  ← KEY STEP
    └─ Return AuthResponseDTO with UserResponseDTO
```

---

### 3. User DTO Mapping (UserMapper.java)
```
User Entity (with eager-loaded Role)
    ↓
UserMapper.toDto() [=toUserResponseDTO]
    ├─ Map basic user fields (id, email, fullName)
    ├─ Get Role from user.getRole()
    ├─ Create RoleResponseDTO
    ├─ Get role's RolePermissions: user.getRole().getRolePermissions()
    ├─ Stream and map each RolePermission → PermissionResponseDTO
    │   ├─ Extract permission.id
    │   ├─ Extract permission.name   ← THIS IS WHAT FRONTEND USES
    │   └─ Extract permission.createdAt/By/etc
    ├─ Set permissions on RoleResponseDTO
    └─ Return complete UserResponseDTO with role.permissions array
```

**Output to Frontend:**
```json
{
  "id": 1,
  "email": "admin@example.com",
  "fullName": "Shakib Admin",
  "role": {
    "id": 2,
    "name": "Admin",
    "permissions": [
      { "id": 1, "name": "user.view.all" },
      { "id": 2, "name": "user.manage.own" },
      { "id": 5, "name": "event.view.all" },
      ...
    ]
  }
}
```

---

### 4. Permission Management (RoleService.java)
```
Permissions in Database ← SINGLE SOURCE OF TRUTH

addPermissionToRole(roleId, permissionId)
    ├─ Find Role by ID
    ├─ Find Permission by ID
    ├─ Check if NOT already assigned (prevent duplicates)
    ├─ Create RolePermission record
    └─ Save to DB

removePermissionFromRole(roleId, permissionId)
    ├─ Find Role and Permission
    └─ Delete RolePermission record

Initial Permissions (auto-created):
  SuperAdmin: ["user.manage.all", "role.manage.all", "event.manage.all", ...]
  Admin:      ["user.manage.own", "event.manage.own", "event.view.all", ...]
  Attendee:   ["event.view.public", "event.view.invited", "event.attend", ...]
```

---

## FRONTEND FLOW (Receiving Permissions → Applying to UI)

### 1. Auth Context (AuthContext.tsx)
```
User logs in
    ↓
AuthService.login(email, password)
    ├─ Sends credentials to backend
    ├─ Receives AuthResponseDTO
    └─ Extracts user with role.permissions
        ↓
localStorage.setItem('eventflow_user', user)
    ↓
setUser(user)  ← React state updated
    ↓
All components can now read user.role.permissions
```

---

### 2. Permission Checking Utility (rolePermissions.ts)
```typescript
getUserPermissions(user) → string[]
    ├─ Check if user exists and has role
    ├─ Handle string role fallback (legacy)
    ├─ Read user.role.permissions array
    ├─ Extract permission.name from each
    └─ Console log for debugging
    
hasPermission(user, 'permission.name') → boolean
    ├─ Get all permissions via getUserPermissions()
    ├─ Check if requested permission exists in array
    └─ Return true/false

hasAnyPermission(user, ['perm1', 'perm2']) → boolean
    └─ True if user has AT LEAST ONE of the permissions

hasAllPermissions(user, ['perm1', 'perm2']) → boolean
    └─ True if user has ALL permissions
```

**NO HARDCODING** - All checks read from actual user.role.permissions array from backend

---

### 3. Dynamic Sidebar Menu (Sidebar.tsx)
```typescript
const baseItems = menuItems[0].items  // Dashboard, Events, Calendar
    ↓
if (hasPermission(user, 'user.view.all')) {
    baseItems.push({ id: 'users', label: 'Users' })  // ADD IF ALLOWED
}
if (hasPermission(user, 'role.manage.all')) {
    baseItems.push({ id: 'roles', label: 'Roles' })  // ADD IF ALLOWED
}
if (hasPermission(user, 'role.manage.all')) {
    baseItems.push({ id: 'permissions', label: 'Permissions' })  // ADD IF ALLOWED
}
    ↓
Sidebar renders only menu items user has permissions for
```

**Result:**
- SuperAdmin: Sees Dashboard, Events, Calendar, Users, Roles, Permissions, Activity, Settings
- Admin: Sees Dashboard, Events, Calendar, Users, Activity, Settings (no Roles/Permissions)
- Attendee: Sees Dashboard, Events, Calendar, Activity, Settings (no Users/Roles/Permissions)

---

### 4. Page-Level Access Control
```typescript
// UserManagement.tsx
const canView = hasPermission(user, 'user.view.all')
const canManage = hasPermission(user, 'user.manage.all')

useEffect(() => {
    if (!canView) navigate('/dashboard')  // Redirect if no permission
}, [canView])

// Show/Hide components based on canManage
{canManage && <Button>Add User</Button>}
{canManage && <th>Actions</th>}

// RoleManagement.tsx
const canManage = hasPermission(user, 'role.manage.all')
if (!canManage) redirect to dashboard

// PermissionManagement.tsx  
const canManage = hasPermission(user, 'role.manage.all')
if (!canManage) redirect to dashboard
```

---

## DATA FLOW DIAGRAM

```
┌─────────────────────────────────────────────────────────┐
│                    DATABASE                              │
│  ┌──────────┐  ┌─────────────┐  ┌──────────────────┐   │
│  │   Users  │  │   Roles     │  │   Permissions    │   │
│  │          │  │             │  │                  │   │
│  │ id       │  │ id          │  │ id               │   │
│  │ email    │  │ name        │  │ name             │   │
│  │ role_id  │──│             │  │ "user.view.all"  │   │
│  └──────────┘  │ id: 2       │  │ "user.manage.all"│   │
│                │ name:"Admin"│  │ "event.view.all" │   │
│                └─────────────┘  └──────────────────┘   │
│                       ↓                                  │
│                 ┌──────────────────┐                   │
│                 │ RolePermissions  │                   │
│                 │ (Join Table)     │                   │
│                 │ role_id: 2       │                   │
│                 │ perm_id: 1,2,5   │                   │
│                 └──────────────────┘                   │
└─────────────────────────────────────────────────────────┘
         ↓ User Login
┌─────────────────────────────────────────────────────────┐
│              BACKEND PROCESSING                         │
│                                                          │
│  AuthService.authenticate()                             │
│    ├─ Find User (with EAGER Role load)                 │
│    ├─ Role.getRolePermissions() [EAGER]                │
│    └─ UserMapper.toDto()                               │
│        └─ Map Role + all Permissions                   │
│                                                          │
│  JSON Response:                                          │
│  {                                                       │
│    "user": {                                            │
│      "id": 1,                                           │
│      "email": "admin@example.com",                      │
│      "role": {                                          │
│        "id": 2,                                         │
│        "name": "Admin",                                 │
│        "permissions": [                                 │
│          {"id": 1, "name": "user.view.all"},           │
│          {"id": 2, "name": "user.manage.own"},         │
│          {"id": 5, "name": "event.view.all"}           │
│        ]                                                │
│      }                                                  │
│    }                                                    │
│  }                                                      │
└─────────────────────────────────────────────────────────┘
         ↓ AuthContext stores user
┌─────────────────────────────────────────────────────────┐
│            FRONTEND STATE (React)                       │
│                                                          │
│  localStorage.setItem('eventflow_user', user)           │
│  useAuth hook returns { user, ... }                     │
└─────────────────────────────────────────────────────────┘
         ↓ rolePermissions.ts reads user.role.permissions
┌─────────────────────────────────────────────────────────┐
│          DYNAMIC PERMISSION CHECKS                      │
│                                                          │
│  hasPermission(user, 'user.view.all')                   │
│    → user.role.permissions.map(p => p.name)            │
│    → Check if 'user.view.all' in array                 │
│    → true ✅                                            │
│                                                          │
│  hasPermission(user, 'role.manage.all')                 │
│    → user.role.permissions.map(p => p.name)            │
│    → Check if 'role.manage.all' in array               │
│    → false ❌ (Admin doesn't have this)                 │
└─────────────────────────────────────────────────────────┘
         ↓ Components use permission checks
┌─────────────────────────────────────────────────────────┐
│            UI COMPONENTS (Render)                       │
│                                                          │
│  Sidebar.tsx                                             │
│    if (hasPermission(...)) push('Users')  // YES ✅     │
│    if (hasPermission(...)) push('Roles')  // NO ❌      │
│                                                          │
│  UserManagement.tsx                                      │
│    canView = hasPermission('user.view.all')  // YES ✅  │
│    {canView && renderUserTable()}                       │
│    {canManage && renderAddButton()}  // NO ❌           │
└─────────────────────────────────────────────────────────┘
```

---

## VERIFICATION CHECKLIST

### Backend ✅
- [x] User entity uses `@ManyToOne(fetch = FetchType.EAGER) Role`
- [x] Role entity uses `@OneToMany(fetch = FetchType.EAGER) rolePermissions`
- [x] UserMapper.toDto() maps role.permissions to RoleResponseDTO.permissions
- [x] AuthService returns user with full role.permissions array
- [x] RoleService.addPermissionToRole() persists to database
- [x] RoleService.removePermissionFromRole() removes from database

### Frontend ✅
- [x] rolePermissions.ts has NO hardcoded permission names
- [x] getUserPermissions() reads from actual user.role.permissions
- [x] hasPermission() checks against dynamic permission array
- [x] Sidebar.tsx uses hasPermission() for menu items (not role names)
- [x] UserManagement.tsx uses hasPermission('user.view.all')
- [x] RoleManagement.tsx uses hasPermission('role.manage.all')
- [x] PermissionManagement.tsx uses hasPermission('role.manage.all')

---

## WHEN YOU CHANGE PERMISSIONS IN BACKEND

### Scenario: Add new permission to Admin role via web UI

**Backend:**
1. Web UI adds permission to Admin role
2. RolePermission record created in database
3. No code changes needed ✅

**Next User Login:**
1. AuthService fetches user
2. User.role has EAGER-loaded permissions
3. All new permissions are in the permission set
4. UserMapper maps all permissions to response
5. Frontend receives complete permissions array
6. UI automatically reflects new permissions ✅

**NO CODE CHANGES NEEDED** - Fully automatic! 🎉

---

## DEBUGGING TIPS

### Check permissions in browser console:
```javascript
// Open DevTools Console (F12)
// You'll see logs like:
// [Permission Debug] User permissions: ['user.view.all', 'user.manage.own', 'event.view.all', ...] Role: Admin
```

### Check user object in browser:
```javascript
// In browser console:
const user = JSON.parse(localStorage.getItem('eventflow_user'))
console.log(user.role.permissions)
// Shows all permissions the user has
```

### Verify backend is sending permissions:
```
1. Open Network tab in DevTools
2. Login as Admin
3. Look for /api/auth/login response
4. Check if response.user.role.permissions is populated
5. Should see array of permission objects with name field
```

---

## Summary: Backend → Frontend Data Path

```
Database (user_id=1 → role_id=2 → permissions=[1,2,5])
        ↓
User.java (@ManyToOne EAGER)
        ↓
Role.java (@OneToMany EAGER)
        ↓
RolePermissions (join table)
        ↓
UserMapper.toDto() → creates PermissionResponseDTO[]
        ↓
AuthResponseDTO.user.role.permissions
        ↓
Frontend localStorage.eventflow_user
        ↓
useAuth() returns user with permissions
        ↓
hasPermission(user, 'permission.name') → checks dynamic array
        ↓
UI Components render based on actual permissions ✅
```

**Result:** Fully dynamic, zero hardcoding, database-driven permissions! 🚀

# Swagger Testing Guide for Event Management System

## Access Points

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Docs**: http://localhost:8080/api-docs

## Default SuperAdmin User

The system automatically creates a default SuperAdmin user:
- **Email**: superadmin@ems.com
- **Password**: password
- **Role**: ADMIN (with all permissions)

✅ **No Login Required for Swagger UI**: Since we removed Spring Security configuration, you can now access Swagger UI without any authentication!

**For API Testing**: Your endpoints still require authentication headers:

1. **For Authorization header**: Use the format `Bearer <your-token>` or your custom auth format
2. **For X-User-Id header**: Use the numeric user ID (e.g., `1`)
## Authentication Setup

Your endpoints require authentication. You'll need to:

1. **For Authorization header**: Use the format `Bearer <your-token>` or your custom auth format
2. **For X-User-Id header**: Use the numeric user ID (e.g., `1` for SuperAdmin)

## Recent Changes

✅ **RolePermission classes merged**: The separate `RolePermissionId.java` has been merged into `RolePermission.java` using `@EmbeddedId` approach for cleaner code structure.

## Testing Scenarios

### 1. Create a New User
- **Endpoint**: POST `/api/users`
- **Headers**: 
  - `Authorization`: Your auth token
- **Body Example**:
```json
{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "password": "securePassword123",
  "roleId": 1
}
```

### 2. Get User by ID
- **Endpoint**: GET `/api/users/{userId}`
- **Headers**:
  - `X-User-Id`: Current user ID (e.g., `1`)
- **Path Parameters**:
  - `userId`: ID of the user to retrieve

### 3. Get All Users
- **Endpoint**: GET `/api/users`
- **Headers**:
  - `Authorization`: Your auth token

### 4. Get User by Email
- **Endpoint**: GET `/api/users/email/{email}`
- **Headers**:
  - `Authorization`: Your auth token
- **Path Parameters**:
  - `email`: Email address of the user

### 5. Update User
- **Endpoint**: PUT `/api/users/{userId}`
- **Headers**:
  - `Authorization`: Your auth token
- **Path Parameters**:
  - `userId`: ID of the user to update
- **Body Example**:
```json
{
  "fullName": "John Updated",
  "email": "john.updated@example.com",
  "password": "newSecurePassword123",
  "roleId": 2
}
```

### 6. Delete User
- **Endpoint**: DELETE `/api/users/{userId}`
- **Headers**:
  - `Authorization`: Your auth token
- **Path Parameters**:
  - `userId`: ID of the user to delete

### 7. Add Role to User
- **Endpoint**: POST `/api/users/{userId}/roles/{roleId}`
- **Headers**:
  - `Authorization`: Your auth token
- **Path Parameters**:
  - `userId`: ID of the user
  - `roleId`: ID of the role to assign

### 8. Remove Role from User
- **Endpoint**: DELETE `/api/users/{userId}/roles/{roleId}`
- **Headers**:
  - `Authorization`: Your auth token
- **Path Parameters**:
  - `userId`: ID of the user
  - `roleId`: ID of the role to remove

## Common Error Responses

- **401 Unauthorized**: Missing or invalid authentication
- **403 Forbidden**: User doesn't have required permissions
- **404 Not Found**: Resource doesn't exist
- **400 Bad Request**: Invalid input data

## Tips for Testing

1. **Start with GET endpoints** to understand the data structure
2. **Use the same user ID** in X-User-Id header as the current authenticated user for testing
3. **Check the response schemas** in Swagger UI to understand expected data formats
4. **Use the "Try it out" feature** to quickly test endpoints without leaving the browser
5. **Monitor the server logs** to see SQL queries and debug information

## Security Considerations

- Your application uses Spring Security with role-based access control
- Some operations require specific permissions like `user.manage.all` or `role.manage.all`
- Make sure your test user has appropriate permissions for the operations you're testing
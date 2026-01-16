# Event Management System

A comprehensive Spring Boot-based REST API for managing events with enterprise-grade security features, role-based access control, and JWT authentication.

## 🎯 Table of Contents

- [Features](#features)
- [Security Architecture](#security-architecture)
- [Technologies Used](#technologies-used)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Authentication & Authorization](#authentication--authorization)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Security Documentation](#security-documentation)
- [Contributing](#contributing)

## ✨ Features

### Core Features
- ✅ CRUD operations for event management
- ✅ RESTful API design with DTO pattern
- ✅ Global exception handling
- ✅ Swagger/OpenAPI 3 documentation
- ✅ JPA/Hibernate data persistence
- ✅ MySQL database integration
- ✅ Maven build system
- ✅ Unit & Integration testing

### Security Features
- 🔐 **JWT Authentication** (HS512 signed tokens)
- 🔑 **BCrypt Password Hashing** (strength 12)
- 👥 **Role-Based Access Control (RBAC)**
- ✅ **Server-Side Token Caching** (enables logout)
- 🛡️ **Token Expiration** (45-minute access tokens, 7-day refresh tokens)
- 🔒 **Permission-Based Authorization** (@PreAuthorize support)
- 📋 **Audit Trail** (created_at, updated_at timestamps)

### Logging & Monitoring Features
- 📝 **5-Level Logging** (TRACE, DEBUG, INFO, WARN, ERROR)
- 📊 **Daily Log Rotation** (separate files for controllers/services/errors)
- 🗂️ **Automatic Log Archiving** (30-day retention with compression)
- 📈 **Service Layer Logging** (comprehensive tracking of all business operations)
- 🎯 **Activity Audit Trail** (user login/logout, event creation/modification)
- 🔍 **Request/Response Logging** (API endpoint tracking)

### Scheduler Features
- ⏰ **Event Reminder Job** - Sends email reminders to attendees before events start (5-minute intervals)
- 🔄 **Event Status Update Job** - Automatically marks events as COMPLETED after they end (10-minute intervals)
- 🗄️ **Audit Log Archival Job** - Archives old audit logs (90-day retention, runs daily)
- 📧 **Email Service** - SMTP integration for event notifications and reminders
- 🎯 **Smart Scheduling** - Configurable cron expressions for all scheduled tasks

## Technologies Used

### Backend Framework
- **Java 17+** - Latest LTS version
- **Spring Boot 3.x** - REST API framework
- **Spring Web** - Web layer
- **Spring Data JPA** - ORM/Persistence
- **Spring Security 6.2.8** - Authentication & Authorization
- **Spring Scheduler** - Task scheduling and automation
- **Spring Mail** - Email notifications

### Database & Persistence
- **MySQL 8.0+** - Relational database
- **Hibernate** - ORM framework
- **Flyway/Liquibase** - Database migrations

### Security
- **JJWT 0.12.3** - JWT token library
- **BCrypt** - Password hashing
- **Spring Security** - Authorization framework

### API & Documentation
- **Swagger/OpenAPI 3** - API documentation
- **Spring Boot Actuator** - Health checks

### Testing
- **JUnit 5** - Unit testing
- **Mockito** - Mocking framework
- **Spring Boot Test** - Integration testing

### Build & DevOps
- **Maven** - Build automation
- **Git** - Version control
- **Docker** - Containerization (optional)

## Project Structure

```
src/main/java/com/event_management_system/
├── config/                 # Configuration classes
│   ├── SwaggerConfig.java  # Swagger documentation
│   └── SecurityConfig.java # Spring Security configuration
├── controller/             # REST controllers
│   ├── AuthController.java # Authentication endpoints
│   ├── UserController.java # User management
│   ├── RoleController.java # Role management
│   ├── PermissionController.java # Permission management
│   ├── EventController.java # Event management
│   └── HistoryController.java # History/Audit trail
├── dto/                    # Data Transfer Objects
│   ├── LoginRequestDTO.java
│   ├── AuthResponseDTO.java
│   ├── UserRequestDTO.java
│   ├── UserResponseDTO.java
│   ├── RoleDTO.java
│   ├── PermissionDTO.java
│   ├── EventRequestDTO.java
│   ├── EventResponseDTO.java
│   ├── UserActivityHistoryResponseDTO.java
│   ├── UserLoginLogoutHistoryResponseDTO.java
│   └── UserPasswordHistoryResponseDTO.java
├── entity/                 # JPA entities
│   ├── BaseEntity.java     # Abstract base with timestamps
│   ├── User.java
│   ├── Role.java
│   ├── Permission.java
│   ├── RolePermission.java
│   ├── Event.java
│   ├── UserActivityHistory.java
│   ├── UserLoginLogoutHistory.java
│   └── UserPasswordHistory.java
├── exception/              # Exception handling
│   ├── GlobalExceptionHandler.java
│   └── Custom exceptions
├── mapper/                 # Object mapping utilities
│   ├── EventMapper.java
│   ├── UserMapper.java
│   ├── RoleMapper.java
│   └── PermissionMapper.java
├── repository/             # JPA repositories
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── PermissionRepository.java
│   ├── EventRepository.java
│   ├── UserActivityHistoryRepository.java
│   ├── UserLoginLogoutHistoryRepository.java
│   └── UserPasswordHistoryRepository.java
├── security/               # Security components
│   ├── JwtService.java     # JWT generation & validation
│   ├── JwtAuthenticationFilter.java # Authentication filter
│   ├── TokenCacheService.java # Server-side token cache
│   └── CustomUserDetailsService.java
├── scheduler/              # Scheduled tasks
│   ├── config/
│   │   └── SchedulerConfig.java # Scheduler configuration
│   ├── job/
│   │   ├── EventReminderJob.java # Email reminder job
│   │   ├── EventStatusUpdateJob.java # Auto-complete events
│   │   └── AuditLogArchivalJob.java # Log cleanup job
│   └── service/
│       └── EventReminderSentService.java # Reminder tracking
├── service/                # Business logic
│   ├── ApplicationLoggerService.java # Centralized logging
│   ├── AuthService.java    # Authentication service
│   ├── UserService.java    # User management
│   ├── RoleService.java    # Role management
│   ├── EventService.java   # Event management
│   ├── EmailService.java   # Email notifications
│   ├── PermissionService.java # Permission management
│   ├── UserActivityHistoryService.java # Activity logging
│   ├── UserLoginLogoutHistoryService.java # Login/Logout tracking
│   └── UserPasswordHistoryService.java # Password change tracking
└── EventManagementSystemApplication.java

src/main/resources/
├── application.properties  # Application configuration
├── logback-spring.xml     # Logging configuration (daily rotation)
└── db/
    └── migration/         # Database migration scripts (if using Flyway)

src/test/java/com/event_management_system/
└── PasswordTest.java      # Unit tests

logs/
├── application.log        # TODAY's all logs (ACTIVE)
├── event-controller.log   # TODAY's controller logs (ACTIVE)
├── service.log           # TODAY's service logs (ACTIVE)
├── error.log             # TODAY's error logs (ACTIVE)
└── archive/              # Historical logs (compressed)
    ├── application-2025-12-20.1.log.gz
    ├── event-controller-2025-12-20.1.log.gz
    ├── service-2025-12-20.1.log.gz
    └── error-2025-12-20.1.log.gz
```
│   └── Event.Visibility    # Event visibility enum (inner enum)
├── exception/              # Exception handling
│   ├── GlobalExceptionHandler.java
│   └── Custom exceptions
├── mapper/                 # Object mapping utilities
│   ├── EventMapper.java
│   ├── UserMapper.java
│   └── RoleMapper.java
├── repository/             # JPA repositories
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── PermissionRepository.java
│   └── EventRepository.java
├── security/               # Security components
│   ├── JwtService.java     # JWT generation & validation
│   ├── JwtAuthenticationFilter.java # Authentication filter
│   ├── TokenCacheService.java # Server-side token cache
│   └── CustomUserDetailsService.java
├── service/                # Business logic
│   ├── AuthService.java    # Authentication service
│   ├── UserService.java    # User management
│   ├── RoleService.java    # Role management
│   ├── EventService.java   # Event management
│   └── PermissionService.java
└── EventManagementSystemApplication.java
```

## Security Architecture

### Authentication Flow

```
1. User Login
   POST /api/auth/login → { email, password }
   ↓
2. Credentials Verification
   - Hash password with BCrypt
   - Compare with stored hash
   ↓
3. Token Generation
   - Create JWT signed with HS512
   - Include userId + unique tokenUuid
   - Set expiration (45 minutes)
   ↓
4. Token Caching
   - Cache tokenUuid in server memory
   - Map to userId and TTL
   ↓
5. Response to Client
   ← accessToken + refreshToken + user info
```

### Authorization Flow

```
1. Protected Request
   GET /api/users
   Headers: Authorization: Bearer eyJhbGc...
   ↓
2. Token Extraction & Validation
   - Extract JWT from header
   - Verify signature hasn't changed
   - Check if token expired
   ↓
3. Token Cache Verification
   - Lookup tokenUuid in cache
   - Verify user still logged in (not cached = logged out)
   ↓
4. User Loading
   - Load user from database
   - Fetch roles and permissions
   ↓
5. Authorization Check
   - @PreAuthorize checks role/permission
   - Grant or deny access
   ↓
6. Response
   ← 200 OK (if authorized) or 403 Forbidden
```

### Security Components

| Component | Purpose | Details |
|-----------|---------|---------|
| **JwtService** | Token generation & validation | Signs with HS512, extracts claims |
| **JwtAuthenticationFilter** | Request interception | Runs on every request, validates token |
| **TokenCacheService** | Server-side logout | Tracks valid tokens in memory |
| **BCryptPasswordEncoder** | Password hashing | One-way hash with salt (strength 12) |
| **CustomUserDetailsService** | User loading | Fetches user + authorities from DB |
| **SecurityConfig** | Spring Security setup | Configures filter chain, CORS, etc. |



## Database Setup & Maintenance

### Initial Database Setup

1. **Create the MySQL database:**
   ```sql
   CREATE DATABASE event_management_db;
   ```

2. **Run the application** - Hibernate will auto-create tables based on `spring.jpa.hibernate.ddl-auto=update` in `application.properties`

### Database Maintenance Scripts

If you encounter database issues (corrupted columns, old schema remnants), run the maintenance script:

```sql
-- File: fix-role-status.sql
-- This script fixes common database issues:
-- - Removes extra columns from old schema
-- - Drops unused tables
-- - Verifies table structures

-- Run in MySQL Workbench:
USE event_management_db;
-- Then execute the entire fix-role-status.sql script
```

**Common Database Issues & Fixes:**

| Issue | Solution |
|-------|----------|
| Extra `user_type` column in `app_roles` | Run `fix-role-status.sql` |
| Empty `user_role` table | Run `fix-role-status.sql` to drop it |
| Corrupted status values | Script will clean up invalid status values |

### Verify Database Structure

```sql
-- Check roles table
SHOW COLUMNS FROM app_roles;

-- Check users and their roles
SELECT u.id, u.full_name, u.email, r.name as role_name 
FROM app_users u 
LEFT JOIN app_roles r ON u.role_id = r.id;

-- Check role permissions
SELECT r.name as role_name, p.name as permission_name
FROM app_roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN app_permissions p ON rp.permission_id = p.id
ORDER BY r.name, p.name;
```

## Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.6** or higher
- **MySQL 8.0** or higher
- **Git** (for version control)

### Installation

1. **Clone the repository:**
```bash
git clone https://github.com/shakibbs/Event-Management-System.git
cd event_management_system
```

2. **Setup MySQL Database:**
```sql
CREATE DATABASE event_management_db;
USE event_management_db;
```

3. **Configure database connection** in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/event_management_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

4. **Update JWT Secret** (optional - for production):
```properties
app.jwt.secret=your-super-secret-key-minimum-32-characters
app.jwt.access-token-expiration=2700000  # 45 minutes in milliseconds
app.jwt.refresh-token-expiration=604800000  # 7 days in milliseconds
```

5. **Build the project:**
```bash
mvn clean install
```

6. **Run the application:**
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Accessing API Documentation

Once the application is running, you can access the Swagger UI at:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

## Authentication & Authorization

### How JWT Authentication Works

1. **User logs in** with email and password
2. **Server validates credentials** using BCrypt
3. **JWT token is generated** with:
   - User ID in `sub` claim
   - Unique token UUID (for logout tracking)
   - Expiration time (45 minutes for access token)
4. **Token is cached** server-side for logout support
5. **Client stores token** and includes in every request
6. **Filter validates token** on each request:
   - Verifies signature hasn't been tampered
   - Checks if token is expired
   - Verifies token UUID is still in cache (not logged out)
7. **User authorities loaded** (roles + permissions)
8. **Request authorized** or denied based on @PreAuthorize rules

### Token Structure

```
eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwidG9rZW5VdWlkIjoiMzc4ZDhhMzAtYzc5Mi00ZjJhLWIyOTAtYTlmZWJiMGI5MWI1IiwiaWF0IjoxNjk2NTQzOTkwLCJleHAiOjE2OTY1NDY2OTB9.signature...
```

- **Header:** Algorithm (HS512)
- **Payload:** User ID, Token UUID, Issued At, Expiration
- **Signature:** HMACSHA512(header.payload, secret)

### Using Authentication in Requests

```bash
# Login to get token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ems.com","password":"password"}'

# Response includes accessToken
# Use token in subsequent requests:
curl -X GET http://localhost:8080/api/events \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

### Role-Based Access Control

Roles determine what actions a user can perform:

- **ADMIN** - Full system access (users, roles, events)
- **ORGANIZER** - Can create and manage events
- **ATTENDEE** - Can view events

Example:
```java
@GetMapping("/users")
@PreAuthorize("hasRole('ADMIN')")  // Only ADMIN role
public ResponseEntity<List<UserResponseDTO>> getAllUsers() { ... }

@PostMapping("/events")
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")  // ADMIN or ORGANIZER
public ResponseEntity<EventResponseDTO> createEvent(...) { ... }
```

## API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|----------------|
| POST | `/api/auth/login` | User login | ❌ No |
| POST | `/api/auth/refresh` | Refresh access token | ❌ No |
| POST | `/api/auth/logout` | Logout user (invalidate token) | ✅ Yes |

### User Management

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|----------------|
| GET | `/api/users` | Get all users | ADMIN |
| GET | `/api/users/{id}` | Get user by ID | ADMIN |
| POST | `/api/users` | Create new user | ADMIN |
| PUT | `/api/users/{id}` | Update user | ADMIN |
| DELETE | `/api/users/{id}` | Delete user | ADMIN |

### Role Management

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|----------------|
| GET | `/api/roles` | Get all roles | ADMIN |
| POST | `/api/roles` | Create role | ADMIN |
| PUT | `/api/roles/{id}` | Update role | ADMIN |
| DELETE | `/api/roles/{id}` | Delete role | ADMIN |

### Event Management

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|----------------|
| GET | `/api/events` | Get all events | ATTENDEE |
| GET | `/api/events/{id}` | Get event by ID | ATTENDEE |
| POST | `/api/events` | Create new event | ORGANIZER/ADMIN |
| PUT | `/api/events/{id}` | Update event | ORGANIZER/ADMIN |
| DELETE | `/api/events/{id}` | Delete event | ADMIN |

### Request/Response Examples

#### Login Request
```json
POST /api/auth/login
{
  "email": "admin@ems.com",
  "password": "password"
}
```

#### Login Response
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwi...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwi...",
  "tokenType": "Bearer",
  "expiresIn": 2700,
  "user": {
    "id": 1,
    "email": "admin@ems.com",
    "firstName": "Admin",
    "lastName": "User",
    "role": {
      "id": 1,
      "name": "ADMIN"
    }
  }
}
```

#### Create Event Request
```json
POST /api/events
Authorization: Bearer <accessToken>
{
  "title": "Tech Conference 2024",
  "description": "Annual technology conference",
  "startDate": "2024-12-31T09:00:00",
  "endDate": "2024-12-31T17:00:00",
  "location": "Convention Center",
  "capacity": 500
}
```

#### Event Response
```json
{
  "id": 1,
  "title": "Tech Conference 2024",
  "description": "Annual technology conference",
  "startDate": "2024-12-31T09:00:00",
  "endDate": "2024-12-31T17:00:00",
  "location": "Convention Center",
  "capacity": 500,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

## Testing

### Running Unit Tests

```bash
mvn test
```

### Running Integration Tests

```bash
mvn verify
```

### API Testing with PowerShell Scripts

The project includes PowerShell scripts for comprehensive API testing:

```bash
# Test basic API endpoints
.\test_api.ps1

# Test error scenarios
.\test_error_scenarios.ps1

# Test RBAC (Role-Based Access Control)
.\rbac_access_test.ps1

# Comprehensive API testing
.\comprehensive_api_test.ps1
```

### Test Coverage

Test files are located in:
```
src/test/java/com/event_management_system/
├── EventManagementSystemApplicationTests.java
└── RBACTest.java
```

### Manual Testing with Swagger

1. Start the application
2. Navigate to http://localhost:8080/swagger-ui.html
3. Click "Authorize" and enter your JWT token
4. Test endpoints directly from Swagger UI

## Security Documentation

For detailed security architecture, JWT implementation, and token verification flow, see: **[SECURITY_REPORT.md](./SECURITY_REPORT.md)**

### Key Security Features

✅ **JWT Authentication** - Stateless, signed tokens (HS512)  
✅ **Server-Side Logout** - Token cache prevents reuse after logout  
✅ **BCrypt Password Hashing** - Strength 12, salted hashes  
✅ **Role-Based Access Control** - Fine-grained authorization  
✅ **Token Expiration** - Access (45 min) and Refresh (7 days)  
✅ **Signature Verification** - Prevents token tampering  
✅ **Audit Trail** - Created/Updated timestamps on all entities  

### Security Best Practices

- 🔒 Always use HTTPS in production
- 🔑 Rotate JWT secret regularly
- 🛡️ Implement rate limiting on login endpoint
- 📝 Monitor authentication logs for suspicious activity
- 🔄 Use refresh tokens for token rotation
- 🚫 Never store secrets in code (use environment variables)

## Configuration

### Database Configuration

MySQL database settings in `src/main/resources/application.properties`:

```properties
# MySQL Database
spring.datasource.url=jdbc:mysql://localhost:3306/event_management_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

Note: Update the password field with your actual MySQL password.

### JWT Configuration

```properties
# JWT Settings
app.jwt.secret=your-super-secret-key-minimum-32-characters-for-hs512
app.jwt.access-token-expiration=2700000  # 45 minutes
app.jwt.refresh-token-expiration=604800000  # 7 days
```

### Logging Configuration

The application uses **Logback** for comprehensive logging with daily rotation:

```properties
# Log Levels
logging.level.root=WARN
logging.level.com.event_management_system=DEBUG
logging.level.org.springframework.security=DEBUG

# Log Files (Daily Rotation)
logs/application.log          # All logs
logs/event-controller.log     # Controller logs only
logs/service.log              # Service logs only
logs/error.log                # Error logs only
logs/archive/                 # Historical logs (30-day retention)

# Rolling Policy
Max file size: 10MB
Max history: 30 days
Total size cap: 1GB (prevents disk overflow)
```

**View Logs:**
```bash
# View today's logs (live)
Get-Content logs/application.log -Wait

# View errors only
Get-Content logs/error.log

# View last 50 service logs
Get-Content logs/service.log -Tail 50
```

## Deployment

### Docker Deployment

1. **Build Docker image:**
```bash
docker build -t event-management-system:latest .
```

2. **Run container:**
```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/event_management_db \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  -e APP_JWT_SECRET=your-secret-key \
  event-management-system:latest
```

### Production Deployment Checklist

- [ ] Use HTTPS (SSL/TLS certificate)
- [ ] Rotate JWT secret
- [ ] Set strong MySQL password
- [ ] Enable Spring Security CSRF protection
- [ ] Configure CORS for trusted domains only
- [ ] Implement rate limiting
- [ ] Enable request logging and monitoring
- [ ] Use environment variables for secrets
- [ ] Set appropriate cache TTL values
- [ ] Configure database backups
- [ ] Enable health checks (`/actuator/health`)
- [ ] Monitor error logs

## Contributing

We welcome contributions! Please follow these steps:

1. **Fork** the repository
2. **Create feature branch:**
```bash
git checkout -b feature/AmazingFeature
```
3. **Make your changes** and commit:
```bash
git commit -m 'Add some AmazingFeature'
```
4. **Push to branch:**
```bash
git push origin feature/AmazingFeature
```
5. **Open a Pull Request**

### Code Standards

- Follow Java naming conventions
- Write unit tests for new features
- Keep methods focused (single responsibility)
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Update README for new features

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact & Support

- **GitHub Issues:** https://github.com/shakibbs/Event-Management-System/issues
- **Email:** shakib@example.com
- **Documentation:** See [SECURITY_REPORT.md](./SECURITY_REPORT.md) for detailed security information

## Changelog

### v1.3.0 (Current - December 2025)
- ✅ Added comprehensive 5-level logging (TRACE, DEBUG, INFO, WARN, ERROR)
- ✅ Implemented daily log rotation with automatic archiving
- ✅ Created separate log files (application, controller, service, error)
- ✅ Added 30-day log retention with auto-cleanup
- ✅ Injected ApplicationLoggerService in all 5 services
- ✅ Complete audit trail for all business operations
- ✅ User activity history tracking (login/logout, password changes)
- ✅ Refactored service layer for better separation of concerns
- ✅ Removed redundant methods from entity layer

### v1.2.0 (December 2025)
- ✅ Added JWT Authentication with HS512 signing
- ✅ Implemented Role-Based Access Control (RBAC)
- ✅ Added server-side token caching for logout support
- ✅ Comprehensive security report
- ✅ Enhanced API endpoints with authentication
- ✅ Activity audit trail implementation

### v1.1.0
- Event CRUD operations
- Swagger API documentation
- Global exception handling
- MySQL persistence

### v1.0.0
- Initial project setup
- Basic Spring Boot configuration
# 🔐 COMPREHENSIVE JWT SECURITY & TOKEN VERIFICATION REPORT

**Last Updated:** December 17, 2025  
**Project:** Event Management System  
**Version:** 1.3.0  
**Security Framework:** Spring Security 6.2.8 + JWT (JJWT 0.12.3)  
**Algorithm:** HS512 (HMAC SHA-512)  

---

## 📋 TABLE OF CONTENTS

1. [Security Architecture Overview](#security-architecture-overview)
2. [JWT Token Structure](#jwt-token-structure)
3. [Login Flow (Token Generation)](#login-flow-token-generation)
4. [Token Verification Flow](#token-verification-flow)
5. [Security Components](#security-components)
6. [Activity Audit Trail](#activity-audit-trail)
7. [Error Handling & Security](#error-handling--security)
8. [Token Lifecycle](#token-lifecycle)
9. [Deployment Security Checklist](#deployment-security-checklist)

---

## 🏗️ SECURITY ARCHITECTURE OVERVIEW

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          CLIENT                                  │
│                    (Mobile App / Web)                             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ 1. POST /api/auth/login
                             │    (email + password)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SPRING SECURITY LAYER                       │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ JwtAuthenticationFilter (OncePerRequestFilter)          │    │
│  │ - Runs on EVERY request                                 │    │
│  │ - Extracts JWT from Authorization header               │    │
│  │ - Validates token signature & expiration               │    │
│  │ - Loads user from database                             │    │
│  │ - Stores in SecurityContext                            │    │
│  └─────────────────────────────────────────────────────────┘    │
│                             ▲                                     │
│                             │                                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ AuthController                                          │    │
│  │ - POST /api/auth/login → authenticate user             │    │
│  │ - POST /api/auth/refresh → refresh access token        │    │
│  │ - POST /api/auth/logout → invalidate tokens            │    │
│  └─────────────────────────────────────────────────────────┘    │
│                             ▲                                     │
│                             │                                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ JwtService                                              │    │
│  │ - Generate tokens (HS512 signed)                        │    │
│  │ - Validate token signature                             │    │
│  │ - Extract claims (userId, tokenUuid)                   │    │
│  │ - Check expiration                                      │    │
│  └─────────────────────────────────────────────────────────┘    │
│                             ▲                                     │
│                             │                                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ TokenCacheService                                       │    │
│  │ - Cache tokens by UUID (ConcurrentHashMap)             │    │
│  │ - Track token expiration                               │    │
│  │ - Implement server-side logout                         │    │
│  └─────────────────────────────────────────────────────────┘    │
│                             ▲                                     │
└─────────────────────────────┼─────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DATABASE LAYER                              │
│                                                                   │
│  ┌──────────────────────────────────────────────────────┐       │
│  │ MySQL 8.0.33                                        │       │
│  │ - app_users (id, email, password_hash, role_id)    │       │
│  │ - app_roles (id, name)                             │       │
│  │ - app_permissions (id, name)                       │       │
│  │ - app_role_permissions (role_id, permission_id)    │       │
│  └──────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

### Key Security Layers

| Layer | Purpose | Technology |
|-------|---------|-----------|
| **Authentication** | Verify user identity | Email + BCrypt Password |
| **Token Generation** | Create signed tokens | JWT (HS512) + UUID |
| **Token Storage** | Server-side tracking | ConcurrentHashMap Cache |
| **Token Validation** | Verify token authenticity | Signature + Expiration |
| **Authorization** | Grant access based on roles | Spring Security + @PreAuthorize |

---

## 🎟️ JWT TOKEN STRUCTURE

### What is JWT?

A **JWT (JSON Web Token)** is a compact, self-contained token that proves a user's identity without storing session state on the server.

### Token Format

```
eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI0NyIsInRva2VuVXVpZCI6IjNjNTU0MmI0LWJkYWUtNGJhZi05NzIyLWM4ZjkwYWVhYzJmYyIsImlhdCI6MTczMzI0MjU5MCwiZXhwIjoxNzMzMjQ1MjkwfQ.signature...
```

**Structure:** `Header.Payload.Signature`

### Part 1: Header

```json
{
  "alg": "HS512",     // Algorithm: HMAC SHA-512
  "typ": "JWT"        // Type: JSON Web Token
}
```

**Base64 Encoded:** `eyJhbGciOiJIUzUxMiJ9`

### Part 2: Payload (Claims)

```json
{
  "sub": "47",                                           // Subject (User ID)
  "tokenUuid": "3c5542b4-bdae-4baf-9722-c8f90aeac2fc", // Token UUID for logout tracking
  "iat": 1733242590,                                     // Issued At (Unix timestamp)
  "exp": 1733245290                                      // Expiration (Unix timestamp)
}
```

**Breakdown:**
- `sub` (subject) = User ID (e.g., 47)
- `tokenUuid` = Unique identifier for this token instance (enables logout)
- `iat` = Issued at timestamp (when token was created)
- `exp` = Expiration timestamp (when token becomes invalid)

**Duration:** `exp - iat` = 2700 seconds = **45 minutes**

**Base64 Encoded:** `eyJzdWIiOiI0NyIsInRva2VuVXVpZCI6IjNjNTU0MmI0LWJkYWUtNGJhZi05NzIyLWM4ZjkwYWVhYzJmYyIsImlhdCI6MTczMzI0MjU5MCwiZXhwIjoxNzMzMjQ1MjkwfQ`

### Part 3: Signature

```
HMACSHA512(
  Base64Encode(Header) + "." + Base64Encode(Payload),
  Secret_Key
)
```

**Verification:**
- Server recalculates signature using same secret key
- If calculated signature ≠ token signature → **Token tampered or forged**
- Only server with secret key can create valid signatures

---

## 🔑 LOGIN FLOW (TOKEN GENERATION)

### Step-by-Step Process

```
CLIENT                    CONTROLLER                SERVICE                DATABASE
  │                           │                        │                       │
  │ 1. POST /api/auth/login   │                        │                       │
  │ {email, password}         │                        │                       │
  ├──────────────────────────>│                        │                       │
  │                           │ 2. authenticate()      │                       │
  │                           ├───────────────────────>│                       │
  │                           │                        │ 3. findByEmail()      │
  │                           │                        ├──────────────────────>│
  │                           │                        │ ◄──────────────────┤ User{
  │                           │                        │    (User object)   │  id: 47,
  │                           │                        │                    │  email: "user@ems.com",
  │                           │                        │                    │  password: "$2a$12$...",
  │                           │                        │                    │  role: Admin
  │                           │                        │                    │}
  │                           │ 4. matches(password)   │                       │
  │                           │ (BCrypt compare)       │                       │
  │                           │ ✓ Password correct     │                       │
  │                           │                        │                       │
  │                           │ 5. generateAccessToken()                       │
  │                           │ - Create UUID          │                       │
  │                           │ - Build payload        │                       │
  │                           │ - Sign with secret     │                       │
  │                           │ - Return JWT           │                       │
  │                           │                        │                       │
  │                           │ 6. cacheAccessToken()  │                       │
  │                           │ UUID → (userId, TTL)   │                       │
  │                           │ (45 min expiry)        │                       │
  │                           │                        │                       │
  │                           │ 7. AuthResponseDTO     │                       │
  │ 200 OK + JWT + User Info  │ {                      │                       │
  │◄──────────────────────────┤   accessToken: "...",  │                       │
  │                           │   tokenType: "Bearer", │                       │
  │ Store JWT in              │   expiresIn: 2700,     │                       │
  │ Authorization header      │   user: {...}          │                       │
  │                           │ }                      │                       │
```

### Code Flow

#### 1. **User Submits Credentials**
```java
// CLIENT REQUEST
POST /api/auth/login
{
  "email": "user@ems.com",
  "password": "password"
}
```

#### 2. **AuthController Receives Request**
```java
@PostMapping("/login")
public ResponseEntity<AuthResponseDTO> login(
    @Valid @RequestBody LoginRequestDTO loginRequest) {
    AuthResponseDTO response = authService.authenticate(loginRequest);
    return ResponseEntity.ok(response);
}
```

#### 3. **AuthService Authenticates User**
```java
public AuthResponseDTO authenticate(LoginRequestDTO loginRequest) {
    // Step 1: Find user by email
    User user = userRepository.findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
    
    // Step 2: Verify password using BCrypt
    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
        throw new RuntimeException("Invalid credentials");
    }
    
    // Step 3: Generate tokens
    String accessToken = jwtService.generateAccessToken(user.getId());
    String refreshToken = jwtService.generateRefreshToken(user.getId());
    
    // Step 4: Cache tokens for logout tracking
    String accessTokenUuid = jwtService.getTokenUuidFromToken(accessToken);
    String refreshTokenUuid = jwtService.getTokenUuidFromToken(refreshToken);
    tokenCacheService.cacheAccessToken(accessTokenUuid, user.getId(), 2700000); // 45 min
    tokenCacheService.cacheRefreshToken(refreshTokenUuid, user.getId(), 604800000); // 7 days
    
    // Step 5: Return response
    return AuthResponseDTO.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(2700)  // 45 minutes in seconds
            .user(userMapper.toDto(user))
            .build();
}
```

#### 4. **JwtService Generates Token**
```java
public String generateAccessToken(Long userId) {
    // Create unique UUID for this token
    String tokenUuid = UUID.randomUUID().toString();
    
    // Calculate expiration
    long now = System.currentTimeMillis();
    long expirationTime = now + jwtAccessTokenExpiration; // 45 min
    
    // Build JWT
    return Jwts.builder()
            .subject(userId.toString())  // sub: userId
            .claim("tokenUuid", tokenUuid)  // tokenUuid
            .issuedAt(new Date(now))  // iat
            .expiration(new Date(expirationTime))  // exp
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS512)
            .compact();  // Return JWT string
}
```

#### 5. **Token Caching**
```java
public void cacheAccessToken(String tokenUuid, Long userId, long ttlMillis) {
    // Store: tokenUuid → (userId, expirationTime)
    // TTL = Time To Live (45 minutes)
    
    cache.put(tokenUuid, new TokenCacheEntry(userId, System.currentTimeMillis() + ttlMillis));
    
    // If token is accessed after TTL → returns null (treated as logged out)
}
```

#### 6. **Client Receives Tokens**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI0NyIsInRva2VuVXVpZCI6IjNjNTU0MmI0LWJkYWUtNGJhZi05NzIyLWM4ZjkwYWVhYzJmYyIsImlhdCI6MTczMzI0MjU5MCwiZXhwIjoxNzMzMjQ1MjkwfQ.signature...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 2700,
  "user": {
    "id": 47,
    "email": "user@ems.com",
    "firstName": "John",
    "role": "ADMIN"
  }
}
```

---

## ✅ TOKEN VERIFICATION FLOW

### On Every Protected Request

```
CLIENT                    FILTER                      SERVICE              CACHE/DATABASE
  │                        │                             │                    │
  │ GET /api/users         │                             │                    │
  │ Headers: {             │                             │                    │
  │   Authorization:       │                             │                    │
  │   "Bearer eyJhbGc..."  │                             │                    │
  │ }                      │                             │                    │
  ├───────────────────────>│                             │                    │
  │                        │ 1. extractToken()           │                    │
  │                        │ Get header value            │                    │
  │                        │ Split by space              │                    │
  │                        │ Extract token               │                    │
  │                        │                             │                    │
  │                        │ 2. validateToken()          │                    │
  │                        ├────────────────────────────>│                    │
  │                        │                             │ Verify signature   │
  │                        │                             │ Check expiration   │
  │                        │ ◄────────────────────────────│ Return: true/false │
  │                        │                             │                    │
  │                        │ 3. getUserIdFromToken()     │                    │
  │                        ├────────────────────────────>│                    │
  │                        │                             │ Extract "sub"      │
  │                        │                             │ Return: 47         │
  │                        │ ◄────────────────────────────│                    │
  │                        │                             │                    │
  │                        │ 4. getTokenUuidFromToken()  │                    │
  │                        ├────────────────────────────>│                    │
  │                        │                             │ Extract tokenUuid  │
  │                        │ ◄────────────────────────────│ Return: "3c554..."│
  │                        │                             │                    │
  │                        │ 5. getUserIdFromCache()     │                    │
  │                        │ Check if tokenUuid expired  │                    │
  │                        ├──────────────────────────────────────────────────>│
  │                        │                             │   Lookup by UUID   │
  │                        │ ◄──────────────────────────────────────────────────│
  │                        │                             │   Return: 47 or null
  │                        │                             │                    │
  │                        │ 6. loadUserDetailsById()    │                    │
  │                        ├──────────────────────────────────────────────────>│
  │                        │                             │ SELECT from DB     │
  │                        │ ◄──────────────────────────────────────────────────│
  │                        │                             │   Return: User{...}│
  │                        │                             │   + Roles/Perms    │
  │                        │                             │                    │
  │                        │ 7. Store in SecurityContext │                    │
  │                        │ UsernamePasswordAuthToken   │                    │
  │                        │ + User Details + Authorities│                    │
  │                        │                             │                    │
  │                        │ 8. Continue filter chain    │                    │
  │                        ├──────────────────────────────────────────────────>│
  │                        │                             │ (Controller)       │
  │                        │                             │ User ID 47, ADMIN  │
  │                        │                             │ Allowed access ✓   │
  │                        │                             │                    │
  │ 200 OK                 │                             │                    │
  │ [User Data]            │                             │                    │
  │◄───────────────────────┤                             │                    │
```

### Code Flow

#### 1. **Request Arrives with JWT**
```java
GET /api/users
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI0NyIs...
```

#### 2. **JwtAuthenticationFilter Intercepts**
```java
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Step 1: Extract token from Authorization header
            String token = extractTokenFromHeader(request);
            if (token == null) {
                filterChain.doFilter(request, response);  // No token, continue without auth
                return;
            }
            
            // Step 2: Validate token (signature + expiration)
            if (!jwtService.validateToken(token)) {
                filterChain.doFilter(request, response);  // Invalid, continue without auth
                return;
            }
            
            // Step 3: Extract user ID and token UUID
            Long userId = jwtService.getUserIdFromToken(token);
            String tokenUuid = jwtService.getTokenUuidFromToken(token);
            
            // Step 4: Verify token UUID in cache (not logged out)
            Long cachedUserId = tokenCacheService.getUserIdFromCache(tokenUuid);
            if (cachedUserId == null) {
                filterChain.doFilter(request, response);  // Token logged out, continue without auth
                return;
            }
            
            // Step 5: Consistency check (token userId == cached userId)
            if (!userId.equals(cachedUserId)) {
                filterChain.doFilter(request, response);  // Mismatch (tampering detected)
                return;
            }
            
            // Step 6: Load user from database
            UserDetails userDetails = customUserDetailsService.loadUserDetailsById(userId);
            
            // Step 7: Create authentication and store in SecurityContext
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()  // Roles + Permissions
                    );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.info("User {} authenticated successfully", userDetails.getUsername());
            
        } catch (JwtException | NumberFormatException e) {
            log.error("JWT processing error: {}", e.getMessage());
            // Continue without authentication
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            // Continue without authentication
        }
        
        // Step 8: Continue filter chain
        filterChain.doFilter(request, response);
    }
}
```

#### 3. **Token Validation in JwtService**
```java
public boolean validateToken(String token) {
    try {
        // Parse and verify signature using secret key
        Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
            .build()
            .parseClaimsJws(token);  // Throws if signature invalid or expired
        
        return true;
    } catch (ExpiredJwtException e) {
        log.warn("Token expired");
        return false;
    } catch (JwtException e) {
        log.warn("Invalid token");
        return false;
    }
}

public Long getUserIdFromToken(String token) {
    Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
            .build()
            .parseClaimsJws(token)
            .getBody();
    
    return Long.parseLong(claims.getSubject());  // Get "sub" claim
}

public String getTokenUuidFromToken(String token) {
    Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
            .build()
            .parseClaimsJws(token)
            .getBody();
    
    return claims.get("tokenUuid", String.class);  // Get "tokenUuid" claim
}
```

#### 4. **User Loaded with Authorities**
```java
// CustomUserDetailsService loads user with roles + permissions
public UserDetails loadUserDetailsById(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(...);
    
    // Build authorities from role + permissions
    Set<GrantedAuthority> authorities = new HashSet<>();
    
    Role role = user.getRole();
    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
    
    Set<RolePermission> permissions = role.getRolePermissions();
    for (RolePermission perm : permissions) {
        authorities.add(new SimpleGrantedAuthority("PERMISSION_" + perm.getPermission().getName()));
    }
    
    return User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .authorities(authorities)  // [ROLE_ADMIN, PERMISSION_CREATE_USER, ...]
            .build();
}
```

#### 5. **Authorization Check**
```java
// Spring Security now has SecurityContext with user + authorities
// Controller can check authorization

@GetMapping("/users")
@PreAuthorize("hasRole('ADMIN')")  // Only users with ROLE_ADMIN
public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
    // If user has ROLE_ADMIN → allowed
    // If user is ATTENDEE → 403 Forbidden
    List<User> users = userService.getAllUsers();
    return ResponseEntity.ok(userMapper.toDtoList(users));
}
```

---

## 🔒 SECURITY COMPONENTS

### 1. **Password Encoding (BCryptPasswordEncoder)**

**What it does:**
- Hashes passwords using BCrypt algorithm (strength 12)
- Uses salt to prevent rainbow table attacks
- One-way hashing (cannot decrypt)

**Example:**
```
Plain text:  "password"
BCrypt hash: "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ee6IB/9w/9pTVbFi"

Every hash is different due to random salt!
```

**Login Verification:**
```java
boolean matches = passwordEncoder.matches("password", storedHash);
// true if password correct, false otherwise
```

### 2. **JWT Signing (HS512)**

**What it does:**
- Signs token using server secret key
- Ensures token hasn't been modified
- Uses HMAC SHA-512 (strongest hash)

**Process:**
```
Secret Key: "your-super-secret-key-minimum-32-characters..."

Signature = HMACSHA512(
  Header + "." + Payload,
  Secret
)
```

**Verification:**
```java
// Server recalculates signature
calculatedSig = HMACSHA512(receivedHeader + "." + receivedPayload, secret)

// Compare with token signature
if (calculatedSig == tokenSig) {
    // Token valid (not tampered)
} else {
    // Token invalid (forged or tampered)
}
```

### 3. **Token Caching (Server-Side Logout)**

**What it does:**
- Stores token UUID in memory cache
- Tracks which tokens are still valid
- Enables instant logout without database query

**Data Structure:**
```
TokenCache = ConcurrentHashMap<String, TokenEntry>

TokenEntry {
  tokenUuid: "3c5542b4-bdae-4baf-9722-c8f90aeac2fc",
  userId: 47,
  expirationTime: 1733245290000,  // Unix timestamp when token expires
  createdAt: 1733242590000
}
```

**Cache Lookup:**
```
On every request:
  tokenUuid = extract from JWT
  cachedEntry = cache.get(tokenUuid)
  
  if (cachedEntry == null) {
    → Token not in cache (logged out or expired)
    → Return 401 Unauthorized
  }
  
  if (cachedEntry.expirationTime < currentTime) {
    → Token TTL expired
    → Return 401 Unauthorized
  }
  
  if (cachedEntry.userId != jwtUserId) {
    → Mismatch (tampering detected)
    → Return 401 Unauthorized
  }
```

### 4. **Role-Based Access Control (RBAC)**

**What it does:**
- Associates users with roles
- Associates roles with permissions
- Enforces access control at controller level

**Role Hierarchy:**
```
User (id=47) 
  ↓
Role (ADMIN)
  ├─ Permission: CREATE_USER
  ├─ Permission: DELETE_USER
  ├─ Permission: CREATE_EVENT
  └─ Permission: DELETE_EVENT

User (id=48)
  ↓
Role (ATTENDEE)
  └─ Permission: VIEW_EVENT
```

**Endpoint Protection:**
```java
@GetMapping("/users")
@PreAuthorize("hasRole('ADMIN')")  // Only ADMIN role
public ResponseEntity<List<UserResponseDTO>> getAllUsers() { ... }

@PostMapping("/events")
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")  // Admin or Organizer
public ResponseEntity<EventResponseDTO> createEvent(...) { ... }

@GetMapping("/events")
@PreAuthorize("hasAuthority('PERMISSION_VIEW_EVENT')")  // Has specific permission
public ResponseEntity<List<EventResponseDTO>> getEvents() { ... }
```

---

## 📋 ACTIVITY AUDIT TRAIL

### Purpose

The audit trail provides **security visibility** by tracking:
- Login/logout events (who, when, IP address, device info)
- Password changes (who changed it, when)
- Critical operations (user creation, role assignment, event management)

### Audit Trail Components

**1. UserLoginLogoutHistory**
```sql
UserLoginLogoutHistory {
  id: Long,
  user_id: Long,
  login_time: DateTime,
  logout_time: DateTime (nullable),
  ip_address: String,
  device_info: String,
  token_uuid: String,
  status: Enum (SUCCESS, FAILED)
}
```

**Use:** Detect unauthorized access attempts, track login patterns

**2. UserPasswordHistory**
```sql
UserPasswordHistory {
  id: Long,
  user_id: Long,
  old_password_hash: String,
  new_password_hash: String,
  changed_by_id: Long,
  changed_at: DateTime
}
```

**Use:** Verify password change history, detect compromised accounts

**3. UserActivityHistory**
```sql
UserActivityHistory {
  id: Long,
  user_id: Long,
  activity_type: Enum (USER_LOGIN, USER_LOGOUT, USER_CREATED, USER_UPDATED, etc.),
  description: String,
  ip_address: String,
  device_id: String,
  session_id: String,
  created_at: DateTime
}
```

**Use:** Complete audit trail of all critical operations

### Security Benefits

✅ **Detect Breaches** - Unusual login patterns
✅ **Compliance** - Meet regulatory requirements (audit logs)
✅ **Forensics** - Investigate security incidents
✅ **Account Recovery** - Verify legitimate vs unauthorized access

---

### 1. **Invalid Credentials Response**

```java
// DON'T DO THIS (reveals information):
if (userNotFound) {
    return "User email not found";  // WRONG! Leaks valid emails
}
if (passwordIncorrect) {
    return "Password incorrect";    // WRONG! Confirms email exists
}

// DO THIS (generic message):
if (!userFound || !passwordMatches) {
    return "Invalid credentials";  // CORRECT! Doesn't leak info
}
```

### 2. **Token Verification Errors**

| Error | Reason | Response |
|-------|--------|----------|
| **No token** | Header missing or format wrong | Continue without auth (public endpoint) or 401 |
| **Invalid signature** | Token tampered or forged | 401 Unauthorized |
| **Expired** | Token past expiration time | 401 Unauthorized |
| **Not in cache** | User logged out or token invalid | 401 Unauthorized |
| **User ID mismatch** | Token tampering detected | 401 Unauthorized |
| **User not found** | User deleted after login | 401 Unauthorized |

### 3. **Authorization Errors**

```java
// User tries to access admin endpoint without admin role
try {
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<...> adminEndpoint() { ... }
} catch (AccessDeniedException e) {
    // Spring returns 403 Forbidden
    return 403 Forbidden
}
```

---

## ⏱️ TOKEN LIFECYCLE

### Access Token

```
Timeline:
┌─────────────────────────────────────────────────┐
│ Generated  +45 min  Expiration  +n days  Deleted│
│ (login)    (TTL)    (exp claim) (cache TTL)    │
└─────────────────────────────────────────────────┘
     │          │          │           │           │
     ▼          ▼          ▼           ▼           ▼
  00:00       45:00      45:00       52:00      Removed
  Token       Still      Token       Cache
  created     valid      expired     cleaned up
```

**Life Events:**
1. **Generated** - User login
2. **Valid** - For 45 minutes (exp claim)
3. **Expired** - After 45 minutes (validateToken fails)
4. **Cached** - TTL of 45 minutes (cache entry valid)
5. **Removed** - After TTL or logout

### Refresh Token

```
Timeline:
┌───────────────────────────────────────────────────────────┐
│ Generated  +7 days  Expiration  +7 days  Deleted         │
│ (login)    (TTL)    (exp claim) (cache TTL)             │
└───────────────────────────────────────────────────────────┘
     │          │          │           │           │
     ▼          ▼          ▼           ▼           ▼
  Day 0      Day 7      Day 7      Day 14     Removed
  Token      Still      Token       Cache
  created    valid      expired     cleaned up
```

**Life Events:**
1. **Generated** - User login (different UUID than access token)
2. **Valid** - For 7 days (exp claim)
3. **Used to refresh** - Client calls `/api/auth/refresh`
4. **Expired** - After 7 days (validateToken fails)
5. **Removed** - After TTL or logout

---

## 🚀 DEPLOYMENT SECURITY CHECKLIST

### Before Production

- [ ] **1. Rotate JWT Secret**
  ```bash
  # Generate secure random secret (base64 32+ bytes):
  openssl rand -base64 32
  # Store in environment variable: APP_JWT_SECRET
  ```

- [ ] **2. Use HTTPS**
  - JWT must travel over HTTPS to prevent interception
  - Configure SSL/TLS certificate

- [ ] **3. Set Secure HTTP Headers**
  ```
  Strict-Transport-Security: max-age=31536000
  X-Content-Type-Options: nosniff
  X-Frame-Options: DENY
  X-XSS-Protection: 1; mode=block
  ```

- [ ] **4. Adjust Token Expiration Times**
  - Current: Access 45 min, Refresh 7 days
  - Consider: Lower if sensitive, higher if usability concern

- [ ] **5. Enable CORS Properly**
  ```java
  @Configuration
  public class CorsConfig {
      @Bean
      public WebMvcConfigurer corsConfigurer() {
          return new WebMvcConfigurer() {
              @Override
              public void addCorsMappings(CorsRegistry registry) {
                  registry.addMapping("/api/**")
                      .allowedOrigins("https://your-domain.com")  // Whitelist only
                      .allowedMethods("GET", "POST", "PUT", "DELETE")
                      .allowCredentials(true);
              }
          };
      }
  }
  ```

- [ ] **6. Implement Rate Limiting**
  - Limit login attempts (e.g., 5 per minute)
  - Prevent brute force attacks

- [ ] **7. Add Refresh Token Rotation**
  - Issue new refresh token on each use
  - Invalidate old refresh token

- [ ] **8. Monitor Security Logs**
  - Log all authentication failures
  - Alert on suspicious patterns (repeated failures, etc.)

- [ ] **9. Use Spring Security's Latest Version**
  - Current: Spring Security 6.2.8 ✓
  - Keep updated for security patches

- [ ] **10. Database Password Security**
  - All passwords in DB must be BCrypt hashes
  - Never store plain text passwords

---

## 📊 SECURITY METRICS

### Current Configuration

| Metric | Value | Assessment |
|--------|-------|------------|
| **JWT Algorithm** | HS512 | ✓ Strong (256-bit security) |
| **Password Encoding** | BCrypt (strength 12) | ✓ Strong (~100ms per hash) |
| **Access Token TTL** | 45 minutes | ✓ Good (short-lived) |
| **Refresh Token TTL** | 7 days | ✓ Reasonable |
| **Token Storage** | Server-side cache | ✓ Enables logout |
| **HTTPS Required** | Configure before prod | ⚠️ Required for security |
| **CORS** | All origins allowed | ⚠️ Restrict in prod |
| **Rate Limiting** | Not implemented | ⚠️ Recommended |

---

## 🎯 SUMMARY

### How Security Works

1. **Login** → User sends email+password
2. **Verification** → Server checks password with BCrypt
3. **Token Generation** → Server creates signed JWT with UUID
4. **Caching** → Server stores token UUID in cache (45 min)
5. **Response** → Server returns JWT to client
6. **Request** → Client includes JWT in Authorization header
7. **Extraction** → Filter extracts JWT from header
8. **Validation** → Filter verifies signature hasn't changed
9. **Cache Lookup** → Filter checks if token UUID still cached
10. **User Loading** → Filter loads user + authorities from DB
11. **Authorization** → Filter stores in SecurityContext
12. **Access** → Controller checks role/permission via @PreAuthorize
13. **Response** → User gets data if authorized
14. **Audit** → Activity tracked in database for compliance

### Key Security Features (v1.3.0)

✅ **Stateless Authentication** - No session storage needed  
✅ **Tamper-proof Tokens** - HS512 signature verification  
✅ **Server-side Logout** - Token cache enables instant logout  
✅ **BCrypt Password Hashing** - Protects passwords at rest  
✅ **Role-Based Access Control** - Fine-grained authorization  
✅ **Token Expiration** - Limited lifetime reduces breach impact  
✅ **Audit Trail** - Complete activity logging for compliance  
✅ **Password History** - Track password changes for security  
✅ **Login/Logout Tracking** - Detect unauthorized access  

---

**Last Updated:** December 17, 2025  
**Version:** 1.3.0  
**Status:** Production Ready (with deployment checklist items completed)

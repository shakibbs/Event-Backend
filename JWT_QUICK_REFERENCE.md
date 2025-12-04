# JWT Security - Quick Reference Guide

## For Developers & Client Integration

### Overview
This Event Management System uses JWT (JSON Web Token) for stateless authentication and Spring Security for authorization.

## Login Flow

### Step 1: Login (Get Tokens)
```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

Response (HTTP 200):
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 2700,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "John Doe",
    "role": "ADMIN"
  }
}
```

### Step 2: Store Tokens (Client-side)
```javascript
// Store in localStorage
localStorage.setItem("accessToken", response.accessToken);
localStorage.setItem("refreshToken", response.refreshToken);
```

### Step 3: Use Access Token in Requests
```bash
GET /api/events
Authorization: Bearer <ACCESS_TOKEN>

Response: Event data (HTTP 200)
```

### Step 4: Refresh Token When Expired
When access token expires (after 45 minutes):
```bash
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<REFRESH_TOKEN>"
}

Response (HTTP 200):
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "<SAME_REFRESH_TOKEN>",
  "tokenType": "Bearer",
  "expiresIn": 2700,
  "user": {...}
}

// Update localStorage
localStorage.setItem("accessToken", response.accessToken);
```

### Step 5: Logout
```bash
POST /api/auth/logout
Authorization: Bearer <ACCESS_TOKEN>

Response (HTTP 200):
{
  "message": "Logged out successfully"
}

// Clear tokens from client
localStorage.removeItem("accessToken");
localStorage.removeItem("refreshToken");
```

## Token Details

### Access Token
- **Lifespan**: 45 minutes
- **Purpose**: Used in every API request
- **Header**: `Authorization: Bearer <token>`
- **Contains**: User ID, Token UUID, Issue Time, Expiration Time

### Refresh Token
- **Lifespan**: 7 days
- **Purpose**: Used to get new access token when expired
- **Endpoint**: `POST /api/auth/refresh`
- **Contains**: User ID, Token UUID, Issue Time, Expiration Time

## Public Endpoints (No Auth Required)

```
POST   /api/auth/login              → Login
POST   /api/auth/refresh            → Refresh access token
POST   /api/auth/logout             → Logout
GET    /swagger-ui.html             → API Documentation
GET    /swagger-ui/**               → Swagger UI resources
GET    /api-docs/**                 → API Docs JSON
POST   /api/users/register          → Register new user (when available)
```

## Protected Endpoints (Auth Required)

All other endpoints require valid access token in Authorization header:
```
Authorization: Bearer <ACCESS_TOKEN>
```

Examples:
```
GET    /api/events                  → List all events
POST   /api/events                  → Create new event
GET    /api/events/{id}             → Get event details
PUT    /api/events/{id}             → Update event
DELETE /api/events/{id}             → Delete event
GET    /api/users                   → List users
GET    /api/roles                   → List roles
...and more
```

## Error Responses

### 401 Unauthorized
Occurs when:
- No Authorization header provided
- Invalid token format
- Token signature invalid
- Token expired
- Token UUID not in cache (logged out)
- User not found in database

```
HTTP 401 Unauthorized
```

### 403 Forbidden
Occurs when:
- User authenticated but insufficient permissions
- User role doesn't have required permission

```
HTTP 403 Forbidden
```

### 400 Bad Request
Occurs when:
- Invalid JSON in request body
- Missing required fields
- Invalid field values (e.g., invalid email format)

```
HTTP 400 Bad Request
{
  "error": "Invalid request"
}
```

## Client Implementation Examples

### JavaScript (Fetch API)

```javascript
// 1. Login
async function login(email, password) {
  const response = await fetch('http://localhost:8083/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  
  if (response.ok) {
    const data = await response.json();
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    return data;
  } else {
    throw new Error('Login failed');
  }
}

// 2. Make API request with token
async function getEvents() {
  const token = localStorage.getItem('accessToken');
  const response = await fetch('http://localhost:8083/api/events', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  if (response.status === 401) {
    // Token expired, refresh it
    await refreshToken();
    return getEvents(); // Retry with new token
  }
  
  return response.json();
}

// 3. Refresh token
async function refreshToken() {
  const refreshToken = localStorage.getItem('refreshToken');
  const response = await fetch('http://localhost:8083/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });
  
  if (response.ok) {
    const data = await response.json();
    localStorage.setItem('accessToken', data.accessToken);
    return data;
  } else {
    throw new Error('Token refresh failed - Please login again');
  }
}

// 4. Logout
async function logout() {
  const token = localStorage.getItem('accessToken');
  await fetch('http://localhost:8083/api/auth/logout', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}
```

### JavaScript (Axios)

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8083'
});

// Add token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 responses
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      try {
        const { data } = await axios.post('/api/auth/refresh', {
          refreshToken: localStorage.getItem('refreshToken')
        });
        localStorage.setItem('accessToken', data.accessToken);
        
        // Retry original request
        return api.request(error.config);
      } catch {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Usage
async function fetchEvents() {
  const response = await api.get('/api/events');
  return response.data;
}
```

### Python (requests)

```python
import requests

BASE_URL = 'http://localhost:8083'
tokens = {}

def login(email, password):
    global tokens
    response = requests.post(f'{BASE_URL}/api/auth/login', json={
        'email': email,
        'password': password
    })
    if response.status_code == 200:
        data = response.json()
        tokens = {
            'access': data['accessToken'],
            'refresh': data['refreshToken']
        }
        return data
    raise Exception('Login failed')

def api_request(method, endpoint, **kwargs):
    global tokens
    headers = kwargs.get('headers', {})
    headers['Authorization'] = f"Bearer {tokens['access']}"
    kwargs['headers'] = headers
    
    response = requests.request(method, f'{BASE_URL}{endpoint}', **kwargs)
    
    if response.status_code == 401:
        refresh_token()
        return api_request(method, endpoint, **kwargs)
    
    return response

def refresh_token():
    global tokens
    response = requests.post(f'{BASE_URL}/api/auth/refresh', json={
        'refreshToken': tokens['refresh']
    })
    if response.status_code == 200:
        data = response.json()
        tokens['access'] = data['accessToken']
    else:
        raise Exception('Token refresh failed')

def logout():
    global tokens
    requests.post(f'{BASE_URL}/api/auth/logout', headers={
        'Authorization': f"Bearer {tokens['access']}"
    })
    tokens = {}

# Usage
login('admin@example.com', 'password')
events = api_request('GET', '/api/events').json()
logout()
```

## Security Guidelines

### DO's ✅
- Store tokens securely (httpOnly cookies or localStorage)
- Always send token in Authorization header
- Implement token refresh logic
- Clear tokens on logout
- Use HTTPS in production
- Validate token expiration on client
- Implement error handling for 401/403

### DON'Ts ❌
- Don't send tokens in URL parameters
- Don't store tokens in plain text
- Don't expose tokens in logs
- Don't send tokens to untrusted servers
- Don't forget to refresh tokens
- Don't ignore 401 responses
- Don't hardcode credentials
- Don't use HTTP in production (use HTTPS)

## Testing the API

### Using cURL

```bash
# 1. Login
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password"}'

# 2. Save tokens (from response above)
export ACCESS_TOKEN="eyJ..."
export REFRESH_TOKEN="eyJ..."

# 3. Get events with token
curl -X GET http://localhost:8083/api/events \
  -H "Authorization: Bearer $ACCESS_TOKEN"

# 4. Refresh token
curl -X POST http://localhost:8083/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"

# 5. Logout
curl -X POST http://localhost:8083/api/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### Using Postman

1. **Create Postman Environment Variable**
   - Variable name: `accessToken`
   - Initial value: (leave empty)

2. **Login Request**
   - Method: POST
   - URL: `{{base_url}}/api/auth/login`
   - Body (JSON): `{"email":"admin@example.com","password":"password"}`
   - Tests: Set token in environment
     ```javascript
     pm.environment.set("accessToken", pm.response.json().accessToken);
     ```

3. **Protected Endpoint Request**
   - Method: GET
   - URL: `{{base_url}}/api/events`
   - Headers:
     - Key: `Authorization`
     - Value: `Bearer {{accessToken}}`

## Token Structure

Each JWT consists of 3 parts separated by dots:

```
Header.Payload.Signature
```

### Decoded Example

**Header** (Algorithm & Type):
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload** (Claims):
```json
{
  "sub": "1",
  "tokenUuid": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1632339650,
  "exp": 1632340550
}
```

**Signature**:
```
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret_key
)
```

## Configuration

All JWT settings are in `application.properties`:

```properties
# JWT Secret (change in production)
app.jwt.secret=your-super-secret-key-minimum-32-characters-change-in-production-1234567890

# Access token expires in 45 minutes
app.jwt.access-token-expiration=2700000

# Refresh token expires in 7 days
app.jwt.refresh-token-expiration=604800000

# Cache type (change to 'redis' for distributed caching)
spring.cache.type=simple
```

## Troubleshooting

### "Invalid credentials" on login
- Verify email exists in database
- Verify password is correct
- Check user status is ACTIVE

### "Unauthorized" (401) on protected endpoint
- Check Authorization header is present
- Verify token format: `Bearer <token>`
- Confirm token hasn't expired (45 minutes)
- Try refreshing token
- Verify token UUID in cache (not logged out)

### "Forbidden" (403) on protected endpoint
- User is authenticated but lacks permission
- Check user's role has required permissions
- Verify role-permission mapping in database

### Token refresh fails
- Refresh token expired (> 7 days)
- Refresh token UUID not in cache
- User was deleted from database
- Refresh token was invalidated on server

## Support

For issues or questions:
1. Check API documentation: http://localhost:8083/swagger-ui.html
2. Review logs: Check application console for errors
3. Verify database: Ensure user and role data exists
4. Test with cURL: Isolate client issues from server issues

---

Last Updated: December 4, 2025

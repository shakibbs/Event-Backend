# Event Management System - Backend

A comprehensive RESTful API for managing events, attendees, roles, and permissions with JWT authentication and email notifications.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Local Development Setup](#local-development-setup)
- [Configuration](#configuration)
- [Database Setup](#database-setup)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Deployment](#deployment)
  - [Docker Deployment](#docker-deployment)
  - [Render Deployment](#render-deployment)
  - [AWS Deployment](#aws-deployment)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [Environment Variables](#environment-variables)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Features

✅ **Event Management**
- Create, read, update, and delete events
- Event approval workflow (Pending → Approved/Rejected)
- Event status tracking (Upcoming, Ongoing, Completed, Cancelled, Inactive)
- Public and Private event visibility
- Event hold and reactivation

✅ **User Management**
- User registration and authentication
- Role-based access control (RBAC)
- User activity and login history tracking
- Password history management
- Auto-account creation for event invitations

✅ **Attendee Management**
- Event invitations via email
- RSVP responses (Accept/Decline)
- Attendee status tracking

✅ **Role & Permission Management**
- Super Admin, Admin, and Attendee roles
- Granular permission-based access control
- Dynamic role creation and modification
- Permission assignment to roles

✅ **Email Notifications**
- Event reminders before start time
- Event invitations with custom tokens
- RSVP confirmation emails
- Auto-account credential delivery

✅ **API Features**
- JWT token-based authentication
- Swagger/OpenAPI documentation
- Comprehensive logging and error handling
- Pagination and filtering support
- CORS enabled for frontend integration

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| **Backend Framework** | Spring Boot 3.5.3 |
| **Java Version** | OpenJDK 17 |
| **Database** | MySQL 8.0+ |
| **ORM** | Hibernate/JPA |
| **Authentication** | JWT (JJWT 0.12.3) |
| **Email** | JavaMail with Gmail SMTP |
| **Reporting** | JasperReports 6.21.0 |
| **Documentation** | SpringDoc OpenAPI 2.1.0 |
| **Security** | Spring Security 6.x |
| **Build Tool** | Maven 3.8+ |
| **Container** | Docker & Dockerfile |

---

## Prerequisites

### Local Development
- **Java**: OpenJDK 17 or higher
- **Maven**: 3.8.0 or higher
- **MySQL**: 8.0 or higher
- **Git**: Latest version
- **IDE**: IntelliJ IDEA, VS Code, or Eclipse

### For Docker/Render Deployment
- **Docker**: Latest version (for local Docker testing)
- **Render Account**: https://render.com (for cloud deployment)
- **GitHub Account**: For pushing code to repository

---

## Local Development Setup

### 1. Clone the Repository

```bash
git clone https://github.com/shakibbs/Event-Backend.git
cd Event-Backend
```

### 2. Setup Environment Variables

Copy `.env.example` to `.env` and fill in your local configuration:

```bash
cp .env.example .env
```

Edit `.env` with your values:
```env
DATABASE_URL=jdbc:mysql://localhost:3306/event_management_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DATABASE_USERNAME=root
DATABASE_PASSWORD=your_mysql_password

JWT_SECRET=your-super-secret-key-minimum-32-characters
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### 3. Create MySQL Database

```bash
mysql -u root -p
```

```sql
CREATE DATABASE event_management_db;
USE event_management_db;
```

Or run the initialization script:
```bash
mysql -u root -p event_management_db < scripts/init.sql
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

The API will be available at: `http://localhost:8083`

---

## Configuration

### Application Properties

All configuration is managed through environment variables (see `.env.example`):

```properties
# Server
PORT=8083
APP_BASE_URL=http://localhost:8083

# Database
DATABASE_URL=jdbc:mysql://localhost:3306/event_management_db
DATABASE_USERNAME=root
DATABASE_PASSWORD=password

# JWT
JWT_SECRET=your-secure-key-minimum-32-chars
JWT_ACCESS_TOKEN_EXPIRATION=2700000    # 45 minutes
JWT_REFRESH_TOKEN_EXPIRATION=604800000 # 7 days

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### Generating Secure JWT Secret

Use OpenSSL to generate a secure random key:

```bash
# Linux/Mac
openssl rand -hex 32

# Windows PowerShell
[BitConverter]::ToString([System.Security.Cryptography.RNGCryptoServiceProvider]::new().GetBytes(32))
```

### Gmail App Password Setup

1. Enable 2-Step Verification on your Google Account
2. Go to https://myaccount.google.com/apppasswords
3. Select "Mail" and "Windows Computer"
4. Generate a 16-character password
5. Use this password in `MAIL_PASSWORD` (without spaces)

---

## Database Setup

### Schema Initialization

The application uses Hibernate with `ddl-auto=update` for development and `ddl-auto=validate` for production.

### Initial Data

Default roles and permissions are created automatically on first startup:

**Default Roles:**
- **SuperAdmin**: Full system access
- **Admin**: Event management and user management (own)
- **Attendee**: Event viewing and attendance

**Default User:**
- **Email**: superadmin@ems.com
- **Password**: SuperAdmin@123

**Change the default password after first login!**

---

## Running the Application

### Development Mode

```bash
# With Maven
mvn spring-boot:run

# With IDE
Run EventManagementSystemApplication.java from your IDE
```

### Production Mode

```bash
# Build JAR
mvn clean package -DskipTests

# Run JAR
java -Dserver.port=8083 -jar target/event_management_system-0.0.1-SNAPSHOT.jar
```

### With Environment Variables

```bash
export DATABASE_URL="jdbc:mysql://production-host:3306/event_db"
export DATABASE_USERNAME="dbuser"
export DATABASE_PASSWORD="dbpassword"
export JWT_SECRET="your-production-secret-key"
export MAIL_USERNAME="noreply@yourdomain.com"
export MAIL_PASSWORD="your-app-password"

mvn spring-boot:run
```

---

## API Documentation

### Swagger UI

Once the application is running, access the interactive API documentation:

```
http://localhost:8083/swagger-ui.html
```

### API Docs (JSON)

```
http://localhost:8083/api-docs
```

---

## Deployment

### Docker Deployment

#### Build Docker Image

```bash
docker build -t event-management-backend:latest .
```

#### Run Docker Container

```bash
docker run -d \
  -p 8083:8083 \
  -e PORT=8083 \
  -e DATABASE_URL="jdbc:mysql://mysql-host:3306/event_db" \
  -e DATABASE_USERNAME="dbuser" \
  -e DATABASE_PASSWORD="dbpassword" \
  -e JWT_SECRET="your-secure-key" \
  -e MAIL_USERNAME="your-email@gmail.com" \
  -e MAIL_PASSWORD="your-app-password" \
  event-management-backend:latest
```

### Render Deployment

#### Prerequisites

1. Push code to GitHub repository
2. Create Render account at https://render.com
3. Set up MySQL database on Render

#### Deployment Steps

1. **Connect GitHub Repository**
   - Go to Render Dashboard
   - Click "New +" → "Web Service"
   - Connect your GitHub repository
   - Select the Event-Backend repository

2. **Configure Build Command**
   ```
   mvn clean package -DskipTests
   ```

3. **Configure Start Command**
   ```
   java -Dserver.port=$PORT -jar target/event_management_system-0.0.1-SNAPSHOT.jar
   ```

4. **Set Environment Variables**
   - `PORT`: 10000
   - `DATABASE_URL`: (from Render MySQL connection string)
   - `DATABASE_USERNAME`: (from Render MySQL)
   - `DATABASE_PASSWORD`: (from Render MySQL)
   - `JWT_SECRET`: (generate secure key)
   - `APP_BASE_URL`: https://your-app.onrender.com
   - `MAIL_USERNAME`: your-email@gmail.com
   - `MAIL_PASSWORD`: your-app-password

5. **Deploy**
   - Click "Deploy"
   - Monitor deployment logs
   - Application will be live at: https://your-app.onrender.com

#### Using render.yaml (IaC)

Alternative: Use the provided `render.yaml` for Infrastructure as Code deployment:

```bash
git push origin main
# Render will automatically detect render.yaml and deploy accordingly
```

### AWS Deployment

#### Using Elastic Beanstalk

```bash
# Install AWS CLI and EB CLI
pip install awsebcli

# Initialize Elastic Beanstalk
eb init -p java-17 event-management-backend
eb create production
eb deploy
```

#### Using EC2 + RDS

1. Launch EC2 instance (Amazon Linux 2 or Ubuntu)
2. Install Java 17
3. Create RDS MySQL database
4. Deploy JAR and configure systemd service

---

## Project Structure

```
Event-Backend/
├── src/main/
│   ├── java/com/event_management_system/
│   │   ├── config/              # Spring configuration (Security, JWT, Swagger)
│   │   ├── controller/          # REST API endpoints
│   │   ├── dto/                 # Data Transfer Objects
│   │   ├── entity/              # JPA entities
│   │   ├── exception/           # Custom exceptions
│   │   ├── mapper/              # Entity-DTO mappers
│   │   ├── repository/          # Data access layer
│   │   ├── security/            # JWT and security filters
│   │   ├── service/             # Business logic
│   │   ├── util/                # Utility classes
│   │   └── EventManagementSystemApplication.java
│   └── resources/
│       ├── application.properties
│       ├── logback-spring.xml
│       └── dynamic_export.jrxml
├── scripts/                      # Database initialization scripts
│   ├── init.sql
│   └── schema.sql
├── Dockerfile                    # Docker configuration
├── render.yaml                   # Render deployment configuration
├── pom.xml                       # Maven configuration
├── .env.example                  # Environment variables template
└── README.md                     # This file
```

---

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login and get JWT token
- `POST /api/auth/register` - Register new user
- `POST /api/auth/refresh` - Refresh JWT token

### Events
- `GET /api/events` - List all events
- `GET /api/events/{id}` - Get event details
- `POST /api/events` - Create new event
- `PUT /api/events/{id}` - Update event
- `DELETE /api/events/{id}` - Delete event
- `POST /api/events/{id}/action` - Approve/Reject event
- `GET /api/events/download/pdf` - Export events as PDF

### Users
- `GET /api/users` - List all users
- `GET /api/users/{id}` - Get user details
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user
- `GET /api/users/download/pdf` - Export users as PDF

### Roles
- `GET /api/roles` - List all roles
- `POST /api/roles` - Create new role
- `PUT /api/roles/{id}` - Update role
- `DELETE /api/roles/{id}` - Delete role

### Permissions
- `GET /api/permissions` - List all permissions
- `POST /api/permissions` - Create new permission
- `PUT /api/permissions/{id}` - Update permission
- `DELETE /api/permissions/{id}` - Delete permission

### Activity
- `GET /api/activities` - View user activity history
- `GET /api/activities/download/pdf` - Export activity as PDF

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | 8083 | Server port |
| `APP_BASE_URL` | http://localhost:8083 | Application base URL |
| `DATABASE_URL` | jdbc:mysql://localhost:3306/event_management_db | MySQL connection string |
| `DATABASE_USERNAME` | root | Database username |
| `DATABASE_PASSWORD` | 765614 | Database password |
| `JWT_SECRET` | (min 32 chars) | JWT signing secret |
| `JWT_ACCESS_TOKEN_EXPIRATION` | 2700000 | Access token lifetime (ms) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | 604800000 | Refresh token lifetime (ms) |
| `MAIL_HOST` | smtp.gmail.com | SMTP server hostname |
| `MAIL_PORT` | 587 | SMTP server port |
| `MAIL_USERNAME` | noreply@eventmanagement.com | SMTP username |
| `MAIL_PASSWORD` | (empty) | SMTP password (app password for Gmail) |
| `ENVIRONMENT` | development | Environment (development/staging/production) |

---

## Troubleshooting

### Database Connection Failed

**Error**: `Communications link failure`

**Solution**:
1. Verify MySQL is running: `mysql -u root -p`
2. Check DATABASE_URL is correct
3. Verify DATABASE_USERNAME and DATABASE_PASSWORD
4. Ensure database exists: `CREATE DATABASE event_management_db;`

### Port Already in Use

**Error**: `Address already in use`

**Solution**:
```bash
# Find process using port 8083
lsof -i :8083

# Kill the process
kill -9 <PID>

# Or use different port
PORT=8084 mvn spring-boot:run
```

### JWT Token Expired

**Error**: `401 Unauthorized - Token expired`

**Solution**:
1. Use refresh token endpoint to get new access token
2. Or login again to get new token

### Email Not Sending

**Error**: `SMTP authentication failed`

**Solution**:
1. Use Gmail App Password (not regular password)
2. Enable 2-Factor authentication on Google Account
3. Verify MAIL_USERNAME and MAIL_PASSWORD in .env
4. Check MAIL_HOST and MAIL_PORT are correct

### Render Deployment Fails

**Error**: `Build command failed`

**Solution**:
1. Check logs: `mvn clean install`
2. Verify Java 17 is available
3. Ensure pom.xml is correct
4. Check for SNAPSHOT dependencies

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit changes: `git commit -am 'Add your feature'`
4. Push to branch: `git push origin feature/your-feature`
5. Submit a Pull Request

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## Support & Contact

For issues, questions, or suggestions:
- Open an issue on GitHub
- Contact: shakibbs@example.com

---

## Changelog

### Version 1.0.0 (2026-01-30)
- Initial release
- Event management system
- User and role management
- JWT authentication
- Email notifications
- Render deployment support

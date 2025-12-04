# Event Management System

A Spring Boot-based REST API for managing events, built with Java and following best practices for enterprise application development.

## Table of Contents

- [Features](#features)
- [Technologies Used](#technologies-used)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Contributing](#contributing)

## Features

- CRUD operations for event management
- RESTful API design
- DTO pattern for request/response handling
- Global exception handling
- Swagger/OpenAPI documentation
- JPA/Hibernate for data persistence
- Maven build system
- Unit testing framework

## Technologies Used

- **Java 17+**
- **Spring Boot 3.x**
- **Spring Web**
- **Spring Data JPA**
- **MySQL Database** (for development and production)
- **Swagger/OpenAPI 3**
- **Maven**
- **JUnit 5**

## Project Structure

```
src/main/java/com/event_management_system/
├── config/                 # Configuration classes
│   └── SwaggerConfig.java  # Swagger documentation configuration
├── controller/             # REST controllers
│   └── EventController.java
├── dto/                    # Data Transfer Objects
│   ├── EventRequestDTO.java
│   └── EventResponseDTO.java
├── entity/                 # JPA entities
│   ├── BaseEntity.java
│   └── Event.java
├── exception/              # Exception handling
│   └── GlobalExceptionHandler.java
├── mapper/                 # Object mapping utilities
│   └── EventMapper.java
├── repository/             # JPA repositories
│   └── EventRepository.java
├── service/                # Business logic
│   └── EventService.java
└── EventManagementSystemApplication.java
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd event_management_system
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Accessing API Documentation

Once the application is running, you can access the Swagger UI at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## API Endpoints

### Event Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/events` | Get all events |
| GET | `/api/events/{id}` | Get event by ID |
| POST | `/api/events` | Create a new event |
| PUT | `/api/events/{id}` | Update an existing event |
| DELETE | `/api/events/{id}` | Delete an event |

### Request/Response Examples

#### Create Event Request
```json
{
  "title": "Sample Event",
  "description": "This is a sample event description",
  "date": "2024-12-31T23:59:59",
  "location": "Sample Location"
}
```

#### Event Response
```json
{
  "id": 1,
  "title": "Sample Event",
  "description": "This is a sample event description",
  "date": "2024-12-31T23:59:59",
  "location": "Sample Location",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

## Testing

### Running Tests

```bash
mvn test
```

### Test Coverage

The project includes unit tests for the main application components. Test files are located in:
```
src/test/java/com/event_management_system/
```

### API Testing Scripts

The project includes PowerShell scripts for API testing:
- `test_api.ps1` - Basic API endpoint testing
- `test_error_scenarios.ps1` - Error scenario testing
- `test_event.json` - Sample event data for testing

## Configuration

### Database Configuration

The application uses MySQL database. Database settings can be configured in `src/main/resources/application.properties`:

```properties
# MySQL Database
spring.datasource.url=jdbc:mysql://localhost:3306/event_management_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

Note: Make sure to update the password field with your actual MySQL password.

### Server Configuration

Default server configuration:
- Port: 8080
- Context path: /

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

For any questions or suggestions, please open an issue in the repository.
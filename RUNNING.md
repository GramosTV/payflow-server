# Instructions for running the PayFlow Lite Server

## Prerequisites

- Java 17 or newer
- Maven (or use the included Maven wrapper)
- Docker and Docker Compose (optional, for containerized deployment)

## Development Setup

### Building the application

```bash
# Navigate to the project directory
cd payflow-server

# Build with Maven
mvn clean package -DskipTests

# Or using Maven wrapper
./mvnw clean package -DskipTests  # Linux/Mac
mvnw.cmd clean package -DskipTests  # Windows
```

### Environment Setup

By default, the application uses the `dev` profile which includes:

- H2 in-memory database
- Debug logging
- Error details in responses
- Sample data initialization

## Running the application

```bash
# Run the application with Java
java -jar target/payflow-api-0.0.1-SNAPSHOT.jar

# Or using Maven
mvn spring-boot:run

# Or using Maven wrapper
./mvnw spring-boot:run  # Linux/Mac
mvnw.cmd spring-boot:run  # Windows
```

## Running with Docker

For containerized deployment, you can use Docker and Docker Compose:

```bash
# Navigate to the parent directory containing docker-compose.yml
cd ..

# Build and run the containers
docker-compose up -d

# Stop the containers
docker-compose down

# View logs
docker-compose logs -f payflow-api
```

The Docker setup includes:

- PostgreSQL database container
- PayFlow API container
- Configured networking between containers
- Persistent volume for database data

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with test coverage report
mvn test jacoco:report
```

Test reports will be available in `target/site/jacoco/index.html`.

## Production Deployment

To run in production mode:

```bash
# Run with production profile
java -jar target/payflow-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# Or with specific database configuration
java -jar target/payflow-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/payflow \
  --spring.datasource.username=postgres \
  --spring.datasource.password=postgres
```

## Accessing the application

Once the application is running:

- API base URL: http://localhost:8080/api/v1/
- Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
- H2 Console (dev profile only): http://localhost:8080/api/v1/h2-console

## Demo Users

The application is pre-configured with two demo users:

1. **Admin User**

   - Email: admin@payflow.com
   - Password: admin123

2. **Regular User**
   - Email: user@payflow.com
   - Password: user123

## API Documentation

API documentation is available via Swagger UI when the application is running. You can access it at:

http://localhost:8080/api/v1/swagger-ui.html

This provides interactive documentation where you can:

- View available endpoints
- Read request/response models
- Test API calls directly
- Authorize with JWT tokens

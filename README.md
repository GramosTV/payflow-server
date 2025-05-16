# PayFlow Lite - Fintech Web App Server

PayFlow Lite is a full-stack fintech web application that simulates a modern digital wallet system. This repository contains the Java Spring Boot server-side implementation.

## Features

- 🔐 **User Authentication**: JWT-based login and registration with role-based access
- 💰 **Multi-Currency Wallets**: Hold balances in multiple currencies with real-time exchange rates
- 📤 **Peer-to-Peer Transfers**: Send money instantly to other users
- 📥 **Money Requests**: Request money from other users with accept/decline functionality
- 📱 **QR Code Payments**: Generate and scan QR codes for payments
- 📊 **Dashboard Data**: API endpoints for wallet balance and transaction history

## Technology Stack

- **Framework**: Spring Boot 2.7
- **Language**: Java 17
- **Database**: H2 (development), PostgreSQL (production)
- **Security**: Spring Security with JWT
- **API Documentation**: OpenAPI (Swagger)
- **QR Code**: ZXing library
- **Testing**: JUnit 5, Mockito, JaCoCo (code coverage)

## API Endpoints

### Authentication

- `POST /api/v1/auth/signup`: Register a new user
- `POST /api/v1/auth/login`: Login a user

### Wallets

- `POST /api/v1/wallets`: Create a new wallet
- `GET /api/v1/wallets`: Get all wallets for the current user
- `GET /api/v1/wallets/{walletId}`: Get a wallet by ID
- `POST /api/v1/wallets/topup`: Top up (add funds to) a wallet
- `GET /api/v1/wallets/{walletId}/transactions`: Get transactions for a wallet

### Transactions

- `POST /api/v1/transactions/transfer`: Transfer money between wallets
- `GET /api/v1/transactions`: Get all transactions for the current user
- `GET /api/v1/transactions/{transactionId}`: Get a transaction by ID
- `GET /api/v1/transactions/search`: Search transactions by date range

### Money Requests

- `POST /api/v1/money-requests`: Request money from another user
- `GET /api/v1/money-requests/sent`: Get money requests sent by current user
- `GET /api/v1/money-requests/received`: Get money requests received by current user
- `POST /api/v1/money-requests/process`: Approve or decline a money request
- `POST /api/v1/money-requests/{requestNumber}/cancel`: Cancel a money request
- `GET /api/v1/money-requests/pending`: Get pending money requests for current user

### QR Codes

- `POST /api/v1/qr-codes`: Create a new QR code for a wallet
- `GET /api/v1/qr-codes`: Get all QR codes for the current user
- `GET /api/v1/qr-codes/{qrId}/image`: Get QR code image as base64 string
- `POST /api/v1/qr-codes/{qrId}/pay`: Pay using a QR code
- `POST /api/v1/qr-codes/{qrId}/deactivate`: Deactivate a QR code

## Testing

PayFlow Lite includes a comprehensive test suite to ensure functionality and reliability:

### Unit Tests

- Service layer tests for core business logic
- Repository tests with H2 in-memory database
- Security tests for JWT authentication

### Integration Tests

- Controller tests with MockMvc
- End-to-end API tests
- Authentication flow tests

### Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage report
mvn test jacoco:report

# Run a specific test class
mvn test -Dtest=UserServiceTest
```

A coverage report will be generated at `target/site/jacoco/index.html`.

### Convenience Scripts

- `run-tests.sh` (Linux/macOS): Runs tests and opens coverage report
- `run-tests.bat` (Windows): Runs tests and opens coverage report

## Getting Started

### Prerequisites

- Java 17 or later
- Maven 3.6+

### Running the application

```bash
# Clone the repository
git clone <repository-url>
cd payflow-server

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on port 8080, and you can access the API at `http://localhost:8080/api/v1/`.

## API Documentation

Once the application is running, you can access the Swagger UI documentation at:

```
http://localhost:8080/api/v1/swagger-ui/index.html
```

## Database Configuration

By default, the application uses an in-memory H2 database. To use PostgreSQL:

1. Uncomment the PostgreSQL configuration in `application.properties`
2. Update the database connection details

## Security

The API uses JWT (JSON Web Token) for authentication. All endpoints except `/auth/**` require an Authorization header with a valid JWT token.

```
Authorization: Bearer <your-jwt-token>
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

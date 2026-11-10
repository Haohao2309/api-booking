# Backend architecture

## Package structure

```text
src/main/java/com/example/api_booking/
├── controller/        # All REST endpoints
├── dto/               # All request/response DTOs
├── entity/            # All JPA entities
├── service/           # All use cases and transactions
├── repository/        # All Spring Data repositories
│   └── AppUserRepository.java
├── common/
│   ├── api/           # Response envelope and pagination
│   └── exception/     # Global exceptions and error mapping
└── config/            # Security, CORS and application configuration

src/main/resources/
└── db/migration/      # Versioned Flyway migrations: V1__name.sql
```

## Rules

- `controller` only handles HTTP and DTO validation.
- `service` owns business rules and transactions.
- `repository` only queries and persists data.
- `entity` is never returned directly from an API.
- Every database schema change gets a new Flyway migration.
- Do not edit an already-applied migration; create the next version.

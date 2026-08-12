# ShopTourBoot (Voyage API)

Spring Boot **4.1** / Java **21** / Kotlin backend for [ShopTourr](https://github.com/Densodim/ShopTourr).

## Stack

- Web MVC + Validation + Problem Details
- Security + OAuth2 Resource Server (first-party HS256 JWT)
- JPA + Flyway + PostgreSQL (`flyway-database-postgresql`)
- Redis, Actuator, Mail
- springdoc OpenAPI
- Testcontainers (Postgres 16 + Redis 7) when Docker is available

## Run (local)

```bash
# Postgres + Redis must be up (or use TestVoyageApplication with Docker)
export VOYAGE_JWT_SECRET='dev-only-change-me-to-a-32byte-secret!!'
export VOYAGE_CORS_ORIGINS='http://localhost:5173'   # optional, this is the default
./gradlew bootRun
```

Smoke: `GET /api/_ping`, Actuator: `GET /actuator/health`, Swagger: `/swagger-ui.html`.

## Tests

```bash
./gradlew test
```

`VoyageApplicationTests` is skipped automatically when Docker is unavailable.

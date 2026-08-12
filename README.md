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

## Run (containers, no local services)

Testcontainers supplies Postgres 16 and Redis 7, thrown away on exit:

```bash
./gradlew bootTestRun
```

## Container runtime

With **colima** (instead of Docker Desktop), Testcontainers does not pick up the docker context
on its own — export these first, or `bootTestRun` will not start and container-backed tests will
be silently skipped:

```bash
colima start
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

`DOCKER_HOST` lets Testcontainers find the daemon; the socket override lets the Ryuk cleanup
container mount the socket from inside the VM. `colima stop` when finished.

## Tests

```bash
./gradlew test
```

`VoyageApplicationTests` is skipped automatically when Docker is unavailable — a green build is
therefore not proof that the integration layer ran. Check the skip count:

```bash
grep -o 'tests="[0-9]*" skipped="[0-9]*"' build/test-results/test/*.xml
```

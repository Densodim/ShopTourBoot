# ShopTourBoot (Voyage API)

Spring Boot **4.1** / Java **21** / Kotlin backend for [ShopTourr](https://github.com/Densodim/ShopTourr).

## Stack

- Web MVC + Validation + Problem Details
- Security + OAuth2 Resource Server (first-party HS256 JWT)
- Federated Google / Apple login: `POST /api/auth/oauth` verifies provider ID tokens via JWKS (`VOYAGE_GOOGLE_CLIENT_IDS`, `VOYAGE_APPLE_AUDIENCES`)
- JPA + Flyway + PostgreSQL (`flyway-database-postgresql`)
- Redis, Actuator, Mail
- springdoc OpenAPI
- Testcontainers (Postgres 16 + Redis 7) when Docker is available

## Run (usual: containers, no local services)

```bash
./scripts/run.sh
```

The script starts **colima** if needed, exports the Docker socket Testcontainers requires, then
runs `./gradlew bootTestRun`. App: `http://localhost:8080`. Stop with Ctrl+C (containers are
thrown away).

Equivalent by hand:

```bash
colima start
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export VOYAGE_JWT_SECRET='dev-only-change-me-to-a-32byte-secret!!'
./gradlew bootTestRun
```

Smoke: `GET /api/_ping`, Actuator: `GET /actuator/health`, Swagger: `/swagger-ui.html`.

## Run (local Postgres + Redis)

Needs Postgres and Redis already on localhost (`voyage/voyage@localhost:5432/voyage`, Redis `6379`):

```bash
export VOYAGE_JWT_SECRET='dev-only-change-me-to-a-32byte-secret!!'
./gradlew bootRun
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

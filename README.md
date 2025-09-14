# Brokerage OMS - Quick Start

[![Coverage](https://codecov.io/gh/OWNER/REPO/branch/main/graph/badge.svg)](https://app.codecov.io/gh/OWNER/REPO)


This project exposes a small Orders API. For ready-to-run HTTP requests, import the Postman collection:
`docs/postman/OMS.postman_collection.json`

### Notes

- Configure Postman variables `baseUrl` (default `http://localhost:8080`), `username`, `password`.
- For `GET /api/v1/orders`, required query params: `customerId`, `from`, `to` (ISO-8601, e.g., `2025-01-01T00:00:00Z`).
- Sorting: use `sort=property[,ASC|DESC]`.  
  Allowed properties: `createdAt` (alias `created_at`), `price`, `size`, `status`, `assetName` (alias `asset_name`).  
  Default: `createdAt,DESC`.
- Actuator health endpoint (`/actuator/health`) is public.

---

## Build

You need **JDK 21** and the **Gradle Wrapper**.

**Windows (from project root):**

```powershell
gradlew.bat clean test bootJar
```

**macOS/Linux (from project root):**

```bash
./gradlew clean test bootJar
```

The application jar will be at: _**build/libs/oms-0.0.1-SNAPSHOT.jar**_

---

## Run locally (without Docker)

Set the required security environment variables, then run the app.

**Windows PowerShell:**

```powershell
$env:APPLICATION_SECURITY_ADMIN_USER="admin"; `
$env:APPLICATION_SECURITY_ADMIN_PASSWORD="admin"; `
$env:APPLICATION_SECURITY_JWT_SECRET="please-change-me-32-bytes-or-more"; `
./gradlew.bat bootRun
```

**macOS/Linux:**

```bash
APPLICATION_SECURITY_ADMIN_USER=admin APPLICATION_SECURITY_ADMIN_PASSWORD=admin APPLICATION_SECURITY_JWT_SECRET=please-change-me-32-bytes-or-more ./gradlew bootRun
```

After startup, check health:

```
http://localhost:8080/actuator/health
```

---

## Run with Docker

**Build an image:**

```bash
docker build -t oms:latest .
```

**Run the container:**

```bash
docker run --rm -p 8080:8080   -e APPLICATION_SECURITY_ADMIN_USER=admin   -e APPLICATION_SECURITY_ADMIN_PASSWORD=admin   -e APPLICATION_SECURITY_JWT_SECRET=please-change-me-32-bytes-or-more   oms:latest
```

**Or with Docker Compose (recommended for local dev):**

```bash
docker compose up --build
```

---

## Environment variables

Supported (mapped by Spring Boot automatically):

```
APPLICATION_SECURITY_ADMIN_USER
APPLICATION_SECURITY_ADMIN_PASSWORD
APPLICATION_SECURITY_JWT_SECRET
APPLICATION_SECURITY_JWT_TTL_SECONDS (default 3600)
SPRING_PROFILES_ACTIVE
```

## Environment variables if you want to use with IntelliJ

```
application.security.admin-user=admin
application.security.admin-password=admin
application.security.jwt-secret=please-change-me-32-bytes-or-more
application.security.jwt-ttl-seconds=3600(default)
spring.profiles.active=local
```
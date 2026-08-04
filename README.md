# Books API

> A production-grade REST API built with **Spring Boot 3.3** and **Java 17**, strictly following the [Zalando RESTful API Guidelines](https://opensource.zalando.com/restful-api-guidelines/).

Built as a learning project to demonstrate real-world API design, security hardening, CI/CD, and cloud infrastructure — every concept maps directly to industry practices.

---

## Table of Contents

- [Security Concepts](#security-concepts)
- [API Endpoints](#api-endpoints)
- [Project Structure](#project-structure)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Quick Start (Local Dev)](#quick-start-local-dev)
- [Environment Variables](#environment-variables)
- [Running Tests](#running-tests)
- [Security Checks](#security-checks)
  - [SAST — SpotBugs + FindSecBugs](#sast--spotbugs--findsecbugs)
  - [SCA — OWASP Dependency-Check](#sca--owasp-dependency-check)
  - [DAST — OWASP ZAP](#dast--owasp-zap)
- [CI/CD Pipeline](#cicd-pipeline)
- [Infrastructure (Terraform)](#infrastructure-terraform)
- [Testing with Insomnia](#testing-with-insomnia)
- [License](#license)

---

## Security Concepts

Security concepts demonstrated in this project:

**Security Design Principles**
- Least privilege — `ROLE_USER` can create/update; `ROLE_ADMIN` required to delete
- Defense in depth — 8 independent security layers (validation → auth → authz → rate limit → headers → deserialization → audit → error handling)
- Fail securely — all unhandled errors return Problem JSON with no stack trace
- Compartmentalization — security package isolated; JWT filter has no business logic

**Authentication & Authorization**
- JWT access tokens (30 min expiry, HMAC-SHA256)
- Refresh token rotation — invalidated on every use to prevent replay attacks
- BCrypt password hashing (cost factor 10)
- Role-based access control (`ROLE_USER`, `ROLE_ADMIN`)

**API Security**
- Cursor-based pagination — opaque Base64 tokens, never exposes internal IDs
- ETag + `If-Match` optimistic locking on PATCH
- Rate limiting — 5 req/min on login, 30 req/min on list (Bucket4j)
- `X-Flow-ID` propagated through all requests for distributed tracing

**Java Process Security**
- Input validation and sanitization on all DTOs (`@Valid` + Bean Validation)
- Log injection prevention — CR/LF stripped from all user-supplied values before logging
- Jackson deserialization hardening — default typing disabled, scalar coercion restricted
- Content Security Policy (CSP), CORS, HSTS, `X-Frame-Options`, `X-XSS-Protection`, `Referrer-Policy`
- Problem JSON (RFC 9457) — no stack traces, no internal paths ever exposed

**Security Testing**
- SAST: SpotBugs + FindSecBugs (bytecode analysis)
- SCA: OWASP Dependency-Check (known CVE scanning on all dependencies)
- DAST: OWASP ZAP API scan against the live OpenAPI spec

**Symmetric & Asymmetric Concepts**
- JWT signed with HMAC-SHA256 (symmetric — shared secret)
- BCrypt (one-way hashing for passwords)
- Refresh tokens — UUID stored as opaque tokens, rotated on use

---

## API Endpoints

### Auth

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/register` | Public | Register new user — bcrypt hashed |
| `POST` | `/auth/login` | Public | Returns JWT + refresh token |
| `POST` | `/auth/refresh` | Public | Rotate refresh token, get new access token |

### Books

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/books` | Bearer | Paginated list — cursor + `?q=` search |
| `GET` | `/books/{id}` | Bearer | Single book with `ETag` header |
| `POST` | `/books` | USER / ADMIN | Create book — returns `201 + Location` |
| `PATCH` | `/books/{id}` | USER / ADMIN | Partial update — `application/merge-patch+json`, `If-Match` required |
| `DELETE` | `/books/{id}` | ADMIN only | Returns `204` |

### Error shape (RFC 9457 Problem JSON)

All error responses use `Content-Type: application/problem+json`:

```json
{
  "type": "/problems/book-not-found",
  "title": "Book Not Found",
  "status": 404,
  "detail": "No book with id 99 exists",
  "instance": "/books/99"
}
```

### Pagination shape (Zalando)

`GET /books` always returns a JSON object, never a raw array:

```json
{
  "items": [...],
  "next": "MTI="
}
```

Pass `?cursor=MTI=` to get the next page. `next` is `null` when there are no more results.

---

## Project Structure

```
src/main/java/com/example/booksapi/
├── controller/          BookController, AuthController
├── service/             BookService, AuthService, SecurityAuditService
├── repository/          BookRepository, UserRepository, RefreshTokenRepository
├── model/               Book, User, RefreshToken, Role
├── dto/                 BookRequest, BookResponse, AuthRequest, AuthResponse,
│                        RegisterRequest, PaginatedResponse, ApiProblem
├── security/            JwtFilter, JwtService, SecurityConfig, UserDetailsConfig
├── exception/           GlobalExceptionHandler, BookNotFoundException,
│                        ConflictException, PreconditionFailedException,
│                        UnauthorizedException
└── config/              CorsConfig, RateLimitConfig, RateLimitInterceptor,
                         JacksonSecurityConfig, OpenApiConfig, WebMvcConfig

src/test/java/com/example/booksapi/
├── controller/          AuthControllerIntegrationTest, BookControllerIntegrationTest
└── service/             BookServiceTest
```

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Spring Boot 3.3.5 | Application framework |
| Java 17 | Language |
| Spring Security | Authentication, authorization, security headers |
| Spring Data JPA + Hibernate | Data access layer |
| H2 | In-memory database (dev profile) |
| PostgreSQL | Production database |
| JJWT 0.12.5 | JWT creation and validation |
| Bucket4j 7.6.0 | In-memory rate limiting |
| springdoc-openapi 2.5.0 | OpenAPI 3 spec + Swagger UI |
| Lombok | Boilerplate reduction |
| SpotBugs + FindSecBugs | SAST |
| OWASP Dependency-Check | SCA — CVE scanning |
| GitHub Actions | CI/CD |
| Terraform + AWS | Infrastructure (EC2, RDS, ALB, SSM, S3) |

---

## Prerequisites

| Tool | Version | Link |
|---|---|---|
| JDK | 17 | [Download](https://adoptium.net/) |
| Maven | 3.9+ | [Download](https://maven.apache.org/) |
| Docker | Latest | [Download](https://www.docker.com/) — optional, for DAST |
| Insomnia | Latest | [Download](https://insomnia.rest/) — optional, for manual testing |
| Terraform | 1.8+ | [Download](https://developer.hashicorp.com/terraform/install) — optional, for AWS deploy |

---

## Quick Start (Local Dev)

The `dev` Spring profile uses H2 in-memory — no database setup needed.

**1. Clone the repo**

```bash
git clone https://github.com/mehedi-hasan-2426/books-api.git
cd books-api
```

**2. Set the required environment variable**

```bash
# Windows PowerShell
$env:JWT_SECRET = "dGhpcyBpcyBhIGRldi1vbmx5IHNlY3JldC1kb250LXVzZS1pbi1wcm9kdWN0aW9u"

# Linux / macOS
export JWT_SECRET="dGhpcyBpcyBhIGRldi1vbmx5IHNlY3JldC1kb250LXVzZS1pbi1wcm9kdWN0aW9u"
```

> The dev profile already has a fallback secret in `application-dev.yml`. Production **must** override this via `JWT_SECRET`.

**3. Run**

```bash
mvn spring-boot:run
```

The API is now at `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`
OpenAPI spec: `http://localhost:8080/api-docs`
H2 console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:booksdb`)

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | **Yes** | Base64-encoded secret, min 32 bytes. Never commit a real value. |
| `DATABASE_URL` | Prod only | e.g. `jdbc:postgresql://host:5432/booksdb` |
| `DATABASE_USERNAME` | Prod only | PostgreSQL username |
| `DATABASE_PASSWORD` | Prod only | PostgreSQL password |
| `PORT` | Optional | Server port (default `8080`) |

Copy `.env.example` as a starting point:

```bash
cp .env.example .env
```

---

## Running Tests

```bash
# Unit + integration tests (uses H2 in-memory, no external services needed)
mvn test

# Full verify (includes compile, test, package)
mvn verify
```

Integration tests cover every endpoint including:
- All happy paths
- All error cases returning Problem JSON
- ETag / `If-Match` behaviour
- Authentication and authorization failures

---

## Security Checks

### SAST — SpotBugs + FindSecBugs

Analyses compiled bytecode for security vulnerabilities (SQL injection patterns, insecure random, XXE, etc.) without running the application.

```bash
mvn spotbugs:check
```

The FindSecBugs plugin is configured in `pom.xml`. Reports are written to `target/spotbugsXml.xml`.

### SCA — OWASP Dependency-Check

Scans all direct and transitive dependencies against the NVD (National Vulnerability Database) and fails the build if any CVE with CVSS ≥ 7 is found.

```bash
mvn org.owasp:dependency-check-maven:check
```

> The first run downloads NVD data feeds and may take several minutes. Subsequent runs use the cached database.

Report: `target/dependency-check-report.html`

### DAST — OWASP ZAP

Runs a live API scan against the running application using the OpenAPI spec to auto-discover all endpoints.

```bash
# Start the app first
mvn spring-boot:run &

# Run ZAP API scan (requires Docker)
docker run --rm -v $(pwd)/zap-reports:/zap/wrk \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-api-scan.py \
  -t http://host.docker.internal:8080/api-docs \
  -f openapi \
  -r zap-report.html
```

Report is written to `./zap-reports/zap-report.html`.

---

## CI/CD Pipeline

GitHub Actions runs on every push and pull request:

```
push / PR
    │
    ├── Build & Test (mvn verify)
    │       └── Upload surefire test results
    │
    ├── SAST — SpotBugs + FindSecBugs (mvn spotbugs:check)
    │       └── Upload SpotBugs report
    │
    └── SCA — OWASP Dependency-Check
            └── Upload HTML report
```

On merge to `main`, the CD workflow builds the Docker image, pushes it to ECR, and triggers a rolling deployment to EC2 via AWS Systems Manager.

Workflow files: [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | [`.github/workflows/cd.yml`](.github/workflows/cd.yml)

**GitHub Secrets required for CI:**

| Secret | Description |
|---|---|
| `JWT_SECRET_CI` | Test JWT secret (any 32+ byte base64 string) |
| `AWS_ACCESS_KEY_ID` | AWS IAM key for CD deployments |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM secret for CD deployments |
| `ECR_REPOSITORY` | ECR repository URI |
| `EC2_INSTANCE_ID` | Target EC2 instance ID |

---

## Infrastructure (Terraform)

The `terraform/` directory provisions the full AWS stack:

| Resource | Purpose |
|---|---|
| EC2 (t3.micro) | Application server |
| RDS PostgreSQL (db.t3.micro) | Production database |
| Application Load Balancer | HTTPS termination, health checks |
| Security Groups | ALB → EC2 → RDS port isolation |
| SSM Parameter Store | Secrets (DB password, JWT secret) — never in code |
| S3 | Terraform remote state |

**Deploy:**

```bash
cd terraform/

# First time only — create S3 state bucket and DynamoDB lock table
terraform init

# Preview changes
terraform plan -var="app_name=books-api" -var="aws_region=eu-west-1"

# Apply
terraform apply -var="app_name=books-api" -var="aws_region=eu-west-1"
```

> Terraform state is stored remotely in S3 with DynamoDB locking — safe for team use.

**Destroy when done (avoids AWS charges):**

```bash
terraform destroy
```

---

## Testing with Insomnia

Import [`insomnia-collection.json`](../insomnia-collection.json) into Insomnia to get a pre-built collection covering every endpoint.

**Typical flow:**

1. `POST /auth/register` — create a user
2. `POST /auth/login` — copy the `access_token` from the response
3. Set `Bearer <token>` as the Authorization header on subsequent requests
4. `POST /books` — create a book
5. `GET /books/{id}` — copy the `ETag` value from the response header
6. `PATCH /books/{id}` — set `If-Match: <etag>` header and send a merge-patch body
7. `POST /auth/refresh` — exchange the `refresh_token` for a new access token

All error responses will be `application/problem+json` — Insomnia will display the structured error body.

---

## License

This project is licensed under the [MIT License](LICENSE).

# Java Books API — Build Prompt (Zalando Guidelines)

Paste the prompt below into a new conversation when ready to build.

---

Build a Spring Boot 3.3 REST API called **Books API** following the Zalando RESTful API Guidelines strictly. Java 17, Maven, H2 for dev, PostgreSQL for production.

**Domain:** Books with fields: `id` (Long), `title` (String), `author` (String), `genre` (String nullable), `created_at` (UTC datetime), `modified_at` (UTC datetime).

**Endpoints following Zalando rules:**
- `GET /books` — paginated list, cursor-based pagination, returns `{"items": [...], "next": "cursor"}`. Query params: `limit` (default 10), `cursor` (opaque), `q` (search by title/author)
- `GET /books/{id}` — single book, returns `ETag` header
- `POST /books` — creates book, returns `201` with `Location: /books/{id}` header
- `PATCH /books/{id}` — partial update using JSON Merge Patch (`application/merge-patch+json`), `If-Match` header required, returns `412` on conflict
- `DELETE /books/{id}` — returns `204`

**Auth:**
- `POST /auth/register` — bcrypt hashed passwords
- `POST /auth/login` — returns JWT (30 min expiry) + refresh token (7 days)
- `POST /auth/refresh` — exchange refresh token for new access token
- Protected endpoints require `Authorization: Bearer <token>`

**Error responses must use Problem JSON (RFC 9457):**
```json
{"type": "/problems/book-not-found", "title": "Book Not Found", "status": 404, "detail": "No book with id 5 exists"}
```

**Zalando rules to enforce:**
- All JSON property names `snake_case`
- All datetime fields suffixed `_at`, format `date-time` UTC with `Z`
- `GET /books` always returns JSON object never array
- `429` responses include `Retry-After` and `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset` headers
- Rate limit: 5 req/min on login, 30 req/min on list books
- `X-Flow-ID` header propagated through all requests for tracing
- CORS configured — only allow specified origins
- No stack traces ever exposed in responses

**Project structure (enterprise layout):**
```
src/main/java/com/example/booksapi/
  controller/   — BookController, AuthController
  service/      — BookService, AuthService
  repository/   — BookRepository (JPA)
  model/        — Book, User (JPA entities)
  dto/          — BookRequest, BookResponse, ProblemDetail (Pydantic equivalent)
  security/     — JwtFilter, SecurityConfig
  exception/    — GlobalExceptionHandler (@ControllerAdvice)
  config/       — RateLimitConfig, CorsConfig
```

**Tests required:**
- Unit tests for BookService with Mockito
- Integration tests for every endpoint using MockMvc
- Test that Problem JSON is returned on every error case

**OpenAPI spec:**
- Generate via springdoc-openapi available at `/api-docs`
- Include `x-api-id`, `x-audience: external-public`, semantic version `1.0.0`

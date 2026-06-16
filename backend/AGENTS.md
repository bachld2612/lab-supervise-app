# Repository Guidelines

## Project Structure & Module Organization

This repository is the Spring Boot backend for Lab Supervise. Main code lives under `src/main/java/com/bachld/backend`.

- `controller/`: REST endpoints and WebSocket controllers.
- `service/`: business logic and integration code, including remote command and VNC session handling.
- `repository/`: Spring Data JPA repositories.
- `model/`: JPA entities.
- `dto/request` and `dto/response`: API payload types.
- `config/`, `filter/`, `exception/`, `util/`, `websocket/`: infrastructure and shared helpers.

Runtime configuration and seed data are in `src/main/resources`. Download templates are stored in `src/main/resources/templates/download`. Tests live in `src/test/java`.

## Build, Test, and Development Commands

- `./mvnw spring-boot:run` or `mvnw.cmd spring-boot:run`: run the API on port `8080`.
- `./mvnw test` or `mvnw.cmd test`: run the JUnit/Spring test suite.
- `./mvnw clean package` or `mvnw.cmd clean package`: compile, test, and build the JAR in `target/`.
- `docker compose up mysql`: start local MySQL 8.0 using `docker-compose.yml`.
- `docker compose up --build`: run MySQL plus the backend container when validating Docker setup.

The app expects MySQL database `datn` and environment variables from `.env.example`, especially `JWT`, `APP_RSA_PRIVATE_KEY`, and `VNC_ENCRYPTION_KEY`.

## Coding Style & Naming Conventions

Use Java 17 and existing Spring conventions. Keep 4-space indentation. Name classes by role, for example `StudentController`, `StudentService`, `StudentRepository`, `StudentCreateRequest`, and `StudentResponse`. Prefer constructor injection or existing local patterns; Lombok is available. Keep controllers thin and place authorization, validation, and persistence logic in services.

## Testing Guidelines

Tests use JUnit 5 with `spring-boot-starter-test` and `spring-security-test`. Add tests under matching package paths in `src/test/java`. Use `*Tests` for Spring context or integration-style tests and `*Test` for focused unit tests. Run `mvnw.cmd test` before submitting changes on Windows.

## Commit & Pull Request Guidelines

Recent history uses Conventional Commit prefixes such as `feat:`, `fix:`, and `chore:`. Keep subjects imperative and scoped to one change, for example `fix: validate vnc session token expiry`.

Pull requests should include a short summary, test evidence, linked issue or task, and API/WebSocket behavior notes when relevant. Include screenshots only for Swagger output or externally visible API behavior. Never commit `.env`, generated `target/` files, or real secrets.

# Repository Guidelines

## Project Structure & Module Organization
This repository is a Java 17 Maven desktop client for TLU Lab Monitoring. Main code lives under `src/main/java/com/bachld`.

- `LabMonitorApp.java` is the application entry point.
- `client/` contains REST API clients.
- `config/` contains application configuration, REST client setup, JWT handling, and error handling.
- `model/request` and `model/response` contain DTOs.
- `service/` contains business logic, tracking, WebSocket, VNC, auth, and session services.
- `ui/` contains Java Swing frames, panels, cards, and dialogs.
- `util/` contains validation and helper utilities.
- `src/main/resources/` contains `application.properties`, `logback.xml`, images, icons, and VNC binaries.
- Tests mirror the package layout in `src/test/java/com/bachld`.

Generated outputs belong in `target/`; runtime logs are written under `logs/`.

## Build, Test, and Development Commands

```bash
mvn clean compile
```
Compiles the application.

```bash
mvn test
```
Runs all JUnit 5 tests.

```bash
mvn test -Dtest=EmailValidatorTest
```
Runs one test class.

```bash
mvn clean package
```
Builds the executable shaded JAR.

```bash
java -jar target/lab-supervise-desktop-1.0.0-SNAPSHOT.jar
```
Runs the packaged desktop client.

```bash
mvn exec:java -Dexec.mainClass="com.bachld.LabMonitorApp"
```
Runs the app directly from Maven.

## Coding Style & Naming Conventions
Use Java 17 and UTF-8. Follow the existing package structure and keep class names in PascalCase, methods and fields in camelCase, and constants in UPPER_SNAKE_CASE. Use 4-space indentation. Keep Swing UI updates on the EDT; use `SwingUtilities.invokeLater` when callbacks update UI. Prefer existing singleton wiring from `LabMonitorApp` over re-instantiating shared services such as `AppConfig`, `RestClient`, `TokenManager`, and `SessionManager`.

## Testing Guidelines
Tests use JUnit 5; QuickTheories is available for property-based tests. Name test classes `*Test.java` and place them in the matching package under `src/test/java`. Add or update tests for validators, API clients, service callbacks, session/token behavior, and UI components when changing those areas. Run `mvn test` before submitting changes.

## Commit & Pull Request Guidelines
Recent commits use short conventional prefixes such as `feat:`, `fix:`, and `chore:`. Keep subjects imperative and scoped, for example `fix: handle expired jwt during login`. Pull requests should include a concise description, test evidence, linked issue when applicable, and screenshots or screen recordings for visible Swing UI changes.

## Security & Configuration Tips
Do not commit secrets. Keep local values in `.env`; document required keys in `.env.example`. Application endpoints are configured in `src/main/resources/application.properties`. Be careful when modifying bundled files in `src/main/resources/vnc`, since they are Windows runtime binaries.

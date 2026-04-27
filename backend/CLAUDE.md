# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Layout

This is a monorepo with three independent modules, each with its own build system:

```
lab_supervise/
├── backend/      # Spring Boot REST API + WebSocket server (Java 17, Maven)
├── frontend/     # React + TypeScript web dashboard (Vite, Yarn)
└── desktop/      # Java Swing desktop client for lab PCs (Java 17, Maven)
```

## Commands

### Backend (`/backend`)
```bash
mvn spring-boot:run          # Start dev server on port 8080
mvn clean package            # Build JAR
mvn test                     # Run tests
```

Requires MySQL at `localhost:3306`, database `datn`, user `root`/`root`. Set the `JWT` environment variable for JWT signing.

### Frontend (`/frontend`)
```bash
yarn install     # Install dependencies
yarn start       # Dev server on port 3000
yarn build       # Production build
yarn lint        # ESLint check
yarn lint:fix    # ESLint auto-fix
yarn prettier    # Prettier format
```

API URL is configured via `VITE_APP_API_URL` in `.env` (defaults to `http://localhost:8080/`).

### Desktop (`/desktop`)
```bash
mvn clean package   # Build fat JAR via maven-shade-plugin
mvn test            # Run tests
java -jar target/lab-supervise-desktop-1.0.0-SNAPSHOT-shaded.jar  # Run
```

## Architecture

### System Overview

Three-tier distributed system for real-time lab monitoring:

1. **Desktop app** runs on each lab PC, collects system info (CPU/RAM via OSHI, foreground window via JNA Windows API, active processes), and streams it to the backend over WebSocket (STOMP).
2. **Backend** exposes a REST API and a WebSocket endpoint. It manages users, classes, schedules, and PC assignments, and relays tracking data to connected clients.
3. **Frontend** is a multi-role dashboard (Admin / Teacher / IT-Center) that displays class management, scheduling, and real-time PC activity.

### Backend Package Structure (`com.bachld.backend`)

- `controller/` — REST controllers, one per domain (Auth, User, Teacher, Student, Class, PersonalComputer, Schedule, ManageClass, Semester, Subject, Department, Section, Major) plus `TrackingWebSocketController` for STOMP messages.
- `service/` — Business logic; mirrors controller domains.
- `repository/` — Spring Data JPA repositories.
- `model/entity/` — JPA entities.
- `model/dto/` — Request/response DTOs.
- `config/` — Spring Security (`SecurityConfig`), WebSocket (`WebSocketConfig`), JWT filter (`JwtAuthFilter`), CORS (all origins allowed).
- `utils/` — `JwtService`, `TokenManager`, helpers.

**Authentication:** JWT tokens (15-minute expiry). Backend uses `JwtAuthFilter` to validate every request. WebSocket connections authenticate via Bearer token in the STOMP `CONNECT` frame.

**WebSocket:**
- SockJS + STOMP endpoint: `/ws`
- Desktop publishes to: `/app/track`
- Subscribers receive on: `/topic/*`

**Default admin account** is seeded on startup: `vpk@tlu.edu.vn` / `123456`.

**OpenAPI/Swagger** available at `/swagger-ui/**` and `/v3/api-docs/**` when running.

### Frontend Architecture (`/frontend/src`)

- `api/` — Axios-based API clients, one module per domain.
- `contexts/` — `JwtContext` (auth state, token storage in localStorage), `ConfigContext`.
- `routes/` — React Router config; `AuthGuard` / `GuestGuard` protect pages.
- `pages/` + `sections/` — Page components and their form sections (add/edit/detail).
- `components/` — Shared UI components.
- `utils/axios.ts` — Axios instance with `Authorization: Bearer` interceptor.

### Desktop Architecture (`/desktop/src/main/java/com/bachld`)

- `LabMonitorApp` — Main entry; sets up Swing UI.
- `LoginFrame` — Login screen; calls backend `/api/auth/v1/login`.
- `service/AuthService` — REST auth via `RestClient` (singleton `RestTemplate`).
- `service/PersonalComputerService` — Collects and POSTs PC hardware info.
- `service/WindowsTrackingService` — Uses JNA (`User32`) to get foreground window title.
- `service/WebSocketService` — STOMP client; sends `PCInfoPayload` periodically.
- `singleton/TokenManager` — Stores JWT after login.
- `singleton/SessionManager` — Holds session state.
- `config/AppConfig` — Singleton with server URL and app metadata.

## Key Domain Model Relationships

```
User (role: ADMIN | TEACHER | IT_CENTER)
  └── Teacher or Student profile

Classes ──< ClassSchedule >── Semester + Subject + Teacher
Student ──< StudentClass >── Classes
StudentClass ──── PersonalComputer   (which PC a student is at)

PersonalComputer (device_id, ip, mac, status)
  └── real-time tracking data via WebSocket
```

Hibernate `ddl-auto: update` — schema evolves automatically; no migration tool in use.

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

Requires MySQL at `localhost:3306`, database `datn`, user `root`/`root`.

Required environment variables (see `.env.example`):
- `JWT` — JWT signing secret
- `VNC_PASSWORD` — VNC password for remote desktop relay
- `VEYON_RSA_PRIVATE_KEY` — RSA private key for Veyon integration
- `VEYON_SECRET` — Veyon API secret

### Frontend (`/frontend`)
```bash
yarn install     # Install dependencies
yarn start       # Dev server on port 3000
yarn build       # Production build
yarn lint        # ESLint check
yarn lint:fix    # ESLint auto-fix
yarn prettier    # Prettier format
```

API base URL configured via `VITE_APP_API_URL` in `.env` (defaults to `http://localhost:8080/`).

### Desktop (`/desktop`)
```bash
mvn clean package   # Build fat JAR via maven-shade-plugin
mvn test            # Run tests
java -jar target/lab-supervise-desktop-1.0.0-SNAPSHOT-shaded.jar  # Run
```

Required env var: `VNC_PASSWORD` (also in `application.properties`).

## Architecture

### System Overview

Three-tier distributed system for real-time lab monitoring:

1. **Desktop app** runs on each lab PC, collects system info (CPU/RAM via OSHI, foreground window via JNA Windows API, active processes), and streams it to the backend over WebSocket (STOMP). Also manages a local UltraVNC server for remote-desktop access.
2. **Backend** exposes a REST API and two WebSocket endpoints. It manages users, classes, schedules, and PC assignments, relays tracking data to connected clients, and proxies VNC connections between teacher browser and student desktop.
3. **Frontend** is a multi-role dashboard (Admin / Teacher / IT-Center) that displays class management, scheduling, real-time PC activity, and a live VNC remote-desktop viewer (noVNC).

### Backend Package Structure (`com.bachld.backend`)

- `controller/` — REST controllers per domain (Auth, User, Teacher, Student, Class, PersonalComputer, Schedule, ManageClass, Semester, Subject, Department, Section, Major, Veyon) plus `TrackingWebSocketController` for STOMP and `VncController` for VNC sessions.
- `service/` — Business logic; mirrors controller domains. `VncSessionService` manages one-time session tokens (30 s expiry).
- `repository/` — Spring Data JPA repositories.
- `model/entity/` — JPA entities.
- `model/dto/` — Request/response DTOs.
- `config/` — `SecurityConfig`, `WebSocketConfig` (STOMP), `VncWebSocketConfig` (raw binary WebSocket), `JwtAuthFilter`, CORS (all origins allowed).
- `utils/` — `JwtService`, `TokenManager`, helpers.
- `websocket/` — `VncWebSocketHandler`: binary WebSocket handler that proxies the VNC protocol between a browser WebSocket and a TCP socket on port 5900.

**Authentication:** JWT tokens (15-minute expiry). `JwtAuthFilter` validates every HTTP request. STOMP connections authenticate via Bearer token in the `CONNECT` frame.

**WebSocket endpoints:**
- `/ws` — SockJS + STOMP; desktop publishes to `/app/track`; subscribers receive on `/topic/*`
- `/vnc-relay` — Raw binary WebSocket; proxies VNC traffic to the student PC's UltraVNC server (port 5900). Requires a one-time session token issued by `POST /api/vnc/v1/session/{classId}/{studentUserId}`.

**VNC relay flow:**
1. Teacher requests a session token from `/api/vnc/v1/session/{classId}/{studentUserId}`.
2. Backend validates the teacher's authorization for that class and returns a short-lived token.
3. Frontend opens a WebSocket to `/vnc-relay?token=<token>`.
4. `VncWebSocketHandler` looks up the student PC's IP, opens a TCP socket to port 5900, handles VNC handshake/auth internally (DES encryption), and then relays raw frames between both sides.

**Default admin account** seeded on startup: `vpk@tlu.edu.vn` / `123456`.

**OpenAPI/Swagger** at `/swagger-ui/**` and `/v3/api-docs/**` when running.

**STOMP authorization:** `WebSocketConfig` channel interceptor enforces that a teacher can only subscribe to `/topic/class/{classId}` for classes they own, and users can only subscribe to their own `/topic/user/{userId}/**` topics.

### Frontend Architecture (`/frontend/src`)

- `api/` — Axios-based API clients per domain, including `vnc.ts` for session creation.
- `contexts/` — `JwtContext` (auth state, token in localStorage), `ConfigContext`.
- `routes/` — React Router config; `AuthGuard` / `GuestGuard` protect pages.
- `pages/` + `sections/` — Page components and form sections (add/edit/detail).
- `components/VncViewer.tsx` — noVNC-based component that connects to `/vnc-relay` and renders the student's desktop.
- `types/novnc.d.ts` — TypeScript declarations for the noVNC library.
- `utils/axios.ts` — Axios instance with `Authorization: Bearer` interceptor.

### Desktop Architecture (`/desktop/src/main/java/com/bachld`)

- `LabMonitorApp` — Main entry; sets up Swing UI (`MainFrame`).
- `LoginFrame` — Login screen; calls `/api/auth/v1/login`.
- `service/AuthService` — REST auth via singleton `RestTemplate`.
- `service/PersonalComputerService` — Collects and POSTs PC hardware info.
- `service/WindowsTrackingService` — JNA (`User32`) to get foreground window title; `LinuxX11TrackingService` for Linux.
- `service/WebSocketService` — STOMP client; sends `PCInfoPayload` periodically.
- `service/VncService` — Extracts bundled UltraVNC binaries (`winvnc.exe` + DLLs from `resources/vnc/`) to a temp directory, writes the VNC password to the Windows Registry, and starts the VNC server process so the backend relay can connect to it.
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
  └── real-time tracking data via WebSocket (STOMP /topic/class/{classId})
  └── remote desktop via VNC relay (/vnc-relay)
```

Hibernate `ddl-auto: update` — schema evolves automatically; no migration tool in use.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
yarn start        # Dev server on port 3000
yarn build        # Production build (runs tsc first)
yarn build-stage  # Staging build using .env.qa
yarn lint         # ESLint check
yarn lint:fix     # ESLint auto-fix
yarn prettier     # Prettier format
yarn knip         # Detect unused exports/dependencies
```

No test runner is configured. TypeScript type-checking runs as part of `yarn build`.

## System Context

This is the web dashboard for a three-tier lab monitoring system:

- **Desktop** (Java Swing) — runs on each lab PC, streams CPU/RAM/active-window data to the backend over WebSocket (STOMP).
- **Backend** (Spring Boot, port 8080) — REST API + WebSocket relay. STOMP endpoint at `/ws`, desktop publishes to `/app/track`, subscribers receive on `/topic/*`.
- **Frontend** (this repo, port 3000) — multi-role dashboard for Admin, Teacher, and IT-Center users.

API URL is read from `VITE_APP_API_URL` in `.env` (defaults to `http://localhost:8080/`). Default seeded admin account: `vpk@tlu.edu.vn` / `123456`.

## Architecture

### Auth & State

`JWTContext` (`src/contexts/JWTContext.tsx`) owns all auth state. It stores the JWT in `localStorage`, validates expiry on app init, and auto-logouts on 401. `AuthGuard` / `GuestGuard` in `src/routes/` wrap protected route trees.

The Axios instance in `src/utils/axios.ts` auto-attaches `Authorization: Bearer <token>` via a request interceptor and redirects to `/maintenance/500` on 401.

### Routing

Two route files:
- `LoginRoutes` — public auth pages
- `MainRoutes` — all dashboard pages, wrapped in `AuthGuard` and lazy-loaded via `Loadable()` HOC

Role-specific menus are defined separately in `src/menu-items/` (admin, teacher, it-center). Adding a new page requires updating both the route file and the matching menu-items file.

### API Layer

`src/api/` contains one module per domain (e.g., `user.ts`, `teacher.ts`, `schedule.ts`). Each module exports typed async functions that call the Axios instance. When adding a new API call, follow this one-module-per-domain pattern.

### Page & Form Pattern

Pages under `src/pages/{domain}/` handle listing/table views. Add/edit/detail forms live in `src/sections/extra-pages/{domain}/` and are routed as child paths (`/user/add`, `/user/edit/:id`, `/user/detail/:id`).

### Real-Time Tracking

WebSocket integration uses SockJS + STOMP (`@stomp/stompjs`). The tracking page subscribes to `/topic/*` to receive live PC data relayed by the backend. Authenticate the STOMP `CONNECT` frame with the same JWT Bearer token used for REST calls.

### Key Libraries

| Purpose | Library |
|---|---|
| UI components | MUI v7 (`@mui/material`) |
| Forms | Formik + Yup |
| Tables | TanStack React Table |
| Server state | TanStack React Query + SWR |
| Routing | React Router v7 |
| WebSocket | `@stomp/stompjs` + `sockjs-client` |
| i18n | react-intl |

### Path Aliases

`@/` maps to `src/` (configured in both `vite.config.mts` and `tsconfig.json`). Use `@/` imports throughout.

# Repository Guidelines

## Project Structure & Module Organization

This is a React 19, TypeScript, and Vite frontend. Application code lives in `src/`, with entry points in `src/index.tsx` and `src/App.tsx`. API wrappers are in `src/api/`, reusable UI in `src/components/`, routes in `src/routes/`, page screens in `src/pages/`, layouts in `src/layout/`, hooks in `src/hooks/`, contexts in `src/contexts/`, and domain types in `src/types/`. Theme customization is under `src/themes/`; static assets are in `src/assets/` and `public/`. Build output goes to `dist/` and should not be edited directly.

## Build, Test, and Development Commands

Use the scripts in `package.json`:

- `yarn start` or `npm run start`: start the Vite dev server.
- `yarn build` or `npm run build`: run TypeScript checks, load `.env`, and build for production.
- `yarn build-stage` or `npm run build-stage`: build using `.env.qa`.
- `yarn preview` or `npm run preview`: serve the production build locally.
- `yarn lint` or `npm run lint`: run ESLint on `src/**/*.{js,jsx,ts,tsx}`.
- `yarn lint:fix` or `npm run lint:fix`: apply automatic ESLint fixes.
- `yarn prettier` or `npm run prettier`: format source files.
- `yarn knip` or `npm run knip`: detect unused files, exports, and dependencies.

## Coding Style & Naming Conventions

Use TypeScript strict mode. Prefer `.tsx` for React components and `.ts` for plain logic, API clients, types, and utilities. Follow existing names: components use `PascalCase`, hooks use `useSomething`, and domain files generally use lowercase or kebab-case names such as `exam-room.ts`. Imports can use `src` as the base URL. Prettier uses 2 spaces, single quotes, no trailing commas, and 140-character print width. ESLint enforces React Hooks rules and unused variables.

## Testing Guidelines

No automated test script is currently configured. Before submitting changes, run `yarn lint` and `yarn build` as the minimum verification. If tests are added, place them near the code they cover using `*.test.ts` or `*.test.tsx`, and add a corresponding package script.

## Commit & Pull Request Guidelines

Recent commits use concise Conventional Commit-style prefixes such as `feat:`, `fix:`, and `chore:`. Keep commit subjects imperative and scoped to one change. Pull requests should include a short summary, verification commands run, linked issues when applicable, and screenshots or screen recordings for visible UI changes.

## Security & Configuration Tips

Environment-specific values belong in `.env` files consumed by Vite and `env-cmd`. Do not commit secrets or machine-specific credentials. Keep API and authentication changes in `src/api/`, `src/utils/axios.ts`, and related context files.

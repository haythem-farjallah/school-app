# School App: lean feature delivery

## Architecture and sources of truth
- Backend: Spring Boot (Java 17), PostgreSQL and Redis. Treat `backend/` as frozen for frontend feature work unless the user explicitly authorizes a backend change.
- Frontend: React + TypeScript + Vite in `frontend/`.
- Follow existing routing, authorization, service, DTO, data-fetching and testing patterns instead of introducing a parallel architecture.
- For new UI follow `frontend/docs/design-system.md`; for browser security follow `frontend/docs/security.md`.
- Use existing API contracts and authorization semantics. Never invent endpoints, DTO fields, role permissions, success states or mock dashboards.
- Tests and mock payloads must match real backend contracts. Do not weaken tests to make the feature pass.

## Fast, safe loop
1. Perform **targeted** reconnaissance of the requested feature's relevant routes, services, types, tests, and backend controller/DTO. Summarize findings briefly. Do not inventory unrelated systems by default.
2. Define the smallest user-visible, end-to-end slice and acceptance criteria before editing.
3. Implement one slice per builder, reusing components and accessible patterns already in the repository.
4. Add or update targeted tests for success, error, empty and authorization-sensitive cases as appropriate.
5. Verify with a focused test, the frontend build and quality baseline. For significant user workflows, run Playwright; use the existing CI for complete regression verification.
6. Get independent API-contract and QA reviews. Fix actual blockers; do not enter unbounded review/rewrite loops.
7. Report exact changed files, tests run with outcomes, known limitations and next suggested slice.

## Commands (from repository Makefile)
- `make frontend-test TEST=<pattern>` — targeted Vitest
- `make frontend-build` — production build
- `make quality` — TypeScript/ESLint quality ratchet against existing baseline
- `make e2e` — Playwright workflow (requires local Docker/services)
- `make task` — backend tests, frontend tests, build; `make verify` for full validation

## Safety and release
- Work on feature branches; never directly commit to `main`.
- Do not push, open a PR, merge, deploy, or change repository settings without explicit user instruction.
- Never alter credentials, real data, authentication, authorization, database migrations or CI gates merely to make a frontend task easier.
- In parallel work, give separate builders disjoint file ownership in isolated worktrees. Do not combine changes until conflicts and tests are resolved.
- Human review is the final approval; passing AI reviews never means auto-merge.

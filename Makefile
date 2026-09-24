# Developer commands. Verification levels:
#   FAST  make backend-test TEST=AuthLoginIntegrationTest[#method]
#         make frontend-test TEST=Login   |   make frontend-watch
#   TASK  make task     backend tests, frontend unit tests, production build
#                       (+ make e2e when the change is user-facing)
#   FULL  make verify   TASK + quality ratchet + Playwright + Docker images (what CI runs)

COMPOSE     := docker compose -f compose.dev.yml
MVN         := mvn -B -ntp -f backend/pom.xml
BACKEND_JAR := backend/target/school-management-0.0.1-SNAPSHOT.jar
BACKEND_SRC := backend/pom.xml $(shell find backend/src/main -type f)
# Written by npm install/ci; newer than the lockfile when dependencies are current.
FRONTEND_DEPS := frontend/node_modules/.package-lock.json

.DEFAULT_GOAL := help
.PHONY: help dev-up dev-down backend-run frontend-run backend-test frontend-test \
        frontend-watch frontend-build quality e2e task verify docker-build

help:
	@sed -n '1,7p' Makefile | sed 's/^# \{0,1\}//'
	@echo
	@echo "Targets: dev-up dev-down backend-run frontend-run backend-test frontend-test"
	@echo "         frontend-watch frontend-build quality e2e task verify docker-build"

# PostgreSQL + Redis; start once and leave running (no-op when already up).
dev-up:
	$(COMPOSE) up -d --wait

dev-down:
	$(COMPOSE) down

backend-run:
	$(MVN) spring-boot:run -Dspring-boot.run.profiles=dev

frontend-run: $(FRONTEND_DEPS)
	cd frontend && npm run dev

# Integration tests use Testcontainers and do not need dev-up.
backend-test:
	$(MVN) test $(if $(TEST),-Dtest='$(TEST)')

frontend-test: $(FRONTEND_DEPS)
	cd frontend && npm run test:unit $(if $(TEST),-- $(TEST))

frontend-watch: $(FRONTEND_DEPS)
	cd frontend && npm run test:watch

frontend-build: $(FRONTEND_DEPS)
	cd frontend && npm run build

quality: $(FRONTEND_DEPS)
	cd frontend && npm run quality

# Reuses a backend on :8088 and Vite on :5173 when they are already running.
e2e: dev-up $(BACKEND_JAR) $(FRONTEND_DEPS)
	cd frontend && npx playwright install chromium && npm run test:e2e

task: backend-test frontend-test frontend-build

verify: task quality e2e docker-build

docker-build:
	docker build -t school-app-backend:local backend
	docker build -t school-app-frontend:local frontend

$(FRONTEND_DEPS): frontend/package.json frontend/package-lock.json
	cd frontend && npm ci

$(BACKEND_JAR): $(BACKEND_SRC)
	$(MVN) package -Dmaven.test.skip=true

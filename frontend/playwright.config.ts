import { defineConfig, devices } from "@playwright/test";

/**
 * Browser tests against the real stack: React → Spring Boot → PostgreSQL/Redis.
 *
 * Prerequisites:
 *   docker compose -f ../compose.dev.yml up -d --wait
 *   mvn -f ../backend/pom.xml package -DskipTests
 *
 * Playwright starts the backend (dev profile, fixture accounts) and the Vite
 * dev server, or reuses ones already running locally.
 */
const backendJar = "../backend/target/school-management-0.0.1-SNAPSHOT.jar";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL: "http://localhost:5173",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: [
    {
      command: `java -jar ${backendJar}`,
      url: "http://localhost:8088/actuator/health",
      env: { SPRING_PROFILES_ACTIVE: "dev" },
      reuseExistingServer: !process.env.CI,
      timeout: 180_000,
      stdout: "ignore",
      stderr: "pipe",
    },
    {
      command: "npm run dev -- --port 5173 --strictPort",
      url: "http://localhost:5173/login",
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
  ],
});

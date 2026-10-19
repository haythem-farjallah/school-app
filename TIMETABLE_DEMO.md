# Verify My Timetable against the backend

This is **synthetic development fixture data persisted in PostgreSQL**, not production school information. The browser receives it from the real authenticated `GET /api/v1/timetables/me`; it does not generate lessons or use fallback demo responses.

Prerequisites: Java 17, Maven, Node 20, Python 3, Docker with Compose, and Chrome. Ports 5432, 6379, 8088 and 5173 must be available for this application's development stack.

## Start the demo

For Haythem's current PR #5 worktree, open a terminal and run:

```bash
cd /tmp/school-app-student-timetable-frontend
make dev-up
npm --prefix frontend ci
npm --prefix frontend run build
```

In a second terminal, start the backend and its opt-in fixtures:

```bash
cd /tmp/school-app-student-timetable-frontend
make timetable-backend
```

Wait for `Development timetable demo created with four persisted lessons`. On later starts, the loader reports that reserved demo data exists and leaves the records unchanged.

In a third terminal, serve the frontend build locally. This avoids the development render-profiler overlays during visual verification:

```bash
cd /tmp/school-app-student-timetable-frontend
npm --prefix frontend run preview -- --port 5173 --strictPort
```

If using another checkout, replace the `cd` path with that checkout's root. Stay on PR #5's `feat/student-own-timetable-frontend` branch.

Open **http://localhost:5173/login** in Chrome. Use this local-only account:

- Email: `student@fixtures.school.test`
- Password: `Fixture-Pass-2024`
- Student: **Sam Student**

Choose **My Timetable**, or open **http://localhost:5173/student/my-timetable** after login. The older `/student/schedule` URL redirects here.

## Expected data

All four lessons belong to **Demo 7A**, with teacher **Theo Teacher**. These are recurring weekday times from the API's `LocalTime` fields; the frontend does not apply timezone conversion.

| Day | Time | Subject | Teacher | Room |
| --- | --- | --- | --- | --- |
| Monday | 08:15–09:00 | Physics | Theo Teacher | Demo Lab 201 |
| Monday | 09:15–10:00 | Mathematics | Theo Teacher | Demo Room 101 |
| Saturday | 09:15–10:00 | Art | Theo Teacher | Demo Room 101 |
| Sunday | 09:15–10:00 | History | Theo Teacher | Demo Room 101 |

Tuesday through Friday have no demo lessons. Desktop shows all seven day columns with rows derived only from actual slot times. At widths below 1024px, select a day from **Day**; Monday is the initial selection. Period indices **101** and **102** are reserved development fixture periods, not an invented frontend schedule.

The loader creates an active **Development demo year** if there is no active academic year. Otherwise it reuses the existing active year without renaming or activating/deactivating any existing year. Enrollment is the student's class membership; TeachingAssignment records connect the teacher, subjects and class.

## Compare Chrome, API and persisted records

With the backend running, use another terminal:

```bash
cd /tmp/school-app-student-timetable-frontend
python3 scripts/verify-timetable-demo.py --api-output /tmp/school-timetable-api.json
```

Expected: `PASS: /api/v1/timetables/me equals persisted enrollment-selected PostgreSQL slots and the demo reference.`, followed by the four lessons above and record counts.

This check logs in normally, reads `/me`, and independently selects persisted slots through the student's ACTIVE Enrollment and active AcademicYear using a read-only SQL query. It compares slot IDs, day, times, class, subject, teacher, room and other returned details. It fails if the API fabricates, omits, duplicates or changes a slot, or if the saved demo differs from this reference. It never updates database records and never prints or saves login tokens.

To inspect the exact response in Chrome without copying credentials:

1. While logged in, open Chrome DevTools → **Network** and filter by `timetables/me`.
2. Reload My Timetable. Select the successful **GET** request and its **Preview** or **Response** tab.
3. Expand `data.slots`. For every slot compare `dayOfWeek`, `periodStartTime`, `periodEndTime`, `forClassName`, `forCourseName`, `teacherFirstName`, `teacherLastName`, and `roomName` with the browser.
4. The terminal command also saves that response's JSON to `/tmp/school-timetable-api.json`; it contains no access/refresh tokens.

Opening the API URL as a separate unauthenticated browser tab returns an authorization error; use the signed-in page's Network response or the verification command.

## Verify an actual restart

Stop only the backend started in terminal two with **Ctrl+C**. Keep PostgreSQL and Redis running. Then run:

```bash
cd /tmp/school-app-student-timetable-frontend
make timetable-restart
```

This builds the current backend if needed, launches it, verifies and snapshots the persisted rows and counts, stops its own process, launches a second process against the same database, and compares again. Expected final messages include:

```text
PASS: all 12 table counts and persisted slot IDs/details are unchanged after restart.
PASS: two separate backend starts retained the same records.
```

The check refuses to stop a backend that it did not start. It leaves its two backend processes stopped and retains local evidence under the printed `/tmp/school-timetable-restart.*` directory. Start `make timetable-backend` again to return to the browser.

For a restart you perform yourself, with the backend running:

```bash
python3 scripts/verify-timetable-demo.py --snapshot /tmp/school-timetable-before.json
```

Stop and restart `make timetable-backend`, then run:

```bash
python3 scripts/verify-timetable-demo.py --compare /tmp/school-timetable-before.json
```

Do not edit demo records or run other data-changing work between the snapshots. Authentication is still rate limited; if a command reports HTTP 429 after many logins, wait one minute before retrying.

## Verify all browser states

- **Desktop:** set Chrome to 1440px wide. All seven named columns must appear. Check all four lessons, including Saturday and Sunday.
- **Mobile:** use DevTools device emulation at 360px wide. The **Day** selector replaces the table; select Monday, Tuesday, Saturday and Sunday. Tuesday shows the selected-day empty message; populated days show exactly their API slots. There must be no horizontal page scrolling.
- **Loading:** in DevTools Network, disable cache and select Slow 3G. Reload the page and observe **Loading your timetable** before the real response arrives. Restore normal networking.
- **Error:** after viewing the timetable once, navigate to Home, stop the development backend in terminal two with **Ctrl+C**, then use My Timetable in the app navigation. Expect **Your timetable could not be loaded** and **Try again**. Restart `make timetable-backend`, wait for its fixture message, then select Try again; the persisted lessons return. Keep Chrome online: its Offline mode can cause React Query to pause requests instead of issuing a failed request.
- **Empty:** log out and sign in as `empty.student@fixtures.school.test`, password `Fixture-Pass-2024`. This development student has no enrollment. Open the same timetable URL; expect **No timetable slots to display** and a real HTTP 200 response with `data.slots: []`.

## Repeat the automated checks

Backend integration tests use isolated Testcontainers databases, not the development database:

```bash
cd /tmp/school-app-student-timetable-frontend
mvn -B -ntp -f backend/pom.xml package
npm --prefix frontend run test:unit
npm --prefix frontend run build
npm --prefix frontend run quality
cd frontend
npx tsc -p tsconfig.node.json --noEmit
npx playwright install chromium
npm run test:e2e
```

Start the development dependencies first. Playwright starts the dev backend with the demo opt-in, builds the frontend and starts Vite preview, or reuses local servers. A reused backend must have been started with `make timetable-backend`. For just this packet's real-backend checks:

```bash
cd /tmp/school-app-student-timetable-frontend/frontend
npm run test:e2e -- student-timetable.spec.ts
```

The primary timetable test does not substitute `/me` JSON: it delays and then continues the genuine request to test loading, compares its response with PostgreSQL and the displayed slots, checks desktop/mobile/day placement, and injects a connection-refused failure for a later request and checks recovery through the real API. The empty-state test uses its separate real student account. An altered local JSON copy is a negative control proving that the persistence verifier rejects fabricated times; it is never sent to the browser or backend. Screenshots and comparison evidence are in `frontend/test-results`; CI independently runs the two-process backend restart check against the same development database and uploads the Playwright report and evidence even on success.

Automated assertions and screenshots are distinct from a person manually operating Chrome. The packet handoff states which checks actually ran, whether screenshots were visually inspected, and links CI for the exact pushed commit. A passing previous commit does not verify this packet. The frontend quality command is a debt ratchet; raw application TypeScript/ESLint checks still have pre-existing diagnostics.

## Data safety and conflicts

Demo seeding requires both the **dev** profile and the explicit `SCHOOL_DEV_TIMETABLE_FIXTURES_ENABLED=true` opt-in. It is excluded when `prod` or `production` is active. Normal production startup does not run this loader. The local account password and signing key must not be used for production.

The demo is created atomically once. Existing reserved demo class/course/room/period/timetable records, changed Sam/Theo fixture identities/memberships, or enrollment history prevent initial seeding. Repeat starts preserve existing demo changes and deletions while any reserved identity remains; they do not “repair” or reset records. Removing every demo identity permits fresh seeding. A verification failure after such changes is intentional. No startup command here drops volumes, deletes tables, resets credentials, or overrides an enrollment. If the loader reports a conflict, retain that database and report the conflict rather than editing records to force a passing demo.

The repository also contains historical Flyway sample-data migrations. Those records are separate from this opt-in development demo; this packet does not change historical migrations or classify their samples as real school data.

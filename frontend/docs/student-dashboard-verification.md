# Student dashboard verification

Run from the repository root with Java 17, Maven, Node 20, npm and Docker available:

```bash
docker compose -f compose.dev.yml up -d --wait
mvn -B -ntp -f backend/pom.xml package
npm --prefix frontend ci
```

Start the backend in one terminal:

```bash
SPRING_PROFILES_ACTIVE=dev java -jar backend/target/school-management-0.0.1-SNAPSHOT.jar
```

Start the frontend in another:

```bash
npm --prefix frontend run dev -- --port 5173 --strictPort
```

Open <http://localhost:5173/login> in Chrome. Sign in with the existing development
account `student@fixtures.school.test`, password `Fixture-Pass-2024`, then open
<http://localhost:5173/student/dashboard>.

In DevTools → Network, filter Fetch/XHR and reload. Compare these responses:

- `/api/v1/dashboard/current-user`: `data.baseInfo.userInfo` supplies the name and
  email; `enrolledClasses` supplies current classes; `recentGrades` supplies result
  descriptions, raw scores and dates; `baseInfo.notifications` supplies unread notices.
- `/api/v1/announcements/public?page=0&size=3`: each returned announcement supplies
  its title, body, importance, creation date and optional active dates. This page is
  not guaranteed to contain the globally newest announcements.

The dashboard never requests a student dashboard by a browser-supplied ID.
`stats.totalEnrollments` includes history and is not displayed as current enrollment.
`recentGrades.courseName` is currently a **class** name and is labeled accordingly.
Scores are shown in their recorded units, without an inferred percentage or maximum.
The backend average is shown only when a returned result has a numeric recorded score,
including zero. With no such evidence, the average is unavailable. If the five recent
results all lack scores, an older recorded score cannot be verified from this response;
the average remains unavailable even if the backend reports a nonzero average.

The existing fixture account may have no classes, grades, announcements or notifications.
Those empty states are expected; no fixture editing or database manipulation is needed.
The timetable is explicitly unavailable on this dashboard until the separate timetable
work is merged and integrated in a later packet.

Use Chrome device emulation at 390px and desktop at 1440px: cards should stack on a phone
without horizontal scrolling. Profile and Announcements are the available quick actions.
To inspect failures, block either endpoint in DevTools, reload, then unblock it and use
Retry. The other section should remain usable. A rejected session follows the existing
refresh/logout behavior. Sign out through the account menu and confirm the private
dashboard is no longer accessible.

Automated real-backend browser checks (read the actual browser responses; no dashboard
JSON mocking, fixture edits or academic-data writes by these tests):

```bash
cd frontend
npx playwright install chromium
npm run test:e2e -- e2e/student-dashboard.spec.ts e2e/auth.spec.ts
```

Playwright starts the built backend with the dev profile and Vite, or reuses existing
servers. Ensure reused servers belong to this branch. Populated result, announcement,
zero-score and failure cases are also covered by isolated MSW frontend tests; an empty
development fixture does not prove populated rendering in the real database.

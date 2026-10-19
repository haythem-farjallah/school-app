#!/usr/bin/env python3
"""Read-only comparison of the local development API with persisted PostgreSQL rows."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
API = "http://localhost:8088/api"
STUDENT = "student@fixtures.school.test"

# Enrollment and the active academic year select the records independently of the API.
SQL = """
SELECT json_build_object(
  'slots', COALESCE((SELECT json_agg(row_to_json(r) ORDER BY r.id) FROM (
    SELECT s.id, s.day_of_week AS "dayOfWeek", s.description,
      p.id AS "periodId", p.index_number AS "periodIndex",
      p.start_time AS "periodStartTime", p.end_time AS "periodEndTime",
      c.id AS "forClassId", c.name AS "forClassName",
      co.id AS "forCourseId", co.name AS "forCourseName", co.code AS "forCourseCode",
      co.color AS "forCourseColor", t.id AS "teacherId", t.first_name AS "teacherFirstName",
      t.last_name AS "teacherLastName", t.email AS "teacherEmail",
      room.id AS "roomId", room.name AS "roomName", room.capacity AS "roomCapacity"
    FROM timetable_slots s
    JOIN classes c ON c.id = s.for_class_id
    JOIN academic_years y ON y.id = c.academic_year_id AND y.active
    JOIN enrollments e ON e.class_id = c.id AND e.status = 'ACTIVE'
    JOIN users student ON student.id = e.student_id
    JOIN periods p ON p.id = s.period_id
    LEFT JOIN courses co ON co.id = s.for_course_id
    LEFT JOIN users t ON t.id = s.teacher_id
    LEFT JOIN rooms room ON room.id = s.room_id
    WHERE student.email = 'student@fixtures.school.test'
  ) r), '[]'::json),
  'counts', (SELECT json_object_agg(name, total) FROM (
    SELECT 'academic_years' name, count(*) total FROM academic_years
    UNION ALL SELECT 'classes', count(*) FROM classes
    UNION ALL SELECT 'courses', count(*) FROM courses
    UNION ALL SELECT 'periods', count(*) FROM periods
    UNION ALL SELECT 'rooms', count(*) FROM rooms
    UNION ALL SELECT 'teaching_assignments', count(*) FROM teaching_assignments
    UNION ALL SELECT 'enrollments', count(*) FROM enrollments
    UNION ALL SELECT 'timetables', count(*) FROM timetables
    UNION ALL SELECT 'timetable_slots', count(*) FROM timetable_slots
    UNION ALL SELECT 'users', count(*) FROM users
    UNION ALL SELECT 'school_memberships', count(*) FROM school_memberships
    UNION ALL SELECT 'school_membership_roles', count(*) FROM school_membership_roles
  ) counts)
);
"""


def read_json(request):
    with urlopen(request, timeout=20) as response:
        return json.load(response)


def verify(api_response, persisted):
    slots = api_response["data"]["slots"]
    records = persisted["slots"]
    if not records:
        raise ValueError("No persisted demo slots. Start the backend with the dev profile and demo opt-in.")
    if len({slot["id"] for slot in slots}) != len(slots):
        raise ValueError("The API contains duplicate slot IDs.")
    if len(slots) != len(records):
        raise ValueError(f"API has {len(slots)} slots; PostgreSQL has {len(records)}.")
    by_id = {slot["id"]: slot for slot in slots}
    for record in records:
        slot = by_id.get(record["id"])
        if slot is None or any(slot.get(key) != value for key, value in record.items()):
            raise ValueError(f"API differs from PostgreSQL for slot {record['id']}.")
    expected = sorted([
        ("MONDAY", "08:15:00", "09:00:00", "Physics", "Demo Lab 201"),
        ("MONDAY", "09:15:00", "10:00:00", "Mathematics", "Demo Room 101"),
        ("SATURDAY", "09:15:00", "10:00:00", "Art", "Demo Room 101"),
        ("SUNDAY", "09:15:00", "10:00:00", "History", "Demo Room 101"),
    ])
    actual = sorted((s["dayOfWeek"], s["periodStartTime"], s["periodEndTime"],
                     s["forCourseName"], s["roomName"]) for s in slots)
    if actual != expected or any(s["forClassName"] != "Demo 7A" or
            (s["teacherFirstName"], s["teacherLastName"]) != ("Theo", "Teacher") for s in slots):
        raise ValueError("Persisted data matches the API but differs from the documented demo. Existing edits were preserved.")
    return slots


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--response", type=Path, help="Compare an already captured /me JSON response (no extra login)")
    parser.add_argument("--api-output", type=Path, help="Save /me JSON only; never saves authentication tokens")
    parser.add_argument("--snapshot", type=Path, help="Save record counts and slot data before a backend restart")
    parser.add_argument("--compare", type=Path, help="Require unchanged counts and slots after a restart")
    args = parser.parse_args()
    if args.response:
        response = json.loads(args.response.read_text())
    else:
        credentials = json.dumps({"email": STUDENT, "password": "Fixture-Pass-2024"}).encode()
        login = read_json(Request(API + "/auth/login", data=credentials,
                                  headers={"Content-Type": "application/json"}))
        response = read_json(Request(API + "/v1/timetables/me",
                                    headers={"Authorization": "Bearer " + login["data"]["accessToken"]}))
    result = subprocess.run(["docker", "compose", "-f", str(ROOT / "compose.dev.yml"),
                             "exec", "-T", "postgres", "psql", "-U", "school", "-d", "school_db",
                             "-At", "-v", "ON_ERROR_STOP=1", "-c", SQL],
                            capture_output=True, text=True, timeout=30, check=False)
    if result.returncode:
        raise ValueError("Cannot read the development PostgreSQL. Run docker compose -f compose.dev.yml up -d --wait.")
    persisted = json.loads(result.stdout)
    slots = verify(response, persisted)
    if args.compare and json.loads(args.compare.read_text()) != persisted:
        raise ValueError("Records or counts changed since the snapshot; restart verification failed.")
    if args.snapshot:
        args.snapshot.write_text(json.dumps(persisted, indent=2) + "\n")
    if args.api_output:
        args.api_output.write_text(json.dumps(response, indent=2) + "\n")
    print("PASS: /api/v1/timetables/me equals persisted enrollment-selected PostgreSQL slots and the demo reference.")
    print("Day        Time          Subject       Class    Teacher       Room")
    order = {day: index for index, day in enumerate(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"])}
    for slot in sorted(slots, key=lambda s: (order[s["dayOfWeek"]], s["periodStartTime"])):
        print(f"{slot['dayOfWeek']:10} {slot['periodStartTime'][:5]}–{slot['periodEndTime'][:5]}   "
              f"{slot['forCourseName']:13} {slot['forClassName']}  "
              f"{slot['teacherFirstName']} {slot['teacherLastName']}  {slot['roomName']}")
    print("Record counts: " + json.dumps(persisted["counts"], sort_keys=True))
    if args.compare:
        print("PASS: all 12 table counts and persisted slot IDs/details are unchanged after restart.")


if __name__ == "__main__":
    try:
        main()
    except HTTPError as error:
        print(f"FAIL: Development API returned HTTP {error.code}.", file=sys.stderr)
        sys.exit(1)
    except (URLError, OSError, ValueError, KeyError, subprocess.TimeoutExpired) as error:
        # Do not print requests, headers or login responses.
        message = str(error) if isinstance(error, ValueError) else type(error).__name__
        print(f"FAIL: {message}", file=sys.stderr)
        sys.exit(1)

#!/usr/bin/env bash
# Starts and stops only its own two backend processes; never deletes development data.
set -euo pipefail
cd "$(dirname "$0")/.."
jar_path="$PWD/backend/target/school-management-0.0.1-SNAPSHOT.jar"
if [[ ! -f "$jar_path" ]]; then
  echo 'Build first: mvn -B -ntp -f backend/pom.xml package -Dmaven.test.skip=true' >&2
  exit 1
fi
if curl --silent --max-time 2 http://localhost:8088/actuator/health >/dev/null; then
  echo 'Stop the development backend on port 8088 first. This check will not stop another process.' >&2
  exit 1
fi
run_dir=$(mktemp -d /tmp/school-timetable-restart.XXXXXX)
demo_pid=''
cleanup() {
  if [[ -n "$demo_pid" ]]; then
    kill "$demo_pid" 2>/dev/null || true
    wait "$demo_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT
start_backend() {
  SPRING_PROFILES_ACTIVE=dev SCHOOL_DEV_TIMETABLE_FIXTURES_ENABLED=true \
    java -jar "$jar_path" > "$run_dir/backend-$1.log" 2>&1 &
  demo_pid=$!
  for ((attempt=0; attempt<180; attempt++)); do
    if ! kill -0 "$demo_pid" 2>/dev/null; then
      echo "Backend failed to start. Local log: $run_dir/backend-$1.log" >&2
      exit 1
    fi
    if curl --fail --silent --max-time 2 http://localhost:8088/actuator/health >/dev/null &&
       python3 -c 'import pathlib,sys; text=pathlib.Path(sys.argv[1]).read_text(); sys.exit(not any(marker in text for marker in ("Development timetable demo created", "Development timetable demo skipped")))' "$run_dir/backend-$1.log"; then
      return
    fi
    sleep 1
  done
  echo "Backend health timed out. Local log: $run_dir/backend-$1.log" >&2
  exit 1
}
start_backend first
python3 scripts/verify-timetable-demo.py --snapshot "$run_dir/before.json" | tee "$run_dir/first-check.txt"
kill "$demo_pid"
wait "$demo_pid" || true
demo_pid=''
start_backend second
python3 scripts/verify-timetable-demo.py --compare "$run_dir/before.json" | tee "$run_dir/second-check.txt"
echo "PASS: two separate backend starts retained the same records. Evidence: $run_dir"

#!/bin/sh
# 재시작 횟수 제한: 실패 시 최대 N회만 재시도 후 중단. docker compose restart: on-failure 와 함께 사용.
# RESTART_MAX_ATTEMPTS 미설정 시 기본 3회.
MAX_ATTEMPTS="${RESTART_MAX_ATTEMPTS:-3}"
COUNTER_FILE="/tmp/backend_restart_count"

get_count() {
  if [ -f "$COUNTER_FILE" ]; then
    cat "$COUNTER_FILE" 2>/dev/null || echo "0"
  else
    echo "0"
  fi
}

inc_count() {
  count=$(get_count)
  count=$((count + 1))
  echo "$count" > "$COUNTER_FILE"
  echo "$count"
}

clear_count() {
  rm -f "$COUNTER_FILE"
}

count=$(get_count)
if [ "$count" -ge "$MAX_ATTEMPTS" ] 2>/dev/null; then
  echo "Backend already failed ${MAX_ATTEMPTS} times. Not restarting. Remove container or recreate to retry."
  exit 0
fi

# exec 사용 안 함: 종료 코드 확인 후 카운터 처리
# BACKEND_OPTS: 선택적 JVM 옵션 (예: -Xmx512m). 미설정 시 빈 값으로 기본 동작.
if [ $# -gt 0 ]; then
  "$@"
else
  java -Duser.timezone=Asia/Seoul ${BACKEND_OPTS} -jar /app/app.jar
fi
exitcode=$?

if [ "$exitcode" -ne 0 ]; then
  newcount=$(inc_count)
  echo "Backend exited with $exitcode (restart $newcount/$MAX_ATTEMPTS)."
  if [ "$newcount" -ge "$MAX_ATTEMPTS" ]; then
    echo "Max restart attempts ($MAX_ATTEMPTS) reached. Stopping."
    exit 0
  fi
  exit 1
fi
clear_count
exit "$exitcode"

#!/usr/bin/env bash
# =============================================================================
# 인증 보안 통합 테스트 — 즉시무효화 / 리프레시 토큰 회전 / 재사용 탐지
#
# 사전 조건:
#   1. Redis / MySQL 실행
#   2. portal 실행 (만료·grace 단축 권장):
#      JWT_EXPIRATION=10000 JWT_ROTATION_GRACE=5000 \
#        ./gradlew :service:portal:bootRun
#   3. (시나리오 8용, 선택) gateway 실행:
#      ./gradlew :service:api-gateway:bootRun
#
# 실행:
#   ./test-auth-security.sh                 # 기본값 (grace 5초, 액세스 만료 10초 가정)
#   GRACE=60 ACCESS_EXP=3600 ./test-auth-security.sh   # 운영 기본값으로 띄운 경우
#
# 환경변수:
#   PORTAL      portal 주소       (기본 http://localhost:8080)
#   GATEWAY     gateway 주소      (기본 http://localhost:9000)
#   GRACE       회전 grace 초     (기본 5 — portal의 JWT_ROTATION_GRACE와 맞출 것)
#   ACCESS_EXP  액세스 만료 초    (기본 10 — portal의 JWT_EXPIRATION과 맞출 것)
#   REDIS_ARGS  redis-cli 추가 인자 (예: "-a password")
# =============================================================================

set -uo pipefail

PORTAL="${PORTAL:-http://localhost:8080}"
GATEWAY="${GATEWAY:-http://localhost:9000}"
GRACE="${GRACE:-5}"
ACCESS_EXP="${ACCESS_EXP:-10}"
REDIS_ARGS="${REDIS_ARGS:-}"
JAR="/tmp/auth_test_cookies.txt"
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

PASS=0; FAIL=0
ok()   { echo -e "${GREEN}[PASS]${NC} $1"; PASS=$((PASS+1)); }
fail() { echo -e "${RED}[FAIL]${NC} $1"; FAIL=$((FAIL+1)); }
info() { echo -e "${YELLOW}[INFO]${NC} $1"; }
sep()  { echo "──────────────────────────────────────────────────"; }

cookie_val() { grep -E "\b$2\b" "$1" 2>/dev/null | awk '{print $NF}' | tail -1; }
rkeys() { redis-cli $REDIS_ARGS KEYS "$1" 2>/dev/null; }

wait_sec() {
  for i in $(seq "$1" -1 1); do printf "\r  ${YELLOW}%d초 대기...${NC}   " "$i"; sleep 1; done
  printf "\r  대기 완료          \n"
}

rm -f "$JAR" "$JAR".*

# =============================================================================
sep; info "0/8  사전 확인"; sep
# =============================================================================
P=$(curl -s -o /dev/null -w "%{http_code}" "$PORTAL/actuator/health" 2>/dev/null || echo 000)
if [ "$P" != "200" ]; then fail "portal($PORTAL) 응답 없음 — 먼저 실행해 주세요."; exit 1; fi
ok "portal 정상"

if redis-cli $REDIS_ARGS PING >/dev/null 2>&1; then
  ok "redis-cli 사용 가능 (Redis 키 검증 포함)"
  HAS_REDIS=1
else
  info "redis-cli 접속 불가 — Redis 키 검증은 건너뜁니다 (REDIS_ARGS로 인증 지정 가능)"
  HAS_REDIS=0
fi

GW_UP=0
curl -s -o /dev/null -w "%{http_code}" "$GATEWAY/actuator/health" 2>/dev/null | grep -q 200 && GW_UP=1
[ "$GW_UP" = "1" ] && info "gateway 감지됨 — 시나리오 8 실행" || info "gateway 미실행 — 시나리오 8 건너뜀"

# =============================================================================
sep; info "1/8  게스트 로그인 — 쿠키 발급, httpOnly, body에 refreshToken 미노출"; sep
# =============================================================================
BODY=$(curl -s -c "$JAR" -X POST "$PORTAL/api/v1/auth/guest/login")

AT=$(cookie_val "$JAR" accessToken)
RT=$(cookie_val "$JAR" refreshToken)
[ -n "$AT" ] && ok "accessToken 쿠키 발급" || fail "accessToken 쿠키 없음"
[ -n "$RT" ] && ok "refreshToken 쿠키 발급" || fail "refreshToken 쿠키 없음"

# curl 쿠키저장소에서 httpOnly 쿠키는 #HttpOnly_ 접두어가 붙는다
if grep "^#HttpOnly_" "$JAR" | grep -q refreshToken; then
  ok "refreshToken 쿠키 httpOnly 확인"
else
  fail "refreshToken 쿠키가 httpOnly가 아님"
fi

if echo "$BODY" | grep -qE '"refreshToken":"[^"]+"'; then
  fail "응답 body에 refreshToken 노출됨"
else
  ok "응답 body에 refreshToken 미노출"
fi

# =============================================================================
sep; info "2/8  보호 엔드포인트 접근 — 유효 토큰 통과"; sep
# =============================================================================
S=$(curl -s -o /dev/null -w "%{http_code}" -b "$JAR" "$PORTAL/api/v1/users/me")
if [ "$S" != "401" ] && [ "$S" != "403" ]; then
  ok "유효 토큰으로 접근 성공 (status: $S)"
else
  fail "유효 토큰인데 거부됨 (status: $S)"
fi

# =============================================================================
sep; info "3/8  refresh 회전 — 새 refreshToken 발급 + Redis 키 교체"; sep
# =============================================================================
OLD_RT="$RT"
curl -s -b "$JAR" -c "$JAR" -X POST "$PORTAL/api/v1/auth/refresh" > /dev/null
NEW_RT=$(cookie_val "$JAR" refreshToken)

if [ -n "$NEW_RT" ] && [ "$NEW_RT" != "$OLD_RT" ]; then
  ok "refreshToken 회전됨 (${OLD_RT:0:8}... → ${NEW_RT:0:8}...)"
else
  fail "refreshToken이 회전되지 않음"
fi

if [ "$HAS_REDIS" = "1" ]; then
  [ -z "$(rkeys "auth:refresh:$OLD_RT")" ]          && ok "구 토큰 auth:refresh 키 삭제됨"      || fail "구 토큰 키가 남아있음"
  [ -n "$(rkeys "auth:refresh:$NEW_RT")" ]          && ok "새 토큰 auth:refresh 키 생성됨"      || fail "새 토큰 키 없음"
  [ -n "$(rkeys "auth:refresh-rotated:$OLD_RT")" ]  && ok "grace 마커(refresh-rotated) 존재"   || fail "grace 마커 없음"
  [ -n "$(rkeys "auth:refresh-used:$OLD_RT")" ]     && ok "재사용 탐지 마커(refresh-used) 존재" || fail "재사용 탐지 마커 없음"
fi

# =============================================================================
sep; info "4/8  grace 구간 — 구 토큰으로 refresh 시 같은 새 토큰 공유"; sep
# =============================================================================
H=$(curl -s -D - -o /dev/null -b "refreshToken=$OLD_RT" -X POST "$PORTAL/api/v1/auth/refresh")
S=$(echo "$H" | head -1 | awk '{print $2}')
GRACE_RT=$(echo "$H" | grep -i "^set-cookie: refreshToken=" | sed 's/[Ss]et-[Cc]ookie: refreshToken=//' | cut -d';' -f1 | tr -d '\r')

if [ "$S" = "200" ]; then ok "grace 구간 내 구 토큰 refresh 허용 (200)"; else fail "grace 구간인데 거부됨 (status: $S)"; fi
if [ "$GRACE_RT" = "$NEW_RT" ]; then
  ok "선점자가 만든 동일한 새 토큰 공유 확인"
else
  fail "다른 토큰이 발급됨 (중복 회전 발생: ${GRACE_RT:0:8}...)"
fi

# =============================================================================
sep; info "5/8  동시 refresh 5건 — 전부 성공 + 같은 토큰 수렴"; sep
# =============================================================================
CUR_RT="$NEW_RT"
for i in 1 2 3 4 5; do
  curl -s -D "$JAR.h$i" -o /dev/null -b "refreshToken=$CUR_RT" -X POST "$PORTAL/api/v1/auth/refresh" &
done
wait

CODES=$(for i in 1 2 3 4 5; do head -1 "$JAR.h$i" | awk '{print $2}'; done | sort -u | tr '\n' ' ')
TOKENS=$(for i in 1 2 3 4 5; do
  grep -i "^set-cookie: refreshToken=" "$JAR.h$i" | sed 's/[Ss]et-[Cc]ookie: refreshToken=//' | cut -d';' -f1 | tr -d '\r'
done | sort -u)
N_TOKENS=$(echo "$TOKENS" | grep -c .)

if [ "$(echo "$CODES" | tr -d ' ')" = "200" ]; then ok "동시 5건 전부 200"; else fail "실패한 요청 있음 (codes: $CODES)"; fi
if [ "$N_TOKENS" = "1" ]; then
  ok "5건 모두 같은 새 토큰으로 수렴 (single-flight 동작)"
else
  fail "서로 다른 토큰 ${N_TOKENS}개 발급됨 (경합 발생)"
fi
CUR_RT=$(echo "$TOKENS" | head -1)

# =============================================================================
sep; info "6/8  재사용 탐지 — grace 경과 후 구 토큰 사용 → 세션 강제 종료"; sep
# =============================================================================
info "grace(${GRACE}초) + 2초 경과 대기"
wait_sec $((GRACE + 2))

# 5번 시나리오에서 회전 직전에 쓰인 NEW_RT(이제 폐기됨)를 탈취범이 재사용했다고 가정
S=$(curl -s -o /dev/null -w "%{http_code}" -b "refreshToken=$NEW_RT" -X POST "$PORTAL/api/v1/auth/refresh")
if [ "$S" = "401" ]; then ok "폐기 토큰 재사용 거부 (401)"; else fail "폐기 토큰이 통과됨 (status: $S)"; fi

# 재사용 탐지는 세션 전체를 죽여야 함 → 정상 사용자의 현재(최신) 토큰도 무효
S=$(curl -s -o /dev/null -w "%{http_code}" -b "refreshToken=$CUR_RT" -X POST "$PORTAL/api/v1/auth/refresh")
if [ "$S" = "401" ]; then ok "세션 강제 종료 확인 — 최신 토큰도 무효화됨"; else fail "세션이 살아있음 (status: $S)"; fi

if [ "$HAS_REDIS" = "1" ]; then
  [ -z "$(rkeys "auth:session:*")" ] && ok "auth:session 키 삭제 확인" || info "다른 세션 키 존재 (다른 로그인과 병행 시 정상)"
fi

# =============================================================================
sep; info "7/8  로그아웃 즉시무효화 — 액세스 토큰 만료 상태 포함"; sep
# =============================================================================
rm -f "$JAR"
curl -s -c "$JAR" -X POST "$PORTAL/api/v1/auth/guest/login" > /dev/null
AT=$(cookie_val "$JAR" accessToken)
RT=$(cookie_val "$JAR" refreshToken)

info "액세스 토큰 만료 대기 (${ACCESS_EXP}초 + 1초) — 만료 상태 로그아웃 버그 검증"
wait_sec $((ACCESS_EXP + 1))

curl -s -b "$JAR" -X POST "$PORTAL/api/v1/auth/logout" > /dev/null

# 만료 토큰으로 로그아웃해도 세션/리프레시가 전부 죽어야 한다
S=$(curl -s -o /dev/null -w "%{http_code}" -b "refreshToken=$RT" -X POST "$PORTAL/api/v1/auth/refresh")
if [ "$S" = "401" ]; then
  ok "만료 토큰 로그아웃 후 refreshToken 무효화 확인"
else
  fail "로그아웃했는데 refreshToken이 살아있음 (status: $S) — 즉시무효화 실패"
fi

if [ "$HAS_REDIS" = "1" ]; then
  [ -z "$(rkeys "auth:session:*")" ] && ok "세션 키 삭제 확인" || info "다른 세션 키 존재 (병행 로그인 시 정상)"
fi

# =============================================================================
sep; info "8/8  게이트웨이 자동 갱신 — 만료 토큰 요청이 그 자리에서 통과"; sep
# =============================================================================
if [ "$GW_UP" = "1" ]; then
  # 게이트웨이 라우트는 /portal 접두어 필수 (bare /api/v1/** 라우트 없음)
  rm -f "$JAR"
  curl -s -c "$JAR" -X POST "$GATEWAY/portal/api/v1/auth/guest/login" > /dev/null
  info "액세스 토큰 만료 대기 (${ACCESS_EXP}초 + 1초)"
  wait_sec $((ACCESS_EXP + 1))

  # 만료된 accessToken + 유효한 refreshToken → 게이트웨이가 재발급 후 그 요청을 이어가야 함
  S=$(curl -s -o /dev/null -w "%{http_code}" -b "$JAR" "$GATEWAY/portal/api/v1/users/me")
  if [ "$S" != "401" ] && [ "$S" != "403" ]; then
    ok "만료 토큰 요청 자동 갱신 후 통과 (status: $S) — 재시도 불필요"
  else
    fail "자동 갱신 후에도 401 — 새 토큰 downstream 전달 실패"
  fi
else
  info "gateway 미실행 — 건너뜀"
fi

# =============================================================================
sep
echo ""
echo -e "결과: ${GREEN}PASS $PASS${NC} / ${RED}FAIL $FAIL${NC}"
echo ""
echo "Redis 상태 직접 확인:"
echo "  redis-cli KEYS 'auth:*'                        # 전체 인증 키"
echo "  redis-cli TTL  'auth:refresh:<UUID>'            # 리프레시 토큰 남은 TTL"
echo "  redis-cli GET  'auth:refresh-used:<UUID>'       # 재사용 탐지 마커 → sessionId"
echo "  redis-cli GET  'auth:session-refresh:<sessionId>'  # 세션의 활성 리프레시 토큰"
sep
[ "$FAIL" = "0" ] && exit 0 || exit 1

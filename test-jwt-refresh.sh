#!/usr/bin/env bash
# =============================================================================
# JWT 리프레시 토큰 통합 테스트 스크립트
#
# 사전 조건:
#   1. Redis 실행:  redis-server &
#   2. portal 실행: ./gradlew :service:portal:bootRun --args='--spring.profiles.active=local,test'
#   3. gateway 실행: ./gradlew :service:api-gateway:bootRun --args='--spring.profiles.active=local,test'
#
# application-test.yml 덕분에 액세스 토큰 10초, 리프레시 토큰 60초로 동작합니다.
# =============================================================================

set -euo pipefail

GATEWAY="http://localhost:9000"
PORTAL="http://localhost:8080"
COOKIE_JAR="/tmp/jwt_test_cookies.txt"
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

ok()   { echo -e "${GREEN}[PASS]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; }
info() { echo -e "${YELLOW}[INFO]${NC} $1"; }
sep()  { echo "────────────────────────────────────────"; }

# ─── 헬퍼: HTTP 상태 코드 추출 ─────────────────────────────────────────────
http_status() {
  echo "$1" | tail -n1
}

# ─── 헬퍼: JSON 필드 추출 (jq 없이) ──────────────────────────────────────
extract() {
  local json="$1" key="$2"
  echo "$json" | grep -o "\"${key}\":\"[^\"]*\"" | head -1 | sed "s/\"${key}\":\"//;s/\"//"
}

rm -f "$COOKIE_JAR"

# =============================================================================
# 1. 서비스 헬스체크
# =============================================================================
sep
info "1/7  서비스 헬스체크"
sep

GW_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY/actuator/health" 2>/dev/null || echo "000")
if [ "$GW_HEALTH" = "200" ]; then
  ok "게이트웨이(9000) 정상"
else
  fail "게이트웨이(9000) 응답 없음 (status: $GW_HEALTH) — 먼저 서비스를 실행해 주세요."
  exit 1
fi

PORTAL_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$PORTAL/actuator/health" 2>/dev/null || echo "000")
[ "$PORTAL_HEALTH" = "200" ] && ok "포털(8080) 정상" || fail "포털(8080) 응답 없음"

# =============================================================================
# 2. 로그인 (일반 로그인)
# =============================================================================
sep
info "2/7  일반 로그인 → accessToken + refreshToken 쿠키 발급 확인"
sep

LOGIN_RESP=$(curl -s -c "$COOKIE_JAR" -w "\n%{http_code}" \
  -X POST "$GATEWAY/api/v1/auth/standard/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}')

LOGIN_STATUS=$(http_status "$LOGIN_RESP")
LOGIN_BODY=$(echo "$LOGIN_RESP" | head -n-1)

if [ "$LOGIN_STATUS" = "200" ]; then
  ok "로그인 성공 (200)"
else
  fail "로그인 실패 (status: $LOGIN_STATUS) — DB에 test@example.com 계정이 있어야 합니다."
  echo "$LOGIN_BODY"
  exit 1
fi

ACCESS_TOKEN=$(grep "accessToken" "$COOKIE_JAR" | awk '{print $NF}' | head -1)
REFRESH_TOKEN=$(grep "refreshToken" "$COOKIE_JAR" | awk '{print $NF}' | head -1)

if [ -n "$ACCESS_TOKEN" ]; then
  ok "accessToken 쿠키 수신 확인"
  info "  accessToken 앞 30자: ${ACCESS_TOKEN:0:30}..."
else
  fail "accessToken 쿠키 없음"
fi

if [ -n "$REFRESH_TOKEN" ]; then
  ok "refreshToken 쿠키 수신 확인"
  info "  refreshToken: $REFRESH_TOKEN"
else
  fail "refreshToken 쿠키 없음"
fi

# =============================================================================
# 3. Redis 상태 확인 (세션 + 리프레시 토큰)
# =============================================================================
sep
info "3/7  Redis 키 확인 (auth:session:*, auth:refresh:*)"
sep

SESSION_KEYS=$(redis-cli KEYS "auth:session:*" 2>/dev/null | head -3)
REFRESH_KEYS=$(redis-cli KEYS "auth:refresh:*" 2>/dev/null | head -3)

if [ -n "$SESSION_KEYS" ]; then
  ok "auth:session 키 존재"
  info "  $SESSION_KEYS"
  SESSION_KEY=$(redis-cli KEYS "auth:session:*" 2>/dev/null | head -1)
  SESSION_TTL=$(redis-cli TTL "$SESSION_KEY" 2>/dev/null)
  info "  TTL: ${SESSION_TTL}초 (약 60초 기대 — test 프로파일)"
else
  fail "auth:session 키 없음"
fi

if [ -n "$REFRESH_KEYS" ]; then
  ok "auth:refresh 키 존재"
  info "  $REFRESH_KEYS"
  REFRESH_KEY=$(redis-cli KEYS "auth:refresh:*" 2>/dev/null | head -1)
  REFRESH_TTL=$(redis-cli TTL "$REFRESH_KEY" 2>/dev/null)
  info "  TTL: ${REFRESH_TTL}초 (약 60초 기대 — test 프로파일)"
else
  fail "auth:refresh 키 없음"
fi

# =============================================================================
# 4. 유효한 accessToken으로 보호된 엔드포인트 접근
# =============================================================================
sep
info "4/7  유효한 accessToken → 보호된 엔드포인트 200 응답 확인"
sep

PROTECTED_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -b "$COOKIE_JAR" \
  "$GATEWAY/api/v1/portfolios")

if [ "$PROTECTED_STATUS" = "200" ] || [ "$PROTECTED_STATUS" = "404" ]; then
  ok "보호된 엔드포인트 접근 성공 (status: $PROTECTED_STATUS) — 401 아님"
else
  fail "예상치 못한 응답 (status: $PROTECTED_STATUS)"
fi

# =============================================================================
# 5. accessToken 만료 대기 (10초)
# =============================================================================
sep
info "5/7  accessToken 만료 대기 (test 프로파일: 10초 후 만료)..."
sep

for i in $(seq 10 -1 1); do
  printf "\r  ${YELLOW}%d초 후 만료...${NC}" "$i"
  sleep 1
done
printf "\r  accessToken 만료 완료!          \n"
ok "10초 경과"

# =============================================================================
# 6. 만료된 accessToken + 유효한 refreshToken → 자동 갱신 확인
# =============================================================================
sep
info "6/7  만료된 accessToken + refreshToken → 게이트웨이 자동 갱신 확인"
sep

REFRESH_RESP=$(curl -s -c "${COOKIE_JAR}.new" -b "$COOKIE_JAR" \
  -w "\n%{http_code}" \
  "$GATEWAY/api/v1/portfolios")

REFRESH_STATUS=$(http_status "$REFRESH_RESP")
NEW_COOKIE_HEADER=$(curl -sv -b "$COOKIE_JAR" "$GATEWAY/api/v1/portfolios" 2>&1 \
  | grep -i "set-cookie: accessToken" | head -1 || echo "")

if [ "$REFRESH_STATUS" = "200" ] || [ "$REFRESH_STATUS" = "404" ]; then
  ok "갱신 후 보호된 엔드포인트 접근 성공 (status: $REFRESH_STATUS)"
elif [ "$REFRESH_STATUS" = "401" ]; then
  fail "401 반환 — 자동 갱신 실패. 게이트웨이 로그를 확인하세요."
else
  fail "예상치 못한 응답 (status: $REFRESH_STATUS)"
fi

# 새 accessToken 쿠키 확인
if [ -n "$NEW_COOKIE_HEADER" ]; then
  ok "Set-Cookie: accessToken 헤더 응답에 포함 (브라우저 쿠키 갱신됨)"
  info "  $NEW_COOKIE_HEADER"
fi

# =============================================================================
# 7. 수동 refresh 엔드포인트 직접 호출
# =============================================================================
sep
info "7/7  POST /api/v1/auth/refresh 직접 호출 확인"
sep

MANUAL_REFRESH=$(curl -s -b "$COOKIE_JAR" -w "\n%{http_code}" \
  -X POST "$GATEWAY/api/v1/auth/refresh")

MANUAL_STATUS=$(http_status "$MANUAL_REFRESH")
MANUAL_BODY=$(echo "$MANUAL_REFRESH" | head -n-1)
NEW_ACCESS=$(extract "$MANUAL_BODY" "accessToken")

if [ "$MANUAL_STATUS" = "200" ] && [ -n "$NEW_ACCESS" ]; then
  ok "수동 refresh 성공 (200)"
  info "  새 accessToken 앞 30자: ${NEW_ACCESS:0:30}..."
elif [ "$MANUAL_STATUS" = "401" ]; then
  info "refreshToken도 만료됨 (60초 초과 시 정상) → 재로그인 필요"
else
  fail "예상치 못한 응답 (status: $MANUAL_STATUS)"
  echo "$MANUAL_BODY"
fi

# =============================================================================
# 최종 요약
# =============================================================================
sep
echo ""
echo -e "${GREEN}테스트 완료${NC}"
echo ""
echo "추가로 확인할 수 있는 명령어:"
echo "  redis-cli KEYS 'auth:*'                  # Redis 전체 토큰 키 조회"
echo "  redis-cli TTL 'auth:refresh:<UUID>'       # 리프레시 토큰 남은 TTL"
echo "  redis-cli GET 'auth:refresh:<UUID>'       # 리프레시 토큰 → sessionId 확인"
echo ""
echo "게이트웨이 로그 확인:"
echo "  grep 'JwtGlobalFilter' 게이트웨이_로그 | grep -E '만료|갱신|통과'"
sep

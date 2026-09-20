# n8n 워크플로우

AI/수집 파이프라인 오케스트레이터입니다. 실행 엔진이 아니라 **실험대**로 쓰는 것이 전제입니다.

## 경계선

| | 담당 |
|---|---|
| 스케줄, 재시도, 실행 이력, 프롬프트 튜닝 | n8n |
| 비즈니스 로직, DB 읽기/쓰기, 유저 응답 경로 | Java(portal) / Python(collector) |

**n8n은 MySQL에 직접 붙지 않습니다.** collector의 내부 HTTP API(`/internal/*`, `X-System-Key`)로만
접근합니다. 테이블 소유자를 하나로 유지하지 않으면 스키마를 바꿀 때 컴파일러가 안 잡아주는 쪽이
조용히 깨집니다.

유저 요청을 블로킹하는 동기 경로(운세, 채팅 응답 등)에도 n8n을 넣지 않습니다. 홉만 늘고 얻는 게
없습니다.

## 실행

```bash
# .env 에 N8N_ENCRYPTION_KEY, SYSTEM_API_KEY, FRED_API_KEY 를 채운 뒤
docker compose -f docker-compose.n8n.yml up -d
docker compose -f docker-compose.n8n.yml logs -f n8n
```

`http://localhost:5678` 접속 → owner 계정 생성.

`desktop_default` 네트워크가 없으면 먼저 만드세요:

```bash
docker network create desktop_default
```

## 워크플로우 임포트

컨테이너에 `n8n/workflows` 가 `/workflows` 로 마운트되어 있습니다.

```bash
docker exec -it kwak-n8n n8n import:workflow --separate --input=/workflows
docker restart kwak-n8n
```

임포트된 워크플로우는 **비활성 상태**로 들어옵니다. UI에서 내용을 확인하고 수동 실행
(Execute workflow)으로 한 번 검증한 뒤 활성화하세요.

> 노드 파라미터 스키마는 n8n 버전에 따라 조금씩 달라집니다. 임포트 후 각 노드를 열어
> 경고 표시가 없는지 확인하세요. 특히 `원문 요청` 노드의 Response 옵션(text / full response /
> never error)과 `기사별 처리`(Loop Over Items)의 done/loop 분기 연결을 확인하면 됩니다.

## 내보내기 규율 — 반드시 지킬 것

n8n의 최대 장점(배포 없이 프롬프트·파이프라인 수정)이 그대로 최대 위험입니다. 워크플로우를
n8n 안에만 두면 **누가 언제 왜 바꿨는지가 git 밖으로 사라집니다.**

UI에서 워크플로우를 수정했으면 그날 안에 내보내서 커밋하세요:

```bash
docker exec -it kwak-n8n n8n export:workflow --all --separate --output=/tmp/wf
docker cp kwak-n8n:/tmp/wf/. ./n8n/workflows/
git add n8n/workflows && git commit -m "chore(n8n): 워크플로우 변경 반영"
```

내보낸 JSON에는 credential **참조(id/name)** 만 들어가고 실제 키 값은 포함되지 않습니다.
그래도 커밋 전에 diff는 확인하세요.

## 워크플로우 목록

### `news-content-enrich`

2시간마다 본문이 없는 기사를 가져와 원문을 받아오고 텍스트를 추출해 기록합니다.

```
2시간마다
  → 본문 필요 기사 조회   GET  /internal/news/pending-content
  → 기사별 처리(loop)
      → 원문 요청          GET  기사 URL (20s 타임아웃, 2회 재시도, 에러도 계속 진행)
      → 본문 추출          script/style 제거 → <article> 우선 → 태그 제거
      → 결과 기록          POST /internal/news/content
  → (done) 수집 현황 확인  GET  /internal/news/content-stats
```

상태값은 `collector/news_crawler/content_store.py` 와 공유합니다:
`OK` / `EMPTY`(본문 400자 미만) / `BLOCKED`(403·404·페이월) / `FAILED`(타임아웃 등, 재시도 대상) /
`UNRESOLVED`.

**알려진 한계 — 성공률은 100%가 나오지 않습니다.**
Google News RSS의 링크는 원문이 아니라 `news.google.com` 리다이렉트 래퍼라 본문을 받을 수
없어서 큐에서 아예 제외했습니다(`UNFETCHABLE_HOSTS`). 그래서 `collector`의 RSS 피드에
CNBC·Yahoo Finance 직접 피드를 추가했습니다 — 이쪽은 원문 URL을 그대로 줍니다.
언론사에 따라 페이월·봇 차단으로 `BLOCKED`가 나오는 건 정상이며, `content-stats` 로 비율을
보고 판단하세요. `OK` 비율이 30% 아래로 계속 머무르면 RSS 피드를 갈아치우거나 정식 뉴스 API를
검토할 시점입니다.

### `fred-macro-sync`

매일 07시(KST) FRED에서 거시지표를 받아 `macro_indicator` 에 UPSERT 합니다.

```
매일 07시
  → 시리즈 목록        DFF / DGS10 / DGS2 / CPIAUCSL / UNRATE
  → FRED 조회          GET api.stlouisfed.org (3회 재시도)
  → 관측치 변환·분할    2000건씩 분할
  → 거시지표 저장       POST /internal/macro/observations
```

UPSERT라 재실행해도 안전합니다. `FRED_START_DATE` 를 과거로 두면 첫 실행이 백필이 됩니다
(기본 2015-01-01). API 키는 https://fred.stlouisfed.org/docs/api/api_key.html 에서 무료 발급.

시리즈를 추가하려면 `collector/macro/store.py` 의 `KNOWN_SERIES` 와 이 워크플로우의
`시리즈 목록` 노드를 **둘 다** 고쳐야 합니다. 목록 밖 시리즈는 내부 API가 400으로 거절합니다 —
오타 하나로 쓰레기 시리즈가 조용히 쌓이면 나중에 이 테이블을 신뢰할 수 없게 되기 때문입니다.

## 판단 기준

도입 3주 뒤 **"프롬프트나 파이프라인을 배포 없이 실제로 몇 번 고쳤는가"** 를 셉니다.
0~1회면 n8n은 걷어내고 크론으로 돌아가는 게 맞습니다.

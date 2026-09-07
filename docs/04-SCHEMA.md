# 스키마 초안

`docs/00-DECISIONS.md`의 D-001 ~ D-033에서 **기계적으로 도출한** 결과다.
여기 있는 모든 컬럼은 결정 하나를 근거로 가진다. 근거 없는 컬럼은 없어야 한다.

> **아직 확정 아님.** 확인 후 `02-ARCHITECTURE.md`에 반영한다.

---

## 1. 카탈로그

### language  ← D-036
```
id      smallint  PK
code    varchar(10)  UNIQUE NOT NULL    KO | EN | JA | …
name    varchar(50)  NOT NULL
```
- 언어 추가가 **스키마 변경이 아니라 행 추가**가 된다 (D-036).
- 곡 이름(D-040)과 보컬 이름(D-041)이 이 테이블을 공유한다.

### producer  ← D-013
```
id          bigint  PK
name        varchar(100)  NOT NULL    원어 표기. wowaka, DECO*27
created_at  timestamp
```
- **언어별 이름을 두지 않는다** (D-013). 보컬과 다르게 가는 이유는 D-041에 적혀 있다.

### vocal / vocal_name / song_vocal  ← D-037, D-041
```
vocal
  id          bigint  PK
  code        varchar(50)  UNIQUE NOT NULL    HATSUNE_MIKU 등. 시드·디버깅용 안정 식별자
  created_at  timestamp

vocal_name
  id           bigint  PK
  vocal_id     bigint    FK → vocal
  language_id  smallint  FK → language
  name         varchar(100)  NOT NULL
  is_primary   boolean  NOT NULL DEFAULT false
  UNIQUE (vocal_id, language_id, name)

song_vocal
  song_id   bigint  FK → song
  vocal_id  bigint  FK → vocal
  PK (song_id, vocal_id)
```
- 듀엣·합창곡 때문에 다대다 (D-037).
- 데이터 출처는 vocaloard (D-035) — YouTube Data API는 보컬을 알려주지 않는다.

### channel  ← D-008, D-031, D-034
```
id                        bigint  PK
youtube_channel_id        varchar(64)  UNIQUE NOT NULL    UCxxxx
uploads_playlist_id       varchar(64)  NULL    ← channels.list contentDetails (D-034)
name                      varchar(200)
producer_id               bigint  FK → producer  NULL
watch                     boolean  NOT NULL DEFAULT false   ← D-031: 기본 꺼짐
last_video_published_at   timestamp  NULL     RSS 폴링 워터마크
last_checked_at           timestamp  NULL
created_at                timestamp
```
- 곡 등록 시 자동 기록되지만 `watch`는 사람이 켠다 (D-031).
- `uploads_playlist_id`가 있으면 `playlistItems.list`로 전량 페이징이 가능하다 (D-034).
  다만 전량을 통째로 넣지는 않는다 (D-039).

### song  ← D-006, D-008, D-036, D-040, D-044
```
id                     bigint  PK
original_language_id   smallint  FK → language  NOT NULL   ← 제목이 어느 언어인지 (D-040)
lyrics_language_id     smallint  FK → language  NOT NULL   ← 가사가 어느 언어인지 (D-044)
status                 varchar(20)  NOT NULL    DRAFT | PUBLISHED
search_keywords        text  NOT NULL           ← 파생 컬럼. song_name 저장 시 갱신
created_at, updated_at
INDEX (lyrics_language_id)
```
- **제목 컬럼이 하나도 없다.** 전부 `song_name` 행이다 (D-040).
- **언어 컬럼이 두 개인 이유 (D-044):**

  | 컬럼 | 의미 | 쓰이는 곳 |
  |---|---|---|
  | `original_language_id` | **제목**의 원어 | 표시 시 원제 병기 (D-007) |
  | `lyrics_language_id` | **가사**의 언어 | 범위지정 필터 축 (D-044) |

  보통 같은 값이지만 다를 수 있다 (영어 제목의 일본어 곡 등).
  가사 언어 필터는 유명곡 일부가 잘려나가는 대신 난이도 일관성을 얻는 의도된 교환이다.
- **`view_count` 컬럼 없음** — 조회수는 영상 단위이므로 `video`에 둔다.

### song_name  ← D-040
```
id           bigint  PK
song_id      bigint    FK → song  NOT NULL
language_id  smallint  FK → language  NOT NULL
name         varchar(300)  NOT NULL
normalized   varchar(300)  NOT NULL   NFKC → 소문자 → 기호제거 → 가타카나→히라가나
is_primary   boolean  NOT NULL DEFAULT false
created_at   timestamp
UNIQUE (song_id, language_id, name)
UNIQUE (song_id, language_id) WHERE is_primary   ← 언어당 대표 이름 1개
INDEX (normalized)
```
- `is_primary = true` → 그 언어의 대표 표시 제목
- `is_primary = false` → 별칭 (약칭, 로마자, 통용 표기)
- **`song_alias` 테이블은 없앴다** (D-040). 정규화 로직이 한 곳에만 존재한다.
- 검색은 이 테이블 **전체**를 본다 (D-007: 전 언어 매칭). 표시는 `is_primary`만 본다.
- `search_keywords`(파생)는 이 테이블의 `normalized`를 이어붙인 것.
  자동완성 API가 그것만 내려주면 되므로 조회 시 join이 없다.

### song_producer  ← D-013
```
song_id      bigint  FK → song
producer_id  bigint  FK → producer
PK (song_id, producer_id)
```

### video  ← D-006, D-010, D-034
```
id                bigint  PK
song_id           bigint  FK → song  NOT NULL
channel_id        bigint  FK → channel  NULL
youtube_video_id  varchar(32)  UNIQUE NOT NULL
kind              varchar(32)  NOT NULL    ORIGINAL | MV | SELF_COVER | COVER | LIVE | OTHER
playable          boolean  NOT NULL DEFAULT true   ← D-010: 출제 필터는 이것만 본다
duration_sec      int  NOT NULL       ← videos.list contentDetails.duration (D-034)
view_count        bigint  NULL         ← videos.list statistics.viewCount (D-034)
stats_updated_at  timestamp  NULL      조회수 배치 갱신 시각
title_snapshot    varchar(300)  NULL   videos.list snippet.title 원문. 검수·추적용
published_at      timestamp  NULL
created_at        timestamp
INDEX (song_id)
```
- `duration_sec`가 **NOT NULL이 되었다.** D-034로 서버가 직접 확보하므로
  클라이언트 `getDuration()` 보고에 의존하지 않는다.
- `view_count`는 하루 1회 배치로 갱신한다 (`videos.list`, id 50개당 1 unit).
  A vs B 대결(D-009)이 이 컬럼을 쓴다.

## 2. 출제 데이터

### segment  ← D-017, D-020, D-024, D-045
**이 프로젝트에서 가장 중요한 테이블.** 여기 행이 없으면 그 곡은 출제되지 않는다 (D-017).

```
id              bigint  PK
video_id        bigint  FK → video  NOT NULL
kind            varchar(32)  NOT NULL   ← DB는 varchar, 서버는 Java enum (D-045)
start_sec       numeric(8,2)  NOT NULL  소수점. 장면 시점 때문 (D-025)
end_sec         numeric(8,2)  NULL      한 점이면 null
structure_tag   varchar(20)   NULL      INTRO|VERSE|PRE_CHORUS|CHORUS|INTERLUDE|OUTRO (D-024)
scene_purpose   varchar(32)   NULL      장면 용도 (D-026, 값 미정 — O-30)
created_at      timestamp
INDEX (video_id)
INDEX (kind, structure_tag)
```

- **JSON 컬럼이 없다** (D-045). 속성이 2~3개로 유한하므로 컬럼이 맞다.
- `kind`가 varchar인 이유: 값이 늘어도 DDL이 필요 없게. **서버에서는 enum으로 다룬다** —
  DB 유연성과 코드 타입 안전성을 둘 다 가진다.
- 오디오 구간: `end_sec` 있음, 길이 5~20초 (D-017), `structure_tag` 채움
- 장면: `end_sec = null`, `scene_purpose` 채움

**JSON을 쓰는 곳과 안 쓰는 곳의 기준 (D-045):**

| 대상 | 저장 | 이유 |
|---|---|---|
| `segment` 속성 | **컬럼** | 유한하고 예측 가능 |
| `round.payload` / `progress` | **JSON** | 퀴즈 유형마다 다르고 무한히 늘어남 (D-005) |

> **"유한하고 예측 가능하면 컬럼, 무한하고 유형마다 다르면 JSON."**

---

## 3. 자동 파이프라인

### ingest_item  ← D-008, D-011, D-022
채널 RSS에서 발견된 신규 영상의 **검수 대기열**.

```
id                bigint  PK
channel_id        bigint  FK → channel  NOT NULL
youtube_video_id  varchar(32)  UNIQUE NOT NULL
raw_title         varchar(300)  NOT NULL    oEmbed 제목 원문
published_at      timestamp
status            varchar(32)  NOT NULL
                  PENDING | APPROVED_NEW_SONG | APPROVED_ADD_VIDEO | REJECTED
ai_verdict        json  NULL
resolved_song_id  bigint  FK → song  NULL   승인 시 어느 곡에 붙었는지
reviewed_at       timestamp  NULL
created_at        timestamp
INDEX (status, created_at)
```

`ai_verdict` 예시 — **AI는 후보만 제시하고 판단하지 않는다 (D-011):**
```jsonc
{
  "isSong": true,
  "confidence": 0.86,
  "titleOriginal": "千本桜",
  "titleKo": "센보자쿠라",
  "titleEn": "Senbonzakura",
  "producerGuess": "黒うさP",
  "similarSongIds": [42, 88],
  "reason": "제목이 기존 곡 #42와 유사"
}
```

- **파이프라인은 여기까지다 (D-022).** 승인되어 `song`/`video`가 생겨도
  사람이 `segment`를 찍기 전까지는 출제되지 않는다.

---

## 4. 사용자 / 인증

### app_user  ← D-032
```
id                bigint  PK
provider          varchar(20)  NOT NULL    GOOGLE
provider_user_id  varchar(100) NOT NULL
email             varchar(200)
role              varchar(20)  NOT NULL DEFAULT 'USER'    USER | ADMIN
created_at        timestamp
UNIQUE (provider, provider_user_id)
```
- `/admin/**`은 `role = ADMIN`만 (D-032).
- 게임 플레이는 로그인 없이 가능. Phase 2 데일리에서만 필수가 된다.

---

## 5. 게임

### game  ← D-005
```
id            uuid  PK
mode          varchar(20)  NOT NULL    DAILY | RANDOM | CUSTOM
quiz_type     varchar(32)  NOT NULL
owner_key     varchar(100) NOT NULL    익명=세션ID, 로그인="user:{id}"
total_score   int  NOT NULL DEFAULT 0
status        varchar(20)  NOT NULL    PLAYING | FINISHED
created_at, finished_at
INDEX (owner_key, created_at)
```

### round  ← D-005, D-018, D-023
```
id               bigint  PK
game_id          uuid  FK → game  NOT NULL
round_no         int   NOT NULL
quiz_type        varchar(32)  NOT NULL
payload          json  NOT NULL    출제 데이터 + 정답. ★ 절대 클라이언트로 안 나감 ★
progress         json  NOT NULL    유형 내부 진행 상태
status           varchar(20)  NOT NULL    PLAYING | SOLVED | FAILED
score            int   NOT NULL DEFAULT 0    ← 반드시 0..100 (D-005)
primary_song_id  bigint  FK → song  NULL     통계용 비정규화. 채점에 쓰지 않는다
started_at, finished_at
UNIQUE (game_id, round_no)
```

`payload` / `progress` 예시:
```jsonc
// 구간 퀴즈 — 통째로 한 번 재생 (D-018)
payload  {"songId":42,"segmentId":301,"videoId":"abc","startSec":47.0,"endSec":59.0}
progress {"attempts":1,"usedHints":["PRODUCER"],"replays":2}

// 인트로 퀴즈 — 0.5초부터 점점 길게 (D-018)
payload  {"songId":42,"videoId":"abc"}
progress {"stage":2}
```

- 채점 방식(`SINGLE_SHOT`/`TIME_BASED`/`ATTEMPT_BASED`/`HINT_BASED`, D-023)은
  **컬럼이 아니다.** `QuizType` 구현체가 소유하므로 DB에 둘 필요가 없다.
- 힌트(D-029)는 별도 테이블 없이 `progress.usedHints`에 기록한다.
  힌트 콘텐츠는 이미 가진 메타데이터라 저장할 것이 없기 때문.

### attempt
```
id          bigint  PK
round_id    bigint  FK → round  NOT NULL
seq         int   NOT NULL
answer      json  NULL    유형별 답안 원본. null이면 패스
correct     boolean  NOT NULL
created_at  timestamp
INDEX (round_id)
```

---

## 6. 출제 쿼리 (D-020 순서)

```sql
-- 1) 후보 곡: PUBLISHED + 재생 가능한 영상에 해당 종류의 구간이 있는 곡
SELECT DISTINCT s.id
FROM song s
JOIN video v   ON v.song_id = s.id AND v.playable = true
JOIN segment g ON g.video_id = v.id
WHERE s.status = 'PUBLISHED'
  AND g.kind = :quizKind
  [AND g.structure_tag = :tag]                      -- "후렴만 출제" (D-028)
  [AND EXISTS (SELECT 1 FROM song_producer sp
               WHERE sp.song_id = s.id AND sp.producer_id IN (:producerIds))]   -- D-013
  [AND EXISTS (SELECT 1 FROM song_vocal sv
               WHERE sv.song_id = s.id AND sv.vocal_id IN (:vocalIds))]         -- D-037
  [AND s.lyrics_language_id IN (:lyricsLanguageIds)]                            -- D-044
```
→ 셔플 후 N곡 선택 (게임 내 중복 방지는 여기서, D-020)
→ 곡마다 영상 랜덤 → 그 영상의 구간 랜덤

**자동완성 목록** (D-007, D-040) — join 없음:
```sql
SELECT s.id, s.search_keywords,
       (SELECT name FROM song_name
        WHERE song_id = s.id AND language_id = :uiLang AND is_primary) AS display,
       (SELECT name FROM song_name
        WHERE song_id = s.id AND language_id = s.original_language_id AND is_primary) AS original
FROM song s WHERE s.status = 'PUBLISHED'
```
→ 앱 시작 시 한 번 받아 캐시. 표시는 `display (original)`, 검색은 `search_keywords`.

## 7. 이 스키마에 **없는** 것 (의도적)

| 없는 것 | 근거 |
|---|---|
| `song.view_count` | 조회수는 영상 단위 → `video.view_count` (D-034) |
| `song.video_id` | D-006 — 영상은 1:N |
| `song_alias` 테이블 | D-040 — `song_name`으로 통합 |
| `song.title_ko` / `title_en` | D-040 — `song_name` 행으로 |
| `producer.name_ko` | D-013 — 원어 표기 하나만 (보컬과 다름, 근거는 D-041) |
| `round.hint_level` | D-018 — 점진 방식은 인트로 퀴즈 전용 |
| 힌트 콘텐츠 테이블 | D-029 — 이미 가진 메타데이터만 쓴다 |
| `segment.attributes` JSON | D-045 — 속성이 유한하므로 컬럼이 맞다 |
| 장면 이미지 저장소 | D-042 — seek 방식을 먼저 만들고 보류 |
| `search.list` 사용 | D-034 — 호출당 100 units. 코드 어디에도 쓰지 않는다 |

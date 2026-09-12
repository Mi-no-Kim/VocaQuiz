# 스키마

**테이블 정의의 진실의 원천이다.** `02-ARCHITECTURE.md`는 테이블을 적지 않고 이 문서를 가리킨다.

여기 있는 모든 컬럼은 `docs/00-DECISIONS.md`의 결정 하나를 근거로 가진다. 근거 없는 컬럼은 없어야 한다.
결정이 바뀌면 결정 로그를 먼저 고치고 이 문서를 맞춘다.

---

## 1. 카탈로그

> **조인 테이블도 대리 키 `id`를 갖는다.** 복합 PK 대신 UNIQUE 제약으로 같은 조합을 막는다.
> 제약은 같고 강제 수단만 옮긴 것이다. JPA 매핑을 단순하게 두기 위한 선택이다.

### language ← D-036

```
id      bigint  PK
code    varchar(10)  UNIQUE NOT NULL    KO | EN | JA | …
name    varchar(50)  NOT NULL
```

- 언어 추가가 **스키마 변경이 아니라 행 추가**가 된다 (D-036).
- 곡 이름(D-040)과 보컬 이름(D-041)이 이 테이블을 공유한다.

### producer ← D-013, D-053, D-063

```
id          bigint  PK
name        varchar(100)  UNIQUE NOT NULL    원어 표기. wowaka, DECO*27  ← D-063
created_at  timestamp
```

- **언어별 이름을 두지 않는다** (D-013). 보컬과 다르게 가는 이유는 D-041에 적혀 있다.
- 곡·영상과의 연결은 `song_credit` / `video_credit`이 맡는다 (D-053). `song_producer`는 없앴다.
- `name`은 유일하다 (D-063). 관리자 화면은 자동완성으로 기존 행을 고르게 하고, 없을 때만 만든다.
  비교는 `utf8mb4_bin`이라 `DECO*27`과 `deco*27`은 서로 다른 이름이다 (D-057).

### vocal / vocal_name ← D-037, D-041, D-064

```
vocal
  id          bigint  PK
  code        varchar(50)  UNIQUE NOT NULL    HATSUNE_MIKU 등. 시드·디버깅용 안정 식별자
  created_at  timestamp

vocal_name
  id           bigint  PK
  vocal_id     bigint    FK → vocal
  language_id  bigint    FK → language
  name         varchar(100)  NOT NULL
  is_primary   boolean  NOT NULL DEFAULT false
  created_at   timestamp
  UNIQUE (vocal_id, language_id, name)
```

- YouTube Data API는 보컬을 알려주지 않는다. Phase 1은 코드로 시드한 목록에서 사람이 고르고 (D-064), vocaloard(D-035)는 Phase 3의 출처다.
- 보컬 이름은 아무 언어로 1개 이상이면 된다. 보여 줄 언어의 이름이 없으면 `code`를 보여 준다 (D-064).

### video_vocal ← D-050

```
id        bigint  PK
video_id  bigint  FK → video  NOT NULL
vocal_id  bigint  FK → vocal  NOT NULL
UNIQUE (video_id, vocal_id)
```

- **진실.** 이 영상에서 실제로 부른 보컬이다.
- 보컬은 곡이 아니라 음원의 속성이다. 셀프 커버는 보컬이 다르고, 커버는 사람이 부르기도 한다.

### song_vocal ← D-050

```
id        bigint  PK
song_id   bigint  FK → song  NOT NULL
vocal_id  bigint  FK → vocal  NOT NULL
UNIQUE (song_id, vocal_id)
```

- **파생.** 그 곡의 `kind = ORIGINAL` 영상들이 가진 `video_vocal`의 합집합이다.
- 힌트와 범위지정 필터가 이 테이블을 읽는다. join 없이 곡 단위로 끝난다.
- 갱신은 `video_vocal` 쓰기 경로 한 곳에 모은다. 그 영상이 ORIGINAL이면 다시 계산한다.
- 정합성 점검을 관리자에게 노출한다. ORIGINAL 영상이 없는 곡은 점검 대상에서 제외한다.

### channel ← D-008, D-031, D-034, D-054

```
id                        bigint  PK
youtube_channel_id        varchar(64)  UNIQUE NOT NULL    UCxxxx
uploads_playlist_id       varchar(64)  NULL    ← channels.list contentDetails (D-034)
name                      varchar(200)
watch                     boolean  NOT NULL DEFAULT false   ← D-031: 기본 꺼짐
last_video_published_at   timestamp  NULL     RSS 폴링 워터마크
last_checked_at           timestamp  NULL
created_at                timestamp
```

- 곡 등록 시 자동 기록되지만 `watch`는 사람이 켠다 (D-031).
- `producer_id`를 뺐다. 합작 채널 때문에 다대다로 간다 (D-054).
- `uploads_playlist_id`는 Phase 3까지 NULL이다. 채우는 수단인 `channels.list`가 Phase 3의 일이다.
  이 값이 있으면 `playlistItems.list`로 전량 페이징이 가능하다 (D-034). 다만 전량을 통째로 넣지는 않는다 (D-039).

### channel_producer ← D-054

```
id           bigint  PK
channel_id   bigint  FK → channel  NOT NULL
producer_id  bigint  FK → producer  NOT NULL
UNIQUE (channel_id, producer_id)
```

- 합작 채널과 한 프로듀서의 여러 채널을 담는다.
- 조인 엔티티로 만들고 양쪽에서 `@OneToMany`로 탐색한다.
- 대표 프로듀서 표시는 두지 않는다.

### song ← D-006, D-040, D-051, D-062

```
id                     bigint  PK
original_language_id   bigint  FK → language  NOT NULL   ← 원제가 어느 언어인가 (D-040)
status                 varchar(20)  NOT NULL    DRAFT | PUBLISHED  ← PUBLISHED 전환 조건 (D-062)
created_at, updated_at
```

- **제목 컬럼이 하나도 없다.** 전부 `song_name` 행이다 (D-040).
- `lyrics_language_id`를 뺐다. 곡의 언어는 `song_language`로 간다 (D-051).
- `search_keywords`를 뺐다. `song_answer`가 그 자리를 대신한다 (D-052, D-056).
- `original_language_id`는 남는다. 표시할 때 원제를 병기하려면 어느 이름이 원제인지 알아야 한다 (D-007).
- **`view_count` 컬럼 없음** — 조회수는 영상 단위이므로 `video`에 둔다.
- PUBLISHED로 바꾸려면 원제 대표 이름 · 정답 패턴 · 수집된 ORIGINAL 영상이 있어야 한다 (D-062). 출제 쿼리(§6)는 PUBLISHED만 본다.

### song_language ← D-051

```
id           bigint    PK
song_id      bigint    FK → song  NOT NULL
language_id  bigint    FK → language  NOT NULL
UNIQUE (song_id, language_id)
```

- 이 곡이 무슨 언어로 불리는가. 범위지정 필터 축이다.
- 일본어와 한국어가 섞인 곡은 행을 둘 갖고, 두 필터 모두에 걸린다.
- 옛 `lyrics_language_id`가 여기로 통합됐다. 곡 자체의 언어가 가사 언어보다 포괄적이다.

### song_name ← D-040, D-056, D-061

```
id           bigint  PK
song_id      bigint    FK → song  NOT NULL
language_id  bigint    FK → language  NOT NULL
name         varchar(300)  NOT NULL
is_primary   boolean  NOT NULL DEFAULT false
created_at   timestamp
UNIQUE (song_id, language_id, name)
```

- **보여주기 전용이다** (D-056). 정답 판정은 이 테이블을 보지 않는다.
- `is_primary = true` → 그 언어의 대표 표시 제목. `false` → 별칭 (약칭, 로마자, 통용 표기).
- **`song_alias` 테이블은 없앴다** (D-040).
- `normalized`와 `answer_pattern`을 뺐다. 정답 쪽은 `song_answer_pattern`으로 갈렸다 (D-056).
- **이름이 있는 언어마다 대표 이름은 1개, 원제 언어에는 대표 필수 (D-061).** 서비스가 검증한다.
  `UNIQUE (song_id, language_id) WHERE is_primary`는 MySQL도 H2도 지원하지 않아 DB 제약으로는 걸 수 없다.

### song_answer_pattern ← D-052, D-056

```
id          bigint  PK
song_id     bigint  FK → song  UNIQUE NOT NULL
pattern     varchar(2000)  NOT NULL
created_at, updated_at
```

- **진실.** 사람이 쓰고 고치는 정답 패턴 원문이다.
- **곡당 한 행이다** (`song_id` UNIQUE). 여러 표기는 한 텍스트 안에 줄로 나열한다.

```
(히토|인간|사람)마니아
HITO Mania
人マニア
```

- 줄은 `\R`로 자르고 각 줄을 `strip()`하며 빈 줄은 버린다.
- 문법은 괄호와 파이프만. 문자 자체가 필요하면 `\(` `\|` `\)` `\\`로 이스케이프한다.
- **언어를 구분하지 않는다** (D-056). 정답 판정에 언어가 필요하지 않다.
- 한 줄이라도 문법이 틀리면 **전체가 실패한다.** 몇 번째 줄인지 알려 준다.
  조용히 한 줄만 빠지면 검색되지 않는 곡이 생기고 아무도 모른다.
- 전개가 20개를 넘을 것으로 보이면 관리자 입력 창에서 경고한다. 저장은 막지 않는다.
- **곡 조회에 딸려 오지 않는다.** 테이블을 나눈 이유가 그것이다 — 정답 원문이 실수로
  응답에 섞이는 경로를 만들지 않는다 (CLAUDE.md §1.1).

### song_answer ← D-052, D-056

```
id          bigint  PK
song_id     bigint  FK → song  NOT NULL
normalized  varchar(300)  NOT NULL   NFKC → 소문자 → 공백·기호 제거
created_at  timestamp
UNIQUE (song_id, normalized)
INDEX (normalized)
```

- **파생.** `song_answer_pattern.pattern`을 전개하고 정규화한 결과다.
  원문이 바뀌면 그 곡의 행을 통째로 다시 만든다.
- `UNIQUE (song_id, normalized)`가 곡 단위 중복을 막는다. 서로 다른 줄이 같은 결과를 내도
  한 행만 남으므로 **조회에서 DISTINCT가 필요 없다.**
- 비교는 `utf8mb4_bin`이다 (D-057). `ミク`와 `みく`는 다른 값이라 둘 다 남는다.
- 정답창에 입력하면 이 테이블로 후보를 띄우고 사용자가 고른다.
  **판정은 여전히 songId 비교다** (02-ARCHITECTURE §7).
- 화면에 보여줄 이름은 `song_name`이다. 전개 결과를 목록에 섞지 않는다.
- 옛 `song.search_keywords`가 하던 일을 이 테이블이 한다.
- 기호는 문자만 지운다. 괄호 안 내용은 남는다 — `千本桜(feat. 初音ミク)` → `千本桜feat初音ミク`

### song_credit ← D-053

```
id           bigint  PK
song_id      bigint  FK → song  NOT NULL
producer_id  bigint  FK → producer  NOT NULL
role         varchar(32)  NOT NULL   ← DB는 varchar, 서버는 enum (D-045와 같은 방식)
UNIQUE (song_id, producer_id, role)
```

- 옛 `song_producer`를 대신한다. 다대다와 원어 표기 규칙은 D-013 그대로다.
- UNIQUE에 `role`이 들어간 이유는 한 사람이 한 곡에서 두 역할을 맡을 수 있어서다.
- **지금은 role 값을 하나만 쓴다.** 관리자 화면은 이름만 받고 role을 자동으로 채운다.
- 값이 늘어도 DDL이 필요 없다. 역할 값의 목록은 아직 정하지 않았다 — O-32.

### video ← D-006, D-010, D-034, D-055, D-059, D-060

```
id                bigint  PK
song_id           bigint  FK → song  NULL      ← 곡 없는 영상이 있다 (D-059)
channel_id        bigint  FK → channel  NULL
youtube_video_id  varchar(32)  UNIQUE NOT NULL
kind              varchar(32)  NOT NULL    ORIGINAL | MV | SELF_COVER | COVER | LIVE | OTHER
playable          boolean  NOT NULL DEFAULT true   ← D-010: 출제 필터는 이것만 본다
(수집 상태)       varchar(20)  NOT NULL   미수집 | 수집됨 | 실패 (D-060). 이름은 P1-3-3에서 정한다
duration_sec      int  NULL           ← videos.list contentDetails.duration (D-034). 수집 전에는 NULL (D-060)
view_count        bigint  NULL         ← videos.list statistics.viewCount (D-034)
stats_updated_at  timestamp  NULL      조회수 배치 갱신 시각
title_snapshot    varchar(300)  NULL   videos.list snippet.title 원문. 검수·추적용
published_at      timestamp  NULL
created_at        timestamp
INDEX (song_id)
```

- 컬럼은 그대로다. 다만 `kind`가 하는 일이 하나 늘었다.
  출제 대상을 `kind = ORIGINAL`로 제한해 리믹스 정답의 어색함을 피한다 (D-055).
- `song_vocal` 재계산도 이 컬럼을 본다 (D-050).
- `view_count`는 하루 1회 배치로 갱신한다 (`videos.list`, id 50개당 1 unit).
- 곡 없이도 존재한다 (D-059). 곡 편집이나 영상 편집에서 곡과 연결한다.
- URL을 넣으면 미수집으로 생기고, 일괄 수집이 `duration_sec` · `published_at` · `title_snapshot` · `view_count` · `channel_id`를 채운다 (D-060). 수집 전 영상에는 구간을 찍지 않는다.

### video_credit ← D-053

```
id           bigint  PK
video_id     bigint  FK → video  NOT NULL
producer_id  bigint  FK → producer  NOT NULL
role         varchar(32)  NOT NULL
UNIQUE (video_id, producer_id, role)
```

- 리믹서와 커버 제작자가 여기에 들어간다. 곡이 아니라 그 영상의 크레딧이다.
- 원곡 영상이라도 곡 크레딧을 **자동으로 상속하지 않는다.**
  관리자 화면의 "곡 크레딧과 동일" 버튼으로 복사한다. 자동 상속은 캐시를 하나 더 만드는 일이다.

## 2. 출제 데이터

### segment ← D-017, D-020, D-024, D-045

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

| 대상                         | 저장     | 이유                                       |
| ---------------------------- | -------- | ------------------------------------------ |
| `segment` 속성               | **컬럼** | 유한하고 예측 가능                         |
| `round.payload` / `progress` | **JSON** | 퀴즈 유형마다 다르고 무한히 늘어남 (D-005) |

> **"유한하고 예측 가능하면 컬럼, 무한하고 유형마다 다르면 JSON."**

---

## 3. 자동 파이프라인

### ingest_item ← D-008, D-011, D-022

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
  "reason": "제목이 기존 곡 #42와 유사",
}
```

- **파이프라인은 여기까지다 (D-022).** 승인되어 `song`/`video`가 생겨도
  사람이 `segment`를 찍기 전까지는 출제되지 않는다.

---

## 4. 사용자 / 인증

### app_user ← D-032

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

### game ← D-005

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

### round ← D-005, D-018, D-023

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
JOIN video v   ON v.song_id = s.id AND v.playable = true AND v.kind = 'ORIGINAL'
JOIN segment g ON g.video_id = v.id
WHERE s.status = 'PUBLISHED'
  AND g.kind = :quizKind
  [AND g.structure_tag = :tag]                      -- "후렴만 출제" (D-028)
  [AND EXISTS (SELECT 1 FROM song_credit sc
               WHERE sc.song_id = s.id AND sc.producer_id IN (:producerIds))]   -- D-053
  [AND EXISTS (SELECT 1 FROM song_vocal sv
               WHERE sv.song_id = s.id AND sv.vocal_id IN (:vocalIds))]         -- D-050
  [AND EXISTS (SELECT 1 FROM song_language sl
               WHERE sl.song_id = s.id AND sl.language_id IN (:languageIds))]   -- D-051
```

`v.kind = 'ORIGINAL'` 조건이 D-055다. 리믹스 영상을 내면 정답이 원곡 제목이라 어색해지므로
출제 자체를 원곡으로 제한한다. 리믹스를 일부러 내는 유형이 생기면 그 유형이 조건을 다시 정한다.

→ 셔플 후 N곡 선택 (게임 내 중복 방지는 여기서, D-020)
→ 곡마다 영상 랜덤 → 그 영상의 구간 랜덤

곡 없는 영상(D-059)은 `v.song_id = s.id` JOIN에서 빠지고, 미수집 영상(D-060)은 구간이 없어서 빠진다. 쿼리를 고칠 필요가 없다.

**자동완성 목록** (D-007, D-040, D-052):

```sql
SELECT s.id,
       (SELECT GROUP_CONCAT(a.normalized SEPARATOR ' ')
        FROM song_answer a WHERE a.song_id = s.id) AS keywords,
       (SELECT name FROM song_name
        WHERE song_id = s.id AND language_id = :uiLang AND is_primary) AS display,
       (SELECT name FROM song_name
        WHERE song_id = s.id AND language_id = s.original_language_id AND is_primary) AS original
FROM song s WHERE s.status = 'PUBLISHED'
```

→ 앱 시작 시 한 번 받아 캐시. 표시는 `display (original)`, 검색은 `keywords`.

- `song.search_keywords` 컬럼을 없앴으므로 이 목록은 전개 결과에서 만든다 (D-052).
- `song_answer`가 곡 단위로 중복을 막으므로 `DISTINCT`가 필요 없다 (D-056).
- `GROUP_CONCAT`의 기본 길이 제한(`group_concat_max_len`)에 걸릴 수 있다.
  전개가 많은 곡에서 잘리면 값을 올리거나 곡당 별도 조회로 바꾼다.

## 7. 이 스키마에 **없는** 것 (의도적)

| 없는 것                      | 근거                                                 |
| ---------------------------- | ---------------------------------------------------- |
| `song.view_count`            | 조회수는 영상 단위 → `video.view_count` (D-034)      |
| `song.video_id`              | D-006 — 영상은 1:N                                   |
| `song_alias` 테이블          | D-040 — `song_name`으로 통합                         |
| `song.search_keywords`       | D-052 — `song_answer`가 대신한다                     |
| `song_name.normalized`       | D-052 — 정규화는 `song_answer`에서 한다              |
| `song_name.answer_pattern`   | D-056 — 정답 패턴은 `song_answer_pattern`으로 갈렸다 |
| `song_answer.language_id`    | D-056 — 정답 판정에 언어가 필요하지 않다             |
| `song.lyrics_language_id`    | D-051 — `song_language`로 통합                       |
| `song_producer` 테이블       | D-053 — `song_credit`(역할 포함)으로                 |
| `channel.producer_id`        | D-054 — `channel_producer`로 (다대다)                |
| `song.title_ko` / `title_en` | D-040 — `song_name` 행으로                           |
| `producer.name_ko`           | D-013 — 원어 표기 하나만 (보컬과 다름, 근거는 D-041) |
| `round.hint_level`           | D-018 — 점진 방식은 인트로 퀴즈 전용                 |
| 힌트 콘텐츠 테이블           | D-029 — 이미 가진 메타데이터만 쓴다                  |
| `segment.attributes` JSON    | D-045 — 속성이 유한하므로 컬럼이 맞다                |
| 장면 이미지 저장소           | D-042 — seek 방식을 먼저 만들고 보류                 |
| `search.list` 사용           | D-034 — 호출당 100 units. 코드 어디에도 쓰지 않는다  |

# VocaQuiz — 기술 설계 문서

> **개정 2026-09-07.** `docs/00-DECISIONS.md`(D-001~D-049) 기준.
> **테이블 정의는 이 문서에 없다 → `docs/04-SCHEMA.md`.** 중복을 만들지 않기 위해서다.
> 이 문서는 **결정과 그 이유**, 그리고 코드 구조를 적는다.

---

## 1. 기술 스택

| 영역         | 선택                                              | 근거                                                             |
| ------------ | ------------------------------------------------- | ---------------------------------------------------------------- |
| 언어/런타임  | Java 21                                           | 가상 스레드, record, pattern matching                            |
| 프레임워크   | Spring Boot **4.1.1** `[개정됨 → D-048]`          | 3.x 였으나 start.spring.io 기본값을 따랐다                       |
| 영속성       | Spring Data JPA (Hibernate)                       | 기존 경험                                                        |
| DB           | 개발·운영 **MySQL**, 테스트 H2 `[개정됨 → D-049]` | 숙련도. 이전은 "개발 H2 → 운영 PostgreSQL"                       |
| 마이그레이션 | Flyway                                            | Phase 2부터. Phase 1은 dev `update` / test `create-drop` (D-049) |
| 인증         | Spring Security OAuth2 Client (Google)            | **Phase 1부터** (D-032)                                          |
| 실시간       | Spring WebSocket + STOMP                          | Phase 4 (D-004)                                                  |
| 프론트       | React 18 + TypeScript + Vite                      | D-003                                                            |
| UI 킷        | Mantine 또는 shadcn/ui 중 **하나**                | CSS를 직접 쓰지 않기 위해                                        |
| 미디어       | YouTube IFrame Player API                         | D-001                                                            |
| 메타데이터   | YouTube Data API v3                               | D-034                                                            |

**의도적으로 안 쓰는 것:** Redis(Phase 4 전), Kafka, Elasticsearch, MSA, 상태관리 라이브러리.
넣을 자리는 §11에 표시해 뒀다.

---

## 2. 패키지 구조

```
com.vocaquiz
├── catalog/                    # 곡 카탈로그
│   ├── domain/     Song, SongName, Video, Segment, Producer, Vocal, VocalName, Language
│   ├── repository/
│   ├── service/    SongCatalogService, SegmentService, TextNormalizer
│   └── api/        SongController          (자동완성 목록)
│
├── youtube/                    # 외부 연동 (D-034)
│   ├── YoutubeDataClient       videos.list / playlistItems.list / channels.list
│   ├── YoutubeRssClient        채널 RSS (쿼터 0)
│   └── dto/
│
├── ingest/                     # 자동 파이프라인 (Phase 3)
│   ├── domain/     Channel, IngestItem
│   ├── service/    ChannelWatcher, SongClassifier(AI), IngestReviewService
│   └── scraper/    VocaloardScraper        (D-035, D-047)
│
├── admin/                      # 관리자 (Phase 1, D-030)
│   └── api/        AdminSongController, AdminSegmentController, AdminIngestController
│
├── game/                       # 게임 (핵심)
│   ├── domain/     Game, Round, Attempt, GameMode, RoundStatus
│   ├── repository/
│   ├── service/    GameService, RoundService, QuestionPicker
│   ├── quiz/       QuizType(interface), QuizTypeRegistry, RoundSeed, JudgeResult
│   │   └── types/  AudioSegmentQuiz         ← Phase 1
│   │               IntroGuessQuiz           ← Phase 3
│   │               SceneQuiz                ← Phase 3
│   │               ViewBattleQuiz           ← Phase 5
│   │               ...                      ← 새 유형은 여기에만
│   └── api/        GameController, dto/
│
├── daily/                      # Phase 2
├── room/                       # Phase 4 (멀티)
├── user/                       AppUser, 인증
└── common/
    ├── config/     SecurityConfig, WebConfig, JacksonConfig, WebSocketConfig
    ├── error/      ApiException, ErrorCode, GlobalExceptionHandler
    └── util/       SeedGenerator
```

**규칙:**

- 다른 도메인 패키지의 `domain`/`repository`를 직접 참조하지 않는다. `service`를 통한다.
- `api`의 DTO는 절대 엔티티를 그대로 노출하지 않는다.
- **`game/quiz/types/` 밖의 어떤 코드도 특정 퀴즈 유형의 이름을 알아서는 안 된다.**
  `"AUDIO_SEGMENT"` 문자열이 `GameService`·`RoundService`·컨트롤러·프론트 게임 화면에
  등장하면 설계 위반이다.

---

## 3. 도메인 모델

**→ `docs/04-SCHEMA.md`** (테이블 21개, 컬럼마다 결정 번호 근거 포함)

여기서는 코드 관점의 요점만 적는다:

- **엔티티에 `@Setter`를 만들지 않는다.** 상태 변경은 의미 있는 메서드로:
  `round.finish(correct, score)`, `round.updateProgress(json)`, `segment.retag(tag)`
- 연관관계는 기본 `LAZY`. `@ManyToOne(fetch = LAZY)`를 항상 명시.
- `song_name_answer`는 파생 테이블이다. `SongName.answer_pattern`이 바뀌면 `SongCatalogService`가 다시 만든다 (D-052).
  **이 갱신을 빠뜨리면 자동완성에서 곡이 안 잡힌다.** 저장 경로를 한 곳으로 모을 것.
- `song_vocal`도 파생이다. `video_vocal`이 바뀌고 그 영상이 `ORIGINAL`이면 다시 계산한다 (D-050).
  파생이 둘이므로 각각의 갱신 경로를 한 곳에 모으고, 정합성 점검을 관리자에게 노출한다.
- `segment.kind`는 **DB varchar + Java enum** (D-045). 알 수 없는 값은 로딩 시 예외.
- `round.payload`/`progress`만 JSON이다. 기준은 D-045:
  > **유한하고 예측 가능하면 컬럼, 무한하고 유형마다 다르면 JSON.**

---

## 4. QuizType 계약 — 유일한 확장점

**새 유형을 추가한다는 것 = 이 인터페이스 구현체 하나를 추가한다는 것.**

### 4.1 인터페이스

```java
public interface QuizType {

    /** "AUDIO_SEGMENT" 등. round.quiz_type에 저장되는 값. */
    String code();

    /** 이 유형이 라운드 하나에 필요한 곡 수. (1, 2, 3 …) */
    int songsPerRound();

    /** 이 유형이 쓰는 구간 종류. QuestionPicker가 후보를 거를 때 쓴다. */
    SegmentKind requiredSegmentKind();

    /** 출제. 후보를 받아 payload / progress / primarySongId를 만든다. */
    RoundSeed generate(List<Song> songs, List<Segment> segments);

    /** 클라이언트에 내려보낼 것. ★ 정답이 들어가면 안 된다 ★ */
    Map<String, Object> view(Round round);

    /** 채점 + 진행. 점수는 반드시 0..100 (D-023). */
    JudgeResult judge(Round round, JsonNode answer);

    /** 라운드 종료 시 공개할 정답 정보. */
    Map<String, Object> reveal(Round round);
}
```

```java
public record RoundSeed(JsonNode payload, JsonNode progress, Long primarySongId) {}

public record JudgeResult(
    boolean  correct,
    boolean  roundFinished,   // false면 같은 라운드에서 계속
    int      score,           // 0..100, roundFinished일 때만 의미 있음
    JsonNode nextProgress
) {}
```

### 4.2 레지스트리 — 유일한 배선 지점

```java
@Component
public class QuizTypeRegistry {
    private final Map<String, QuizType> byCode;

    public QuizTypeRegistry(List<QuizType> types) {   // Spring이 구현체를 전부 주입
        this.byCode = types.stream().collect(toMap(QuizType::code, identity()));
    }

    public QuizType get(String code) {
        QuizType t = byCode.get(code);
        if (t == null) throw new ApiException(UNKNOWN_QUIZ_TYPE, code);
        return t;
    }
}
```

→ `@Component`를 단 구현체를 만들면 **등록 코드조차 필요 없다.**

`RoundService`는 위임만 한다:

```java
public GuessResponse guess(UUID gameId, JsonNode answer, String ownerKey) {
    Round round = loadCurrentRound(gameId, ownerKey);       // 권한 + 상태 검증
    QuizType type = registry.get(round.getQuizType());      // ← 유일한 분기

    JudgeResult r = type.judge(round, answer);
    round.recordAttempt(answer, r.correct());

    if (r.roundFinished()) {
        round.finish(r.correct(), r.score());
        return GuessResponse.finished(r, type.reveal(round), advanceGame(round));
    }
    round.updateProgress(r.nextProgress());
    return GuessResponse.continued(r, type.view(round));
}
```

**이 메서드에 `if (quizType == ...)` 분기가 생기면 설계가 무너진 것이다.** 리뷰 1번 항목.

### 4.3 계약 검증 — 후보 유형이 들어가는가

| 유형             | songs | segmentKind  | payload                     | view                         | answer          | 채점                  |
| ---------------- | ----- | ------------ | --------------------------- | ---------------------------- | --------------- | --------------------- |
| `AUDIO_SEGMENT`  | 1     | AUDIO        | songId, videoId, start, end | videoId, start, end          | `{songId}`      | 유형이 정함           |
| `INTRO_GUESS`    | 1     | — (0초 고정) | songId, videoId             | videoId, playSec             | `{songId}`      | 6단계 감점            |
| `SCENE_*`        | 1     | SCENE        | songId, videoId, atSec      | videoId, atSec, mask         | `{songId}`      | 유형이 정함           |
| `VIEW_BATTLE`    | 2     | —            | aId, bId, answer            | a{}, b{}                     | `{choice:"A"}`  | SINGLE_SHOT           |
| `LYRIC_BLANK`    | 1     | —            | songId, line, blank         | lineWithBlank                | `{text}`        | 유형 내부 문자열 비교 |
| `PRODUCER_GUESS` | 1     | AUDIO        | songId, producerId          | videoId, start, end, choices | `{producerId}`  | SINGLE_SHOT           |
| `RELEASE_ORDER`  | 3     | —            | songIds, order              | songs[]                      | `{order:[...]}` | 부분 점수             |

**7개 전부 계약 안에 들어간다.** 특히:

- `INTRO_GUESS`는 구간이 필요 없다 → `requiredSegmentKind()`가 null을 반환한다
- `LYRIC_BLANK`는 자유 텍스트 → `judge()` 안에서만 문자열 비교. **API·스키마 변경 없음**
- `VIEW_BATTLE`은 곡 2개 → `songsPerRound()=2`

**이 표를 통과 못 하는 아이디어가 나오면 계약을 먼저 고친다.** 코드를 먼저 고치지 않는다.

### 4.4 이 추상화가 과설계가 아닌 이유

`CLAUDE.md`는 "구현체 하나뿐인 인터페이스"를 금지한다. `QuizType`은 **명시적 예외**다:

- 두 번째·세 번째 구현체가 Phase 3에 **확정**되어 있다 (D-018, D-026)
- 나중에 도입하는 비용이 크다: 스키마 마이그레이션 + API 응답 형태 변경 + 프론트 전면 수정
- 지금 도입하는 비용은 작다: Phase 1에 약 2~3시간

> **판단 기준: 두 번째 구현체가 *예정*이면 추상화, *상상*이면 과설계.**

---

## 5. REST API

베이스 `/api/v1`. 에러는 공통 형식 `{ "code": "...", "message": "..." }`.

### 5.1 곡 카탈로그

```
GET /api/v1/songs/autocomplete?lang=ko
  → 200
  [ { "id": 42,
      "display": "센보자쿠라",
      "original": "千本桜",
      "keywords": "senbonzakura 천본앵 千本桜 센보자쿠라" }, ... ]
```

- **전체 곡을 한 번에 내려준다.** 수천 곡이어도 gzip 후 수십 KB. 서버 왕복 없이 프론트에서 필터링.
- 정답이 새지 않는다 — 전체 목록이라 어떤 곡이 문제인지 알 수 없다.
- 표시는 `display (original)` (D-007), 검색은 `keywords` (전 언어 매칭).
- `keywords`는 `song_name_answer`의 전개 결과를 이어붙인 것이다 (D-052).
  `song.search_keywords` 컬럼은 없앴다.
- `Cache-Control: public, max-age=3600` + ETag.

### 5.2 게임

**설계 원칙: 유형이 늘어도 엔드포인트는 늘지 않는다.**
유형별로 다른 것은 전부 `view` / `answer` / `reveal` **봉투** 안에 들어간다.

```
POST /api/v1/games
  { "mode":"RANDOM", "quizType":"AUDIO_SEGMENT", "rounds":10,
    "filter": { "producerIds":[], "vocalIds":[], "lyricsLanguageIds":[],
                "yearFrom":null, "yearTo":null, "structureTags":[], "topN":null } }
  → 201 { "gameId":"uuid", "quizType":"AUDIO_SEGMENT", "totalRounds":10 }
  → 400 NOT_ENOUGH_SONGS / UNKNOWN_QUIZ_TYPE

GET /api/v1/games/{gameId}/rounds/current
  → 200 { "roundNo":3, "totalRounds":10, "quizType":"AUDIO_SEGMENT",
          "view": { "videoId":"...", "startSec":47.0, "endSec":59.0 },   ← 유형마다 다름
          "attemptsLeft":3, "possibleScore":100, "availableHints":["PRODUCER","YEAR"] }

POST /api/v1/games/{gameId}/rounds/current/guess
  { "answer": { "songId": 42 } }            ← 유형마다 다름. 패스는 null
  → 200 계속  { "correct":false, "finished":false, "view":{...},
                "attemptsLeft":2, "possibleScore":60 }
  → 200 종료  { "correct":true, "finished":true, "score":100,
                "reveal":{ "songId":42, "display":"센보자쿠라", "original":"千本桜",
                           "producers":["黒うさP"], "youtubeUrl":"..." },
                "gameFinished":false }
  → 409 ROUND_ALREADY_FINISHED / 400 INVALID_ANSWER / 403 NOT_GAME_OWNER

POST /api/v1/games/{gameId}/rounds/current/hint
  { "hint": "PRODUCER" }                    ← D-029
  → 200 { "hint":"PRODUCER", "value":"黒うさP", "possibleScore":70 }

GET /api/v1/games/{gameId}/result
  → 200 { "totalScore":470, "maxScore":1000, "rounds":[...] }
  → 409 GAME_NOT_FINISHED
```

**봉투 규칙:**

- 컨트롤러·서비스는 봉투 **안을 절대 들여다보지 않는다.** `QuizType` 구현체만 안다.
- 프론트도 마찬가지 — 게임 화면은 `quizType`으로 렌더러를 고르고 `view`를 통째로 넘긴다.
- 유형이 10개가 되어도 컨트롤러 코드는 한 줄도 안 늘어난다.

### 5.3 API 규칙

- **`current`를 경로에 쓴다.** 클라이언트가 라운드 번호를 지정하면 되돌아가는 치팅이 생긴다.
- 모든 상태 전이는 **`POST`에서만.** `GET`은 부작용이 없다 → 새로고침해도 게임이 안 망가진다.
- `GET current`를 100번 호출해도 `progress`가 그대로여야 한다.

### 5.4 관리자 (Phase 1, D-030)

```
POST   /api/v1/admin/songs/preview   { "videoId":"..." }
       → videos.list로 제목·설명문·길이·조회수·게시일·채널을 받아 초안 반환 (저장 안 함)
POST   /api/v1/admin/songs           곡 + 이름들 + 프로듀서 + 보컬 + 언어 저장
GET    /api/v1/admin/songs           목록 (검색, status 필터)
GET    /api/v1/admin/videos/{id}/segments
POST   /api/v1/admin/videos/{id}/segments   { kind, startSec, endSec, structureTag }
DELETE /api/v1/admin/segments/{id}
```

- 전부 `role = ADMIN`만 (D-032).

---

## 6. 게임 진행 흐름

```
[클라이언트]                         [서버]
    │ POST /games ─────────────────>  QuizType t = registry.get(quizType)
    │                                 후보 곡 = 필터 + t.requiredSegmentKind()로 조회
    │                                 필요 곡 수 = rounds × t.songsPerRound()
    │                                 부족하면 400 NOT_ENOUGH_SONGS
    │                                 seed로 셔플 → 곡 배분
    │                                 곡마다: 영상 랜덤 → 그 영상의 구간 랜덤  (D-020)
    │                                 t.generate(songs, segments) → RoundSeed
    │                                 Round N개 INSERT
    │ <──── gameId
    │
    │ GET .../rounds/current ──────>  return t.view(round)      ← 정답 없음
    │ <──── { quizType, view, ... }
    │
    │ quizType으로 렌더러 선택 → view를 그대로 넘겨 화면 구성
    │
    │ POST .../guess {answer} ─────>  권한/상태 검증
    │                                 JudgeResult r = t.judge(round, answer)
    │                                 attempt INSERT (answer 원본)
    │                                 ├ 종료 → round.finish() + t.reveal()
    │                                 └ 계속 → round.updateProgress() + t.view()
    │ <──── 결과
```

**서버가 유형을 아는 지점은 `registry.get()` 한 줄뿐이다.**

### QuestionPicker (D-020)

```java
List<RoundMaterial> pick(GameFilter f, QuizType t, int rounds, long seed) {
    List<Long> songIds = songRepository.findPlayableIds(f, t.requiredSegmentKind());
    int need = rounds * t.songsPerRound();
    if (songIds.size() < need) throw new ApiException(NOT_ENOUGH_SONGS);

    Collections.shuffle(songIds, new Random(seed));   // 데일리=날짜 시드
    // 곡마다: playable 영상 중 랜덤 → 그 영상의 구간 중 랜덤
    return build(songIds.subList(0, need), t);
}
```

- **`ORDER BY RANDOM()`을 쓰지 않는다.** 중복 배제와 데일리 재현성을 동시에 못 준다.
- `findPlayableIds`는 `04-SCHEMA.md` §6의 쿼리다. 구간이 없는 곡은 후보에 안 들어온다 (D-017).

---

## 7. 정답 판정 — 유형이 스스로 정한다

**답안 형태는 `QuizType`이 정한다. 프레임워크는 강제하지 않는다.**

곡 이름을 묻는 유형은 자유 텍스트를 받지 않고 **자동완성에서 고른 `songId`**를 받는다.
후보 검색은 `song_name_answer`를 본다 (D-052) — 한 곡이 여러 표기로 불려도 같은 `songId`로 모인다.
그 유형들의 `judge()`는 이렇게 끝난다:

```java
boolean correct = payload.songId() == answer.get("songId").asLong();
```

**왜 (곡 이름 유형에 한해):** 보카로 제목은 한/일/영/로마자/약칭이 뒤섞여 있고,
오타 허용 범위를 정하는 순간 끝없는 튜닝 지옥이 열린다. 게임의 재미와 무관한 작업이다.

**하지만 전역 규칙이 아니다.** `LYRIC_BLANK` 같은 유형은 본질적으로 자유 텍스트여야 한다.
그때는 **그 유형의 `judge()` 안에서만** 정규화 + 편집거리 판정을 한다.
API 스펙 변경 없음, 스키마 변경 없음, 다른 유형에 영향 없음.
**문자열 매칭의 고통이 유형 하나에 격리된다** — 봉투 설계의 실질적 값어치.

### TextNormalizer

```
1. NFKC 정규화
2. 소문자화 (Locale.ROOT)
3. 공백·기호 제거 — ( ) [ ] 【 】 ・ ー 〜 ! ? , . / -
```

**기호는 문자만 지운다.** 괄호를 지우되 괄호 안 내용은 남긴다.
`千本桜(feat. 初音ミク)` → `千本桜feat初音ミク`

가타카나→히라가나 통일은 넣지 않는다 (D-052).

`song_name_answer.normalized`가 이걸 쓴다 (D-052).
`catalog/service`에 둔다 — 자동완성과 미래의 `LYRIC_BLANK`가 함께 쓴다.

정규화 규칙을 바꾸면 `song_name_answer`를 전부 다시 만들어야 한다. 파생값이기 때문이다.

**나중에 (선택):** 곡이 수천 개가 되고 클라이언트 필터링이 느려지면 서버 검색으로 옮긴다.
**그때가 Elasticsearch 자리다 — 그전이 아니다.**

---

## 8. YouTube 연동 (D-034, D-043)

### 8.1 쿼터 규칙

하루 **10,000 units.**

| 메서드               | 비용    | 한 번에 | 용도                                |
| -------------------- | ------- | ------- | ----------------------------------- |
| `videos.list`        | 1       | id 50개 | 제목·설명문·길이·조회수·게시일·채널 |
| `playlistItems.list` | 1       | 50개    | uploads 재생목록 페이징             |
| `channels.list`      | 1       | id 50개 | `uploads_playlist_id` 획득          |
| `search.list`        | **100** | —       | **쓰지 않는다**                     |

> **`search.list`를 코드 어디에도 쓰지 않는다.**
> `videos.list`가 그것을 대체하는 게 아니다 — 하는 일이 다르다 (D-043).
> videoId 공급 경로는 넷이다: 사람이 고른 목록 / 채널 RSS / uploads 재생목록 / vocaloard.
> **검색이 필요해지는 순간이 설계가 틀어진 신호다.**

### 8.2 쿼터를 쓰지 않는 수단 (병행)

| 수단                                        | 얻는 것                                |
| ------------------------------------------- | -------------------------------------- |
| `youtube.com/feeds/videos.xml?channel_id=…` | 채널 최근 15개 (videoId, 제목, 게시일) |
| `youtube.com/oembed?url=…`                  | 제목, 채널명, 썸네일                   |
| `img.youtube.com/vi/{id}/…`                 | 썸네일 이미지                          |

**최적 조합: 신규 감지는 RSS(공짜, 자주) → 상세는 Data API(정확, 배치).**

### 8.3 얻을 수 없는 것

| 원하는 것              | 이유                               | 대응                         |
| ---------------------- | ---------------------------------- | ---------------------------- |
| most-replayed / 히트맵 | 공식 API에 엔드포인트 없음 (D-015) | 쓰지 않는다                  |
| 무음 구간              | 오디오 파형을 안 준다 (D-016)      | 사람이 구간을 찍는다 (D-017) |
| 임의 시점 프레임       | 영상 다운로드 필요 → D-001 위반    | seek 후 정지 (§9)            |

### 8.4 배치

```
조회수 갱신     @Scheduled  하루 1회  videos.list (id 50개당 1 unit)
채널 RSS 폴링   @Scheduled  6시간마다  쿼터 0
vocaloard      @Scheduled  하루 1~3회  D-047. User-Agent에 연락처 명시
```

**게임 요청 시점에 외부 API를 호출하지 않는다.**

---

## 9. 장면 제공 (D-025, D-042)

**서버는 이미지를 저장하지 않는다.** `{videoId, atSec}`만 저장하고 플레이어를 seek 후 정지한다.

### 정밀도

공식 문서: 플레이어는 **"해당 시각 직전의 가장 가까운 키프레임으로 이동한다.
단 그 구간이 이미 다운로드된 경우는 예외."** 키프레임 간격은 통상 2~5초.

**우회 패턴:**

```
seekTo(t - 2) → 음소거 재생으로 버퍼 확보 → seekTo(t) → pauseVideo()
```

프레임 단위 보장은 불가. 실질 오차 ±0.1초 — 게임에는 충분하다.

### 불가능한 경로 (시도하지 말 것)

| 방법                     | 왜                                  |
| ------------------------ | ----------------------------------- |
| `<canvas>`로 iframe 캡처 | cross-origin, tainted canvas        |
| 서버 ffmpeg 프레임 추출  | 영상 다운로드 → D-001 위반          |
| 임의 시점 썸네일 URL     | 유튜브는 대표 + 자동생성 3장만 제공 |

- 모자이크는 플레이어 위 CSS 오버레이 (`filter: blur()` 또는 저해상도 확대).
- **이미지 저장 방식은 D-042로 보류.** seek 방식 품질이 부족할 때 O-28로 돌아온다.

---

## 10. 자동 파이프라인 (Phase 3)

```
channel (watch = true)                       ← 사람이 켠다 (D-031)
  └ RSS 폴링 (쿼터 0)
      └ 새 videoId → ingest_item (PENDING)
          └ videos.list로 상세 (1 unit)
              └ AI 판별 → ai_verdict
                  { isSong, confidence, titleKo/En/Original,
                    producerGuess, similarSongIds[], reason }
                  └ 관리자 검수 ── 새 곡 / 기존 곡 #42에 병합 / 폐기
                      └ 사람이 구간을 찍어야 비로소 출제됨 (D-017)
```

- **AI는 후보만 제시한다 (D-011).** 자동 병합하지 않는다 — 보카로는 제목이 비슷한 별개 곡이
  흔하고, 오판 시 서로 다른 곡이 합쳐져 정답 판정이 깨진다. 되돌리기도 어렵다.
- **파이프라인은 "찾기"만 한다 (D-022).** 곡 수가 늘어도 콘텐츠는 즉시 안 는다.
  검수 대기열이 쌓이는 건 정상이며, 카탈로그 품질을 보장하는 대가다.
- vocaloard 스크래퍼(D-035)는 **보컬 정보의 유일한 출처**다. Data API로 대체 불가.
  1일 1~3회만 접속 (D-047).

---

## 11. 멀티플레이 (Phase 4)

### 11.1 방 상태 머신

```
       ┌─────────┐  host:start  ┌──────────────┐
       │ WAITING │─────────────>│ ROUND_PLAYING│<──┐
       └─────────┘              └──────┬───────┘   │
            ▲                          │ 전원 정답  │
            │ host:playAgain           │ or 타임아웃│ 다음 라운드
       ┌─────────┐              ┌──────▼───────┐   │
       │FINISHED │<─────────────│ ROUND_REVEAL │───┘
       └─────────┘  마지막 라운드 └──────────────┘
```

- **상태 전이는 서버만 한다.** 클라이언트는 정답 제출과 방장의 시작 요청만 보낸다.
- `ROUND_REVEAL`은 5초 고정.

### 11.2 저장 위치

**인메모리 `ConcurrentHashMap` + 단일 인스턴스.** 결과만 종료 시 DB에 한 번 저장.
서버 재시작 시 진행 중 방이 날아간다 — **수용한다.** 20분짜리 세션이다.
확장 시 Redis Pub/Sub를 넣는다. 그때 `RoomService` 인터페이스만 갈아 끼우게 설계해 둔다.

### 11.3 STOMP

```
연결   /ws                              (SockJS fallback 사용 안 함)
구독   /topic/rooms/{code}              방 전체
       /user/queue/rooms/{code}         나에게만
발행   /app/rooms/{code}/join|start|guess
```

| 이벤트                          | payload                   | 주의                              |
| ------------------------------- | ------------------------- | --------------------------------- |
| `MEMBER_JOINED` / `MEMBER_LEFT` | 참가자 목록               |                                   |
| `ROUND_STARTED`                 | roundNo, view, deadlineAt | `deadlineAt`은 **서버 절대 시각** |
| `MEMBER_SOLVED`                 | nickname, elapsedMs       | **곡 정보 절대 미포함**           |
| `ROUND_ENDED`                   | reveal, 라운드 점수       |                                   |
| `GAME_ENDED`                    | 최종 순위                 |                                   |

### 11.4 동시성

- 방 단위 lock으로 상태 변경을 직렬화. 방 사이엔 경합이 없다.
- **타임아웃 스케줄러와 마지막 정답이 동시에 라운드를 끝내려 한다.**
  `round.status`를 CAS로 확인하고 한 번만 전이시킨다. 이 버그는 반드시 한 번 만난다.

---

## 12. 보안 / 치팅 방어 체크리스트

- [ ] 응답 DTO에 정답 곡 정보가 라운드 종료 전에 들어가지 않았는가?
- [ ] `round.payload`가 그대로 실려 나가고 있지 않은가? (`view()`를 거쳤는가)
- [ ] 점수를 클라이언트가 보낸 값으로 계산하고 있지 않은가?
- [ ] 경과 시간을 클라이언트 timestamp로 계산하고 있지 않은가?
- [ ] `game.owner_key`가 요청자와 일치하는지 확인했는가?
- [ ] 이미 끝난 라운드에 `guess`가 오면 409로 거부하는가?
- [ ] `/admin/**`이 `role = ADMIN`으로 막혀 있는가?
- [ ] 데일리를 하루 두 번 시작할 수 있지 않은가? (DB 유니크 제약)

**확장성 체크리스트 (설계 위반 감지):**

- [ ] `game/quiz/types/` 밖에 유형 이름으로 분기하는 `if`/`switch`가 생기지 않았는가?
- [ ] 새 유형을 추가하며 DB 마이그레이션을 쓰고 있지 않은가?
- [ ] 새 유형을 추가하며 API 응답 형태를 바꾸고 있지 않은가?
- [ ] `judge()`가 돌려주는 점수가 `0..100`을 벗어나지 않는가?
- [ ] **`search.list`를 호출하는 코드가 생기지 않았는가?** (D-034)

---

## 13. 배포 (Phase 2)

- 백엔드: Fly.io / Railway / Oracle Cloud Free 중 **가장 빨리 뜨는 곳**
- DB: 관리형 **MySQL** `[개정됨 → D-049]` (이전: 관리형 PostgreSQL)
- 프론트: Gradle 빌드 시 `npm run build` 결과를 `src/main/resources/static`으로 복사해 **통합 배포**
  → CORS·쿠키 문제가 사라진다. 1인 개발이라면 통합이 덜 아프다.
- 환경변수: `YOUTUBE_API_KEY`, `OAUTH_*`, `DB_*`. 하드코딩 금지, `.env`는 `.gitignore`.

---

## 14. 나중에 넣을 자리

| 넣을 것                  | 시점                        | 미리 준비할 것                           |
| ------------------------ | --------------------------- | ---------------------------------------- |
| Redis                    | 멀티 서버 확장              | `RoomService`를 인터페이스로             |
| Elasticsearch            | 곡 5,000개 초과 + 서버 검색 | `SongCatalogService.search()` 시그니처   |
| 랭킹                     | Phase 2 이후                | `game.total_score` 인덱스                |
| 한 게임에 여러 유형 섞기 | 유형 3개 이상               | 점수 0..100 정규화가 이미 가능하게 해 둠 |
| 장면 이미지 저장소       | seek 품질 부족 시           | O-28                                     |
| WebSub 푸시              | 배포 후                     | 채널 RSS 폴링을 대체                     |

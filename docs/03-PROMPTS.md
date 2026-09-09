# VocaQuiz — 단계별 개발 프롬프트

> **개정 2026-09-05.** D-033 순서(관리자 → 구간 데이터 → 게임)에 맞춰 재구성.

각 단계의 프롬프트를 **그대로 복사해서** 새 대화에 붙여 넣는다. `{...}`만 채운다.

## 형식

```
[맥락]  어떤 단계인가, 어떤 문서를 읽어야 하는가
[작업]  정확히 무엇을 만들 것인가
[제약]  하지 말아야 할 것
[완료]  무엇이 되면 끝인가 — 검증 가능한 형태로
```

**[완료] 없는 프롬프트는 쓰지 않는다.** AI가 과하게 만들거나 덜 만드는 가장 큰 원인이다.

## 모든 프롬프트의 공통 머리말

```
docs/00-DECISIONS.md가 이 프로젝트의 진실의 원천이다. 다른 문서와 어긋나면 그것이 이긴다.
작업 전에 CLAUDE.md를 읽어라.
```

---

## Phase 1 — 관리자 → 구간 → 게임 (MVP)

> **순서가 강제되어 있다 (D-033).** 구간이 없으면 게임을 만들 수 없다.
> 5번에서 30곡만 채우고 6번으로 넘어간다. **300곡을 다 채우고 게임을 시작하지 않는다.**

### P1-1. 스캐폴딩 + OAuth2 + 관리자 화이트리스트

```
[맥락]
docs/00-DECISIONS.md D-032, docs/02-ARCHITECTURE.md §1, §2를 읽어라.

[작업]
Spring Boot 3.x + Java 21 + Gradle(Kotlin DSL).
- 의존성: web, data-jpa, validation, lombok, h2, security, oauth2-client
- 설계 문서 §2의 패키지 구조를 빈 패키지로 생성 (Phase 1에 필요한 것만:
  catalog/, youtube/, admin/, game/, user/, common/)
- common/error에 ApiException, ErrorCode(enum), GlobalExceptionHandler
- Google OAuth2 로그인. app_user 엔티티 (04-SCHEMA.md §4)
- SecurityConfig: /admin/**은 role=ADMIN만. 나머지는 익명 허용
- 관리자 화이트리스트는 환경변수 ADMIN_EMAILS로. 첫 로그인 시 그 목록에 있으면 role=ADMIN
- application.yml: dev 프로파일 H2 인메모리 + ddl-auto=create-drop + h2-console

[제약]
- WebSocket, Redis, Flyway는 아직 넣지 마라.
- room/, daily/, ingest/ 패키지는 만들지 마라.
- JWT를 직접 구현하지 마라. 세션 기반으로 간다.
- 권한 체계를 만들지 마라. USER / ADMIN 둘뿐이다.

[완료]
1) ./gradlew bootRun이 뜨고 h2-console이 열린다.
2) 내 구글 계정으로 로그인하면 /admin에 접근되고, 다른 계정은 403.
```

### P1-2. 카탈로그 도메인 + YouTube 연동

```
[맥락]
docs/04-SCHEMA.md §1 전체와 docs/00-DECISIONS.md D-034, D-040, D-043,
D-050 ~ D-055를 읽어라.

[작업]
1. 엔티티: Language, Producer, Vocal, VocalName, VideoVocal, SongVocal,
   Channel, ChannelProducer, Song, SongLanguage, SongName, SongNameAnswer,
   SongCredit, Video, VideoCredit  — 04-SCHEMA.md §1 그대로
2. catalog/service/TextNormalizer — 설계 문서 §7의 정규화 4단계
3. catalog/service/AnswerPatternExpander — song_name.answer_pattern 전개 (D-052)
   괄호와 파이프만. \( \| \) \\ 이스케이프. 20개 초과면 경고 신호를 반환한다
4. SongCatalogService — 파생 두 개의 갱신 경로를 각각 한 곳으로 모아라
   ★ answer_pattern이 바뀌면 song_name_answer를 다시 만든다 (D-052) ★
   ★ video_vocal이 바뀌고 그 영상이 ORIGINAL이면 song_vocal을 다시 계산한다 (D-050) ★
5. youtube/YoutubeDataClient — videos.list만 구현.
   id 50개씩 배치. 응답에서 title/description/duration/viewCount/publishedAt/channelId 추출
   description은 Phase 3에서 "이게 노래인가"를 AI에게 판정시킬 입력이다 (D-011).
   part=snippet에 이미 들어 있어 추가 쿼터가 0이다. 지금은 받아만 두고 저장하지 않는다.
6. Language 시드 데이터 (KO, EN, JA)

[제약]
- ★ search.list를 호출하는 코드를 절대 만들지 마라 ★ (D-034: 호출당 100 units)
- playlistItems.list, channels.list는 Phase 3에서 만든다. 지금은 videos.list만.
- API 키는 환경변수 YOUTUBE_API_KEY. 하드코딩 금지.
- song에 제목 컬럼을 만들지 마라. 전부 song_name 행이다 (D-040).
- song에 언어 컬럼을 만들지 마라. 곡의 언어는 song_language다 (D-051).
  original_language_id는 예외다 — 원제가 어느 언어인지를 가리킨다.
- song.search_keywords와 song_name.normalized를 만들지 마라. song_name_answer가 대신한다 (D-052).
- credit.role은 DB varchar + 서버 enum이고 지금은 값 하나만 쓴다 (D-053).
  역할 목록을 미리 늘리지 마라 (O-32).
- 원곡 영상의 크레딧을 곡에서 자동으로 상속하지 마라 (D-053).

[완료]
1) TextNormalizer 단위 테스트: "千本桜" "senbonzakura" "천본앵" 이 각각 정규화된다.
2) AnswerPatternExpander 단위 테스트: (히토|인간|사람)(마니아|매니아)가 6개로 전개되고,
   \( 는 문자 '(' 로 남는다.
3) YoutubeDataClient로 videoId 하나를 조회하면 제목·길이·조회수가 나온다.
4) 곡 하나를 코드로 저장하면 song_name_answer에 전 언어 후보가 들어간다.
5) ORIGINAL 영상에 video_vocal을 넣으면 song_vocal이 같은 집합이 된다.
```

### P1-3. 관리자 — 곡 등록 · 목록

```
[맥락]
docs/02-ARCHITECTURE.md §5.4, docs/01-PRD.md §8을 읽어라.

[작업]
백엔드:
- POST /api/v1/admin/songs/preview  { videoId } → videos.list로 초안 반환 (저장 안 함)
- POST /api/v1/admin/songs          곡 + 이름들(언어별) + 프로듀서 + 보컬 + 언어 저장
- GET  /api/v1/admin/songs          목록. 검색 + status 필터

프론트 (frontend/, Vite + React + TS):
- /admin/songs        목록
- /admin/songs/new    videoId 붙여넣기 → 미리보기 → 이름·언어·프로듀서·보컬 입력 → 저장

[제약]
- 상태 관리 라이브러리를 설치하지 마라. useState/useReducer만.
- CSS 파일을 직접 쓰지 마라. UI 킷({Mantine 또는 shadcn/ui}) 컴포넌트로만.
- any 금지. API 타입은 src/api/types.ts 한 곳에.
- 게임 화면을 만들지 마라. 지금은 관리자만.

[완료]
브라우저에서 유튜브 URL을 붙여넣어 곡 하나를 끝까지 등록하고 목록에서 확인한다.
```

### P1-4. 관리자 — 구간 찍기 화면 ★ 가장 어려운 화면 ★

```
[맥락]
docs/00-DECISIONS.md D-017, D-020, D-024, D-045와 docs/04-SCHEMA.md §2를 읽어라.
나는 프론트를 잘 모른다. 코드에 무엇을 하는지 짧게 설명을 달아 달라.

이 화면 없이는 게임에 낼 문제가 하나도 없다 (D-030). 여기가 Phase 1의 핵심이다.

[작업]
백엔드:
- GET/POST/DELETE  /api/v1/admin/videos/{id}/segments
- segment: kind(varchar, Java는 enum), start_sec, end_sec, structure_tag, scene_purpose
- 검증: 오디오 구간이면 end_sec 필수이고 길이 5~20초 (D-017)

프론트 /admin/videos/{id}/segments :
1. YouTube IFrame Player 임베드 (정상 크기로 보이게)
2. setInterval로 getCurrentTime()을 폴링해 현재 시각을 표시
3. [시작 지점 찍기] [끝 지점 찍기] 버튼 → 현재 시각을 기록
4. 구조 태그 선택 (INTRO/VERSE/PRE_CHORUS/CHORUS/INTERLUDE/OUTRO)
5. 저장 → 구간 목록에 추가
6. 목록의 각 구간에 [미리듣기] — seekTo(start) 후 재생, (end-start)초 뒤 pause

[제약]
- JSON 컬럼을 만들지 마라 (D-045). 속성은 컬럼이다.
- 파형(waveform) 시각화를 만들지 마라. 오디오에 접근할 수 없다 (D-016).
- 자동 구간 추천을 만들지 마라. 사람이 찍는 것이 D-017의 결정이다.
- 드래그 타임라인 같은 정교한 UI를 만들지 마라. 버튼 2개로 충분하다.

[완료]
영상 하나에 구간 3개를 찍고, 각각 미리듣기로 의도한 소리가 나오는지 확인한다.
```

### P1-5. 구간 데이터 채우기 (코딩 아님)

```
곡 30개에 구간을 각각 2~3개씩 찍는다. 약 1~2시간.

★ 여기서 300곡을 다 채우려 하지 마라 (D-033). 30곡이면 게임 개발에 충분하다.
   나머지는 게임이 돌아가는 걸 본 뒤에 채운다. ★

[완료] segment 테이블에 60~90행이 있다.
```

### P1-6. 게임 도메인 + QuizType 계약 ★ 설계의 핵심 ★

```
[맥락]
docs/02-ARCHITECTURE.md §4 전체(계약, 레지스트리, 검증 표)와 §6, docs/04-SCHEMA.md §5를 읽어라.

퀴즈 유형이 계속 추가되는 것이 1급 요구사항이다 (D-005).
목표: 새 유형 추가 = 클래스 1개. DB 마이그레이션 0, 기존 코드 수정 0.

[작업]
1. 엔티티: Game, Round, Attempt
   - Round는 quiz_type(varchar) / payload(json) / progress(json) / score / primary_song_id
   - 유형별 컬럼을 절대 만들지 마라
   - JSON은 @JdbcTypeCode(SqlTypes.JSON). H2/PostgreSQL 양쪽에서 동작해야 한다
   - @Setter 금지. round.finish(...), round.updateProgress(...)로
2. game/quiz/ : QuizType, RoundSeed, JudgeResult, QuizTypeRegistry — §4.1~4.2 시그니처 그대로
3. game/quiz/types/AudioSegmentQuiz — 유일한 구현체
   - songsPerRound()=1, requiredSegmentKind()=AUDIO
   - generate(): 구간의 videoId/start/end로 payload
   - view(): videoId, startSec, endSec — ★ 정답 songId 절대 미포함 ★
   - judge(): 채점 방식은 {ATTEMPT_BASED 등 골라서}. 점수는 반드시 0..100
4. game/service/QuestionPicker — §6 의사코드. 곡→영상→구간 순서 (D-020)
5. POST /api/v1/games

[제약]
- 구현체를 AudioSegmentQuiz 하나만 만들어라. IntroGuess를 미리 만들지 마라.
- GameService/QuestionPicker/컨트롤러 어디에도 "AUDIO_SEGMENT" 문자열이 등장하면 안 된다.
- 공용 ScoringService를 만들지 마라. 점수는 각 QuizType이 소유한다 (D-023).
- 데일리/범위지정 분기를 만들지 마라. mode=RANDOM만.

[완료]
1) AudioSegmentQuiz 단위 테스트: 점수가 항상 0..100. view()에 payload의 songId 없음.
2) 계약 테스트 — registry의 모든 QuizType을 순회하며 검사한다.
   새 유형이 추가되면 코드 수정 없이 자동으로 그 유형도 검사되어야 한다:
   - code() 유일성
   - judge()의 score가 항상 0..100
   - view()를 JSON 직렬화한 문자열에 정답 식별자가 없음
   - generate()가 songsPerRound() 개의 곡으로 항상 성공
3) curl로 게임 생성 → 201 + gameId. round 10개의 payload가 서로 다른 곡을 가리킨다.
```

### P1-7. 게임 API

```
[맥락] docs/02-ARCHITECTURE.md §5.2, §5.3, §6, §12를 읽어라.

[작업]
1. GET  /api/v1/games/{gameId}/rounds/current
2. POST /api/v1/games/{gameId}/rounds/current/guess
3. GET  /api/v1/games/{gameId}/result
설계 문서 §5.2의 view/answer/reveal 봉투 구조 그대로.
RoundService.guess()는 §4.2의 의사코드 형태여야 한다.

[제약] — 절대 규칙
- RoundService/GameController 안에 퀴즈 유형 이름으로 분기하는 if/switch 금지.
- 서비스·컨트롤러가 view/answer/reveal 봉투 "안"을 들여다보지 마라.
- GET current는 어떤 상태도 바꾸지 않는다. 100번 호출해도 progress가 그대로.
- round.payload가 응답에 실려 나가면 안 된다. 반드시 view()를 거친다.
- owner_key 불일치 403, 끝난 라운드에 guess 오면 409.

[완료]
@SpringBootTest + MockMvc 통합 테스트 1개:
  게임 생성 → GET current 3번 호출해도 progress 불변 단언
  → 오답 → 정답 → reveal에 곡 정보 등장
  → 10라운드 완주 → result 조회
그리고 라운드 진행 중 응답 JSON 문자열에 정답 곡 제목이 없음을 단언한다.
```

### P1-8. 게임 프론트

```
[맥락]
docs/01-PRD.md §8, docs/02-ARCHITECTURE.md §5.1, §5.2를 읽어라.
퀴즈 유형은 계속 추가된다. 게임 화면은 유형을 몰라야 한다.

[작업]
화면 3개 추가: 홈 / 게임 / 결과

핵심 구조 — 이대로 만들어라:

  src/quiz/types.ts
    export interface QuizRenderer<V, A> {
      code: string;
      View: React.FC<{ view: V; onAnswer: (a: A) => void; disabled: boolean }>;
      Reveal: React.FC<{ reveal: unknown }>;
    }

  src/quiz/registry.ts
    export const RENDERERS: Record<string, QuizRenderer<any, any>> = {
      [AudioSegment.code]: AudioSegment,
      // 새 유형은 여기 한 줄만 추가된다
    };

  src/quiz/AudioSegment.tsx
    - IFrame Player로 loadVideoById(videoId, startSec)
    - (endSec - startSec) 뒤 pauseVideo()
    - [다시 듣기] 버튼
    - 자동완성 입력 → onAnswer({ songId })

  src/pages/GamePage.tsx
    - GET current → { quizType, view, ... }
    - const R = RENDERERS[quizType] → <R.View view={view} onAnswer={...} />
    - ★ GamePage 안에 videoId, startSec, songId 같은 유형별 개념이 등장하면 안 된다 ★

자동완성: GET /songs/autocomplete를 앱 시작 시 한 번 받아 메모리에 두고,
입력값을 백엔드 TextNormalizer와 같은 규칙으로 정규화해 keywords로 필터링한다.
공용 훅 src/quiz/useSongAutocomplete로 만들어라 — 여러 유형이 쓴다.
표시는 "display (original)" 형식 (D-007).

플레이어는 보이지 않게 하되 display:none은 쓰지 마라 (PRD §7.2).

[제약]
- 상태 관리 라이브러리 금지. CSS 직접 작성 금지. any 금지(registry 값 타입 제외).
- 반응형·애니메이션·다크모드 신경 쓰지 마라. Phase 1은 동작이 전부다.

[완료]
1) "시작 → 구간 듣기 → 곡 선택 → 정답/오답 → 10라운드 완주 → 결과" 를 실제로 해낸다.
2) GamePage.tsx에 "videoId", "startSec", "songId" 라는 단어가 하나도 없다.
```

---

## Phase 2 — 배포 + 데일리

### P2-1. Flyway 전환

```
[작업] ddl-auto를 validate로 바꾸고 현재 스키마를 V1__init.sql로. 운영 프로파일에 PostgreSQL.
[완료] 빈 PostgreSQL에 prod 프로파일로 기동하면 스키마가 생성되고 앱이 뜬다.
```

### P2-2. 배포

```
[작업] {Fly.io / Railway / Oracle Cloud 중 고른 것}. 프론트는 Gradle 빌드 시
       npm run build 결과를 src/main/resources/static으로 복사해 통합 배포.
[제약] Docker Compose로 여러 서비스를 띄우지 마라. CI/CD를 지금 만들지 마라.
[완료] 공개 URL을 친구에게 보내면 한 판을 플레이할 수 있다.
```

### P2-3. 데일리

```
[작업] daily 도메인. PRD §3.6.
  - 날짜(KST) 시드로 QuestionPicker 호출 → 전 유저 동일 문제
  - (user_id, play_date) 유니크 제약으로 하루 1회 강제
  - 워들 스타일 공유 텍스트
[제약] 데일리는 로그인 필수. 과거 아카이브를 만들지 마라.
[완료] 같은 날 두 번 시작하면 409. 다른 계정으로 시작하면 같은 곡이 나온다.
```

---

## Phase 3 — 유형 추가 + 범위지정 + 파이프라인

> **Phase 3의 진짜 목적은 기능이 아니라 "Phase 1 설계가 맞았는지 검증하는 것"이다.**

### P3-1. INTRO_GUESS 추가 — 설계 검증

```
[맥락] docs/02-ARCHITECTURE.md §4, D-018, D-046을 읽어라.

[작업]
1. game/quiz/types/IntroGuessQuiz — @Component 하나만 새로 추가
   - songsPerRound()=1, requiredSegmentKind()=null  (0초 고정이라 구간이 필요 없다)
   - 단계: 0.5 / 1 / 2 / 4 / 8 / 16 초 (D-046)
   - progress={"stage":n}, 단계별 감점. 점수는 0..100
2. frontend/src/quiz/IntroGuess.tsx + registry 한 줄

[제약] — 이걸 어기면 Phase 1 설계가 실패한 것이다
- DB 마이그레이션 금지. API 스펙 변경 금지.
- GameService / RoundService / GameController / GamePage.tsx 수정 금지.
- AudioSegmentQuiz 수정 금지.
- 두 유형의 공통 코드를 뽑아 부모 클래스를 만들지 마라. 세 번째가 다르면 부채가 된다.

[완료]
1) P1-6의 계약 테스트가 IntroGuessQuiz도 자동으로 검사하며 통과한다.
2) git diff --stat에 위 [제약]의 파일이 하나도 없다.   ← 진짜 완료 조건
3) 브라우저에서 인트로 모드를 10라운드 완주한다.
```

### P3-2. SCENE 유형 + 장면 구간

```
[작업]
- 관리자 구간 찍기에 kind=SCENE 지원 (end_sec 없이 한 점, scene_purpose 입력)
- SceneQuiz: seekTo(t-2) → 음소거 재생으로 버퍼 확보 → seekTo(t) → pause (설계 문서 §9)
- 모자이크는 플레이어 위 CSS 오버레이
[제약] canvas 캡처를 시도하지 마라. 서버에 이미지를 저장하지 마라 (D-042).
[완료] 장면 모드를 완주한다. 정지 화면이 의도한 시점과 ±0.5초 안에 맞는다.
```

### P3-3. 범위지정 필터

```
[작업] POST /games의 filter를 동작시킨다 (PRD §3.7, 04-SCHEMA.md §6).
  프로듀서 / 보컬 / 연도 / 가사 언어(D-044) / 구조 태그 / 인기도
  - GET /api/v1/songs/count?{filter} 추가
  - 후보 부족 시 400 NOT_ENOUGH_SONGS, 프론트는 시작 버튼 비활성화
[제약] QuestionPicker가 특정 유형을 알면 안 된다. songsPerRound/requiredSegmentKind는 유형에게 묻는다.
[완료] 가사 언어를 일본어로 걸면 영어곡이 빠지고, 후보가 부족하면 이유가 표시된다.
```

### P3-4. 채널 감시 파이프라인

```
[맥락] docs/02-ARCHITECTURE.md §10, D-008, D-011, D-022, D-031, D-034를 읽어라.

[작업]
1. ingest 도메인: Channel, IngestItem (04-SCHEMA.md §3)
2. YoutubeRssClient — feeds/videos.xml 폴링 (쿼터 0). 6시간마다
3. 신규 videoId → videos.list로 상세 (1 unit) → ingest_item(PENDING)
4. SongClassifier — AI 판별. ai_verdict에 isSong / 제목 초안 / similarSongIds 저장
5. 관리자 검수 화면: 새 곡 / 기존 곡에 병합 / 폐기
6. channels.list로 uploads_playlist_id 확보 (1 unit)

[제약]
- ★ search.list 금지 ★ (D-034)
- AI가 자동으로 병합하게 만들지 마라 (D-011). 후보 제시까지만.
- 자동으로 구간을 부여하지 마라 (D-022). 사람이 찍는다.
- channel.watch 기본값은 false (D-031).

[완료]
watch=true인 채널에 새 영상이 올라오면 검수 대기열에 뜨고, 승인하면 곡이 생긴다.
승인된 곡은 구간을 찍기 전까지 게임에 나오지 않는다.
```

### P3-5. vocaloard 스크래퍼

```
[작업] VocaloardScraper — 랭킹 페이지에서 제목/프로듀서/보컬/videoId 파싱.
       보컬 정보를 song_vocal에 채운다 (D-035: 보컬의 유일한 출처).
[제약] ★ 1일 1~3회만 접속 (D-047) ★. 간격 제한을 코드에 명시하고
       User-Agent에 연락처를 적어라. 병렬 요청을 하지 마라.
[완료] 하루 1회 배치가 돌고, 신곡의 보컬 정보가 채워진다.
```

---

## Phase 4 — 멀티플레이

### P4-1. 방 생성/입장 (게임 없이)

```
[작업] 설계 문서 §11대로 room 도메인. 인메모리 ConcurrentHashMap.
       STOMP 설정 + join → MEMBER_JOINED / MEMBER_LEFT 브로드캐스트만.
[제약] 게임 로직·채팅을 넣지 마라. Redis를 쓰지 마라.
[완료] 탭 2개로 같은 방 코드에 들어가면 서로의 닉네임이 뜬다. 닫으면 사라진다.
```

### P4-2. 라운드 진행 + 채점

```
[작업] §11.1 상태 머신. ROUND_STARTED(deadlineAt=서버 절대 시각), guess 처리,
       PRD §3.3 멀티 공식, 타임아웃 스케줄러, ROUND_ENDED / GAME_ENDED.
[제약] — 절대 규칙
- elapsed는 서버에서만 계산. 클라이언트 시간을 쓰지 마라.
- MEMBER_SOLVED에 곡 정보를 넣지 마라. 닉네임과 소요 시간만.
- 방 단위 lock으로 직렬화하고, 타임아웃과 마지막 정답이 라운드를 두 번 끝내지 못하게 CAS.
[완료] 브라우저 2개로 5라운드를 끝내고 순위를 본다.
       즉답한 쪽과 늦게 맞힌 쪽이 둘 다 점수를 받되 차이가 난다.
```

---

## Phase 5 — VIEW_BATTLE

```
[맥락] D-009, D-034를 읽어라. 조회수는 video.view_count에 이미 있다.

[작업]
1. 조회수 갱신 배치 — @Scheduled 하루 1회, videos.list id 50개 배치 (1 unit)
2. game/quiz/types/ViewBattleQuiz — @Component 추가
   - songsPerRound()=2, requiredSegmentKind()=null
   - generate(): 조회수 차이가 20% 이내인 조합은 피해라 — 운빨 게임이 된다
   - view(): a{display, thumbnailUrl}, b{...}  ★ 조회수 절대 미포함 ★
   - judge(): SINGLE_SHOT. 정답 100 / 오답 0
3. frontend/src/quiz/ViewBattle.tsx + registry 한 줄

[제약]
- DB 마이그레이션 금지 (view_count는 이미 있다).
- API 스펙, GameService, RoundService, GamePage 수정 금지.
- ★ search.list 금지 ★

[완료]
1) 계약 테스트가 ViewBattleQuiz도 통과한다.
2) git diff --stat에 quiz/types/와 frontend/src/quiz/ 밖의 파일이
   배치 클래스 하나를 빼고 없다.
```

---

## 새 퀴즈 유형을 추가할 때 (범용 템플릿)

```
[맥락]
docs/02-ARCHITECTURE.md §4 (계약, 레지스트리, §4.3 검증 표)를 읽어라.
새 퀴즈 유형 {코드명} 을 추가한다.

먼저 이 유형을 §4.3 표의 형식으로 표현해 보고 계약에 들어가는지 확인한 뒤 보고하라.
들어가지 않으면 코드를 쓰지 말고 계약을 어떻게 바꿔야 하는지 먼저 제안하라.

[유형 정의]
- 이름 / 출제 내용 / 답안 형태 / 진행 방식 / 채점 방식 / 라운드당 곡 수 / 필요한 구간 종류

[작업]
1. game/quiz/types/{코드명}Quiz.java  (@Component)
2. frontend/src/quiz/{코드명}.tsx
3. frontend/src/quiz/registry.ts 한 줄

[제약]
- DB 마이그레이션 금지 / API 스펙 변경 금지
- GameService / RoundService / GameController / GamePage.tsx 수정 금지
- 기존 QuizType 구현체 수정 금지
- 다른 유형과 비슷해 보여도 공통 부모 클래스를 뽑지 마라
- search.list 금지

[완료]
1) 계약 테스트가 새 유형도 자동 검사하며 통과 (score 0..100, view()에 정답 없음)
2) git diff --stat에 [제약]의 파일이 없다
3) 브라우저에서 이 유형으로 한 판 완주
```

---

## 막혔을 때

### 버그

```
[증상] {기대값과 실제값}
[재현] {어떤 요청/조작}
[로그] {스택트레이스 전문}

먼저 이 버그를 재현하는 실패하는 테스트를 써라.
원인을 설명한 다음 고쳐라. 관련 없는 코드는 건드리지 마라.
```

### 설계가 흔들릴 때

```
docs/00-DECISIONS.md의 {D-번호} 를 다시 검토하고 싶다.
지금 {이런 상황}을 만났는데 이 결정으로는 {이런 문제}가 생긴다.

선택지를 2~3개 제시하고 각각의 비용을 말해라. 아직 코드를 고치지 마라.
결론이 나면 결정 로그에 새 D-번호로 적고, 기존 결정에는 [개정됨 → D-xxx]를 표시한다.
```

### 코드가 커졌을 때

```
{파일 경로}를 읽어라.
하는 일을 유지하면서 절반 길이로 줄일 수 있는지 검토해라.
줄일 수 없다면 이유를 말하고 그대로 두어라.
```

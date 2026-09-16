/**
 * 서버 응답 타입은 이 파일 한 곳에 모은다 (CLAUDE.md §2.4).
 */

/** `ErrorCode.java`와 값이 같다. HTTP 상태만으로는 못 가르는 경우를 이걸로 가른다. */
export type ErrorCode =
  | "NOT_FOUND"
  | "INVALID_REQUEST"
  | "FORBIDDEN"
  | "EXTERNAL_API_ERROR"
  | "CONFLICT"
  | "COLLECTION_IN_PROGRESS";

/**
 * 서버가 실패할 때 주는 본문. `ErrorResponse.java`(record)와 필드가 같다.
 * `missingConditions`는 PUBLISHED 전환 조건 미충족(D-062, B1)일 때만 채워진다.
 */
export interface ErrorResponse {
  message: string;
  errorCode: ErrorCode;
  missingConditions?: string[];
}

/** GET /api/v1/admin/me 응답. `AdminMeResponse.java`(record)와 필드가 같다. */
export interface AdminMeResponse {
  email: string;
  authorities: string[];
}

/** `VideoCollectionStatus.java`와 값이 같다 (P1-3-3, D-060). */
export type VideoCollectionStatus =
  "UNCOLLECTED" | "COLLECTED" | "FAILED" | "EXCLUDED";

/** `VideoKind.java`와 값이 같다. 등록 직후엔 UNDEFINED다. */
export type VideoKind =
  "UNDEFINED" | "ORIGINAL" | "MV" | "SELF_COVER" | "COVER" | "LIVE" | "OTHER";

/** `/api/v1/admin/videos` 응답. `AdminVideoResponse.java`(record)와 필드가 같다. */
export interface AdminVideoResponse {
  id: number;
  youtubeVideoId: string;
  collectionStatus: VideoCollectionStatus;
  kind: VideoKind;
  playable: boolean;
  songId: number | null;
  titleSnapshot: string | null;
  durationSec: number | null;
  viewCount: number | null;
  publishedAt: string | null;
  channelName: string | null;
  excludeReason: string | null;
}

/** `SongStatus.java`와 값이 같다. */
export type SongStatus = "DRAFT" | "PUBLISHED";

/** `GET /api/v1/admin/languages` 응답 원소. `AdminLanguageResponse.java`(record)와 필드가 같다. */
export interface AdminLanguageResponse {
  id: number;
  code: string;
  name: string;
}

/** `/api/v1/admin/producers` 응답. `AdminProducerResponse.java`(record)와 필드가 같다. */
export interface AdminProducerResponse {
  id: number;
  name: string;
}

/** `POST /api/v1/admin/answer-patterns/check` 응답. `CheckAnswerPatternResponse.java`(record)와 필드가 같다 (D-052). */
export interface CheckAnswerPatternResponse {
  results: string[];
  count: number;
  warning: boolean;
}

/** `POST /api/v1/admin/songs` 응답. `AdminSongResponse.java`(record)와 필드가 같다. */
export interface AdminSongResponse {
  id: number;
  originalLanguageId: number;
  status: SongStatus;
}

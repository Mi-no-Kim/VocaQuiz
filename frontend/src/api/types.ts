/**
 * 서버 응답 타입은 이 파일 한 곳에 모은다 (CLAUDE.md §2.4).
 */

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

/** `/api/v1/admin/videos` 응답. `VideoResponse.java`(record)와 필드가 같다. */
export interface VideoResponse {
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

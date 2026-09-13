/**
 * 서버 응답 타입은 이 파일 한 곳에 모은다 (CLAUDE.md §2.4).
 */

/** GET /api/v1/admin/me 응답. `AdminMeResponse.java`(record)와 필드가 같다. */
export interface AdminMeResponse {
  email: string;
  authorities: string[];
}

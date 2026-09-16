import { apiFetch } from "@/api/client";
import type { AdminSongResponse, SongStatus } from "@/api/types";

export interface CreateSongNameInput {
  languageId: number;
  name: string;
  primary: boolean;
}

export interface CreateSongInput {
  originalLanguageId: number;
  names: CreateSongNameInput[];
  languageIds: number[];
  producerIds: number[];
  answerPattern: string | null;
  status: SongStatus;
}

/**
 * 곡을 만든다. 원제 언어의 대표 이름이 없으면 400(INVALID_REQUEST, D-061). status로
 * PUBLISHED를 주고 조건(D-062)을 못 채우면 400 + missingConditions다 (B1) — 새로 만드는
 * 곡은 수집된 ORIGINAL 영상이 있을 수 없어 이 경로로는 항상 걸린다(서버 쪽 제약).
 */
export function createSong(input: CreateSongInput): Promise<AdminSongResponse> {
  return apiFetch<AdminSongResponse>("/api/v1/admin/songs", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

import { apiFetch } from "@/api/client";
import type {
  AdminSongDetailResponse,
  AdminSongResponse,
  SongStatus,
} from "@/api/types";

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

/** `id`가 없으면 새 이름, 있으면 그 행을 고친다 — 요청에 없는 기존 id는 서버가 삭제로 본다 (B2). */
export interface UpdateSongNameInput {
  id: number | null;
  languageId: number;
  name: string;
  primary: boolean;
}

export interface UpdateSongInput {
  originalLanguageId: number;
  names: UpdateSongNameInput[];
  languageIds: number[];
  producerIds: number[];
  answerPattern: string | null;
  status: SongStatus;
}

/** 곡 하나의 전체 상세를 가져온다 — 수정 화면이 초기값으로 쓴다. 없으면 404(NOT_FOUND). */
export function fetchSong(id: number): Promise<AdminSongDetailResponse> {
  return apiFetch<AdminSongDetailResponse>(`/api/v1/admin/songs/${id}`);
}

/**
 * 곡을 고친다 — PUT 전체교체다 (B2). `answerPattern`을 비우면(null) 기존 패턴을 지운다.
 * status로 PUBLISHED를 주고 조건(D-062)을 못 채우면 400 + missingConditions다 (B1).
 */
export function updateSong(
  id: number,
  input: UpdateSongInput,
): Promise<AdminSongDetailResponse> {
  return apiFetch<AdminSongDetailResponse>(`/api/v1/admin/songs/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

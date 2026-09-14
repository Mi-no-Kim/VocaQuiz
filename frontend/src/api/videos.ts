import { apiFetch } from "@/api/client";
import type { VideoCollectionStatus, VideoResponse } from "@/api/types";

const BASE = "/api/v1/admin/videos";

/** song이 없는 영상만 온다 — 곡에 붙은 순간부터는 영상 편집(P1-3-6) 화면의 몫이다. */
export function fetchVideos(
  status?: VideoCollectionStatus,
): Promise<VideoResponse[]> {
  const query = status ? `?status=${status}` : "";
  return apiFetch<VideoResponse[]>(`${BASE}${query}`);
}

/** URL(videoId)만으로 미수집 영상을 만든다. 이미 있으면 409(CONFLICT). */
export function addVideo(youtubeVideoId: string): Promise<VideoResponse> {
  return apiFetch<VideoResponse>(BASE, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ videoId: youtubeVideoId }),
  });
}

/** 미수집 전체를 즉시 수집한다. 이미 도는 중이면 409(COLLECTION_IN_PROGRESS). */
export function collectPendingVideos(): Promise<void> {
  return apiFetch<void>(`${BASE}/fetch`, { method: "POST" });
}

export function requeueVideo(id: number): Promise<void> {
  return apiFetch<void>(`${BASE}/${id}/requeue`, { method: "POST" });
}

/** 사유를 반드시 받는다 — 제외 모달에서 입력한 값을 그대로 넘긴다. */
export function excludeVideo(id: number, reason: string): Promise<void> {
  return apiFetch<void>(`${BASE}/${id}/exclude`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reason }),
  });
}

/** EXCLUDED를 취소한다. */
export function restoreVideo(id: number): Promise<void> {
  return apiFetch<void>(`${BASE}/${id}/restore`, { method: "POST" });
}

/** 출제 목록에서 뺀다/다시 켠다 — playable 토글. 화면에는 아직 노출하지 않는다(P1-3-6부터). */
export function enableVideo(id: number): Promise<void> {
  return apiFetch<void>(`${BASE}/${id}/enable`, { method: "POST" });
}

export function disableVideo(id: number): Promise<void> {
  return apiFetch<void>(`${BASE}/${id}/disable`, { method: "POST" });
}

export function deleteVideo(id: number): Promise<void> {
  return apiFetch<void>(`${BASE}/${id}`, { method: "DELETE" });
}

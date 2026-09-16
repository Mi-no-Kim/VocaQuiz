import { apiFetch } from "@/api/client";
import type { AdminLanguageResponse } from "@/api/types";

/** 언어 참조 목록 (D-036) — 거의 안 바뀌니 화면마다 한 번만 불러 쓴다. */
export function fetchLanguages(): Promise<AdminLanguageResponse[]> {
  return apiFetch<AdminLanguageResponse[]>("/api/v1/admin/languages");
}

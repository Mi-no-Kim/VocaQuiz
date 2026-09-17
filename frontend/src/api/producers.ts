import { apiFetch } from "@/api/client";
import type { AdminProducerResponse } from "@/api/types";

const BASE = "/api/v1/admin/producers";

export interface CreateProducerNameInput {
  languageId: number;
  name: string;
  primary: boolean;
}

/**
 * 자동완성 검색 (D-063, D-073). 빈 문자열이면 전체를 돌려준다. 언어 구분 없이 모든
 * 언어의 이름을 다 뒤진다.
 */
export function searchProducers(q: string): Promise<AdminProducerResponse[]> {
  const query = q.trim() ? `?q=${encodeURIComponent(q.trim())}` : "";
  return apiFetch<AdminProducerResponse[]>(`${BASE}${query}`);
}

/**
 * 이름은 1개 이상이어야 한다. 언어 안에서 같은 표기가 이미 있으면 409(CONFLICT)다
 * (D-063, D-073) — 다른 언어에서는 같은 표기를 다시 써도 된다.
 */
export function createProducer(
  names: CreateProducerNameInput[],
): Promise<AdminProducerResponse> {
  return apiFetch<AdminProducerResponse>(BASE, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ names }),
  });
}

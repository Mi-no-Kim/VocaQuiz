import { apiFetch } from "@/api/client";
import type { AdminProducerResponse } from "@/api/types";

const BASE = "/api/v1/admin/producers";

/** 자동완성 검색 (D-063). 빈 문자열이면 전체를 돌려준다. */
export function searchProducers(q: string): Promise<AdminProducerResponse[]> {
  const query = q.trim() ? `?q=${encodeURIComponent(q.trim())}` : "";
  return apiFetch<AdminProducerResponse[]>(`${BASE}${query}`);
}

/** `producer.name`은 UNIQUE라 이미 있는 이름이면 409(CONFLICT)다 (D-063). */
export function createProducer(name: string): Promise<AdminProducerResponse> {
  return apiFetch<AdminProducerResponse>(BASE, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name }),
  });
}

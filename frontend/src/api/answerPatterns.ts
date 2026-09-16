import { apiFetch } from "@/api/client";
import type { CheckAnswerPatternResponse } from "@/api/types";

/**
 * 정답 패턴 원문을 저장하기 전에 미리 펼쳐 본다 (D-052). 문법이 틀리면 ApiError
 * (INVALID_REQUEST)로 던져진다 — 서버가 곡과 무관하게 문자열만으로 판단한다.
 */
export function checkAnswerPattern(
  pattern: string,
): Promise<CheckAnswerPatternResponse> {
  return apiFetch<CheckAnswerPatternResponse>(
    "/api/v1/admin/answer-patterns/check",
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ pattern }),
    },
  );
}

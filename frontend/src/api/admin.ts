import { apiFetch } from "@/api/client";
import type { AdminMeResponse } from "@/api/types";

export function fetchAdminMe(): Promise<AdminMeResponse> {
  return apiFetch<AdminMeResponse>("/api/v1/admin/me");
}

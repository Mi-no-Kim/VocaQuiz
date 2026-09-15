/**
 * API 호출은 이 파일의 함수로만 한다. 컴포넌트에서 fetch를 직접 호출하지 않는다 (CLAUDE.md §2.4).
 *
 * CSRF: 서버는 csrf.spa()를 쓴다 (D-065) — 토큰은 XSRF-TOKEN 쿠키로 오고,
 * X-XSRF-TOKEN 헤더로 되돌려 보내야 한다. 이 헤더를 붙이는 곳은 여기 한 곳뿐이다.
 */

import type { ErrorCode, ErrorResponse } from "@/api/types";

const XSRF_COOKIE_NAME = "XSRF-TOKEN";
const XSRF_HEADER_NAME = "X-XSRF-TOKEN";

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

/**
 * 401(로그인 필요)·403(권한 없음)을 구분해서 던지는 에러 (D-065).
 *
 * <p>`errorCode`는 서버 `ErrorResponse`의 것이고, 상태 코드만으로는 못 가르는 경우를
 * 위해 있다 — 예를 들어 CONFLICT와 COLLECTION_IN_PROGRESS는 둘 다 409다.
 * 서버가 `ErrorResponse`가 아닌 본문을 주면(프록시 오류 등) null이다.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly errorCode: ErrorCode | null;

  constructor(
    status: number,
    message: string,
    errorCode: ErrorCode | null = null,
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.errorCode = errorCode;
  }
}

/**
 * 실패 응답을 ApiError로 바꾼다. 본문이 ErrorResponse면 message·errorCode를 살리고,
 * 아니면 상태 코드만 들고 간다 — 서버·프록시·브라우저 어느 단계에서든 우리 모양이
 * 아닌 본문이 올 수 있으므로 파싱 실패를 정상 경로로 다룬다.
 */
async function toApiError(response: Response): Promise<ApiError> {
  const text = await response.text().catch(() => "");

  if (text) {
    try {
      const body = JSON.parse(text) as Partial<ErrorResponse>;
      if (body.errorCode) {
        return new ApiError(
          response.status,
          body.message ?? response.statusText,
          body.errorCode,
        );
      }
    } catch {
      // ErrorResponse가 아니다 — 아래 기본 경로로 간다.
    }
  }

  return new ApiError(response.status, response.statusText);
}

/**
 * 관리자 API 호출 공통 함수.
 * GET 외의 메서드를 쓰게 되면(P1-3-3 이후) XSRF 헤더가 이미 여기서 붙는다.
 */
export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  const xsrfToken = readCookie(XSRF_COOKIE_NAME);
  if (xsrfToken) {
    headers.set(XSRF_HEADER_NAME, xsrfToken);
  }

  const response = await fetch(path, {
    ...init,
    headers,
    credentials: "same-origin",
  });

  if (!response.ok) {
    throw await toApiError(response);
  }

  // 본문 없이 200/204만 돌려주는 엔드포인트가 있다(P1-3-3의 상태 전이 API들) —
  // 상태 코드로 가르지 않고 실제 본문이 비어 있는지로 판단한다.
  const text = await response.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

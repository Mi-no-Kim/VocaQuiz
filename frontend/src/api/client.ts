/**
 * API 호출은 이 파일의 함수로만 한다. 컴포넌트에서 fetch를 직접 호출하지 않는다 (CLAUDE.md §2.4).
 *
 * CSRF: 서버는 csrf.spa()를 쓴다 (D-065) — 토큰은 XSRF-TOKEN 쿠키로 오고,
 * X-XSRF-TOKEN 헤더로 되돌려 보내야 한다. 이 헤더를 붙이는 곳은 여기 한 곳뿐이다.
 */

const XSRF_COOKIE_NAME = "XSRF-TOKEN";
const XSRF_HEADER_NAME = "X-XSRF-TOKEN";

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

/** 401(로그인 필요)·403(권한 없음)을 구분해서 던지는 에러 (D-065). */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
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
    throw new ApiError(response.status, response.statusText);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

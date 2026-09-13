/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 구글 로그인 시작 주소 (D-065, P1-3-2). 백엔드 8080에서 로그인한다. */
  readonly VITE_GOOGLE_LOGIN_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

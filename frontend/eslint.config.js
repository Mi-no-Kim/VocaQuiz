import js from "@eslint/js";
import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import tseslint from "typescript-eslint";
import { defineConfig, globalIgnores } from "eslint/config";

export default defineConfig([
  globalIgnores(["dist"]),
  {
    files: ["**/*.{ts,tsx}"],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
  },
  {
    // shadcn CLI가 생성·재생성하는 파일이다. 컴포넌트와 cva 결과(buttonVariants 등)를
    // 한 파일에서 같이 내보내는 게 shadcn의 기본 패턴이라 fast refresh 규칙과 구조적으로 안 맞는다.
    // 파일을 쪼개도 다음 `shadcn add` 때 다시 합쳐지므로, 이 폴더에서만 규칙을 끈다.
    files: ["src/components/ui/**/*.{ts,tsx}"],
    rules: {
      "react-refresh/only-export-components": "off",
    },
  },
]);

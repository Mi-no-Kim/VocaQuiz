import { useEffect, useState, type ReactNode } from "react";
import { fetchAdminMe } from "@/api/admin";
import { ApiError } from "@/api/client";
import type { AdminMeResponse } from "@/api/types";
import { buttonVariants } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

const GOOGLE_LOGIN_URL = import.meta.env.VITE_GOOGLE_LOGIN_URL;

type GateState =
  | { status: "loading" }
  | { status: "unauthenticated" }
  | { status: "forbidden" }
  | { status: "ready"; me: AdminMeResponse };

/**
 * 관리자 화면 공통 틀 (P1-3-2).
 * 들어올 때 GET /api/v1/admin/me를 불러 401/403/200 세 갈래로 나눈다 (D-065).
 * 화면(기능)은 아직 없다 — 이 단계의 제약이다.
 */
export function AdminGate({ children }: { children: ReactNode }) {
  const [state, setState] = useState<GateState>({ status: "loading" });

  useEffect(() => {
    let cancelled = false;

    fetchAdminMe()
      .then((me) => {
        if (!cancelled) setState({ status: "ready", me });
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        if (error instanceof ApiError && error.status === 401) {
          setState({ status: "unauthenticated" });
          return;
        }
        if (error instanceof ApiError && error.status === 403) {
          setState({ status: "forbidden" });
          return;
        }
        // 401/403 외의 실패(네트워크 오류 등)는 이 단계의 범위 밖이다.
        throw error;
      });

    return () => {
      cancelled = true;
    };
  }, []);

  if (state.status === "loading") {
    return null;
  }

  if (state.status === "unauthenticated") {
    return (
      <GateScreen
        title="로그인이 필요합니다"
        description="관리자 화면을 쓰려면 구글 계정으로 로그인해야 합니다."
      >
        <a
          className={buttonVariants({ variant: "default" })}
          href={GOOGLE_LOGIN_URL}
        >
          구글로 로그인
        </a>
      </GateScreen>
    );
  }

  if (state.status === "forbidden") {
    return (
      <GateScreen
        title="관리자 권한이 없습니다"
        description="이 계정은 관리자로 등록돼 있지 않습니다."
      />
    );
  }

  return (
    <div className="min-h-svh">
      <header className="flex items-center justify-end border-b border-border px-6 py-3 text-sm text-muted-foreground">
        {state.me.email}
      </header>
      {children}
    </div>
  );
}

function GateScreen({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children?: ReactNode;
}) {
  return (
    <div className="flex min-h-svh items-center justify-center p-6">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>{title}</CardTitle>
          <CardDescription>{description}</CardDescription>
        </CardHeader>
        {children ? <CardContent>{children}</CardContent> : null}
      </Card>
    </div>
  );
}

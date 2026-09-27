import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router";
import { fetchSongs } from "@/api/songs";
import type {
  AdminPageResponse,
  AdminSongListItemResponse,
  AdminSongSort,
  SongStatus,
} from "@/api/types";
import { DisplayName } from "@/components/DisplayName";
import { Button } from "@/components/ui/button";

const STATUS_OPTIONS: { value: SongStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "DRAFT", label: "초안" },
  { value: "PUBLISHED", label: "공개" },
];

const STATUS_LABEL: Record<SongStatus, string> = {
  DRAFT: "초안",
  PUBLISHED: "공개",
};

const SORT_OPTIONS: { value: AdminSongSort; label: string }[] = [
  { value: "LATEST", label: "최신순" },
  { value: "OLDEST", label: "오래된순" },
];

function formatDate(iso: string): string {
  return iso.slice(0, 10);
}

/**
 * 관리자 곡 목록·검색 화면 (P1-3-5). 검색어는 정답 패턴(D-056)에서만 찾는다 —
 * 곡 이름으로는 찾지 않는다. 페이지는 50개씩, "미작업" 사유는 목록에 그대로 나열한다.
 */
export function AdminSongListPage() {
  const [qInput, setQInput] = useState("");
  const [q, setQ] = useState("");
  const [statusFilter, setStatusFilter] = useState<SongStatus | "ALL">("ALL");
  const [sort, setSort] = useState<AdminSongSort>("LATEST");
  const [page, setPage] = useState(0);
  const [pageData, setPageData] =
    useState<AdminPageResponse<AdminSongListItemResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<string | null>(null);

  // 검색어가 바뀔 때마다 바로 조회하지 않고 잠깐 기다린다 — AdminSongNewPage의
  // 프로듀서 검색과 같은 패턴. setState는 전부 타임아웃 콜백(비동기 경계) 안에서만
  // 부른다 — effect 본문에서 곧바로 부르지 않는다(react-hooks/set-state-in-effect).
  useEffect(() => {
    const timer = setTimeout(() => {
      setQ(qInput.trim());
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [qInput]);

  // 로딩 표시는 처음 한 번(초기값 true)만 뜨고, 이후 필터를 바꾸거나 페이지를
  // 넘길 때는 목록이 갱신될 때 조용히 바뀐다 — AdminVideosPage의 refresh와 같은 패턴.
  const refresh = useCallback(() => {
    fetchSongs({
      q: q || undefined,
      status: statusFilter === "ALL" ? undefined : statusFilter,
      sort,
      page,
    })
      .then(setPageData)
      .catch(() => setMessage("목록을 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [q, statusFilter, sort, page]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function handleStatusChange(next: SongStatus | "ALL") {
    setStatusFilter(next);
    setPage(0);
  }

  function handleSortChange(next: AdminSongSort) {
    setSort(next);
    setPage(0);
  }

  const items = pageData?.content ?? [];
  const totalPages = pageData?.page.totalPages ?? 0;

  return (
    <div className="mx-auto max-w-6xl space-y-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-medium">곡 목록</h1>
        <Link
          to="/admin/songs/new"
          className="text-sm text-muted-foreground hover:text-foreground"
        >
          곡 만들기
        </Link>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <input
          value={qInput}
          onChange={(event) => setQInput(event.target.value)}
          placeholder="정답 패턴으로 검색 (D-056)"
          className="h-8 min-w-64 flex-1 rounded-lg border border-border bg-background px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        />
        <select
          value={statusFilter}
          onChange={(event) =>
            handleStatusChange(event.target.value as SongStatus | "ALL")
          }
          className="h-8 rounded-lg border border-border bg-background px-2.5 text-sm"
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <select
          value={sort}
          onChange={(event) =>
            handleSortChange(event.target.value as AdminSongSort)
          }
          className="h-8 rounded-lg border border-border bg-background px-2.5 text-sm"
        >
          {SORT_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      {message ? <p className="text-sm text-destructive">{message}</p> : null}

      {loading ? (
        <p className="text-sm text-muted-foreground">불러오는 중...</p>
      ) : items.length === 0 ? (
        <p className="text-sm text-muted-foreground">곡이 없습니다.</p>
      ) : (
        <table className="w-full table-fixed text-sm">
          <colgroup>
            <col className="w-16" />
            <col className="w-64" />
            <col />
            <col className="w-24" />
            <col className="w-16" />
          </colgroup>
          <thead>
            <tr className="border-b border-border text-left text-muted-foreground">
              <th className="py-2 font-normal">상태</th>
              <th className="py-2 font-normal">이름</th>
              <th className="py-2 font-normal">미작업 사유</th>
              <th className="py-2 font-normal">생성일</th>
              <th className="py-2 font-normal" />
            </tr>
          </thead>
          <tbody>
            {items.map((song) => (
              <tr key={song.id} className="border-b border-border/50">
                <td className="py-2">{STATUS_LABEL[song.status]}</td>
                <td className="py-2 pr-2">
                  <DisplayName name={song.name} />
                </td>
                <td className="py-2 pr-2">
                  {song.missingConditions.length > 0 ? (
                    <span className="text-xs text-destructive">
                      {song.missingConditions.join(" · ")}
                    </span>
                  ) : (
                    <span className="text-xs text-muted-foreground">완료</span>
                  )}
                </td>
                <td className="py-2 text-xs text-muted-foreground">
                  {formatDate(song.createdAt)}
                </td>
                <td className="py-2 text-right">
                  <Link
                    to={`/admin/songs/${song.id}`}
                    className="text-xs text-muted-foreground hover:text-foreground"
                  >
                    편집
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {totalPages > 1 ? (
        <div className="flex items-center justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((prev) => prev - 1)}
          >
            이전
          </Button>
          <span className="text-xs text-muted-foreground">
            {page + 1} / {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((prev) => prev + 1)}
          >
            다음
          </Button>
        </div>
      ) : null}
    </div>
  );
}

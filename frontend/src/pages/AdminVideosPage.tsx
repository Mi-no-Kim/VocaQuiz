import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ApiError } from "@/api/client";
import type { AdminVideoResponse, VideoCollectionStatus } from "@/api/types";
import {
  addVideo,
  collectPendingVideos,
  deleteVideo,
  excludeVideo,
  fetchVideos,
  requeueVideo,
  restoreVideo,
} from "@/api/videos";
import { extractYoutubeVideoId } from "@/lib/youtube";
import { Button } from "@/components/ui/button";

const STATUS_OPTIONS: {
  value: VideoCollectionStatus | "ALL";
  label: string;
}[] = [
  { value: "ALL", label: "전체" },
  { value: "UNCOLLECTED", label: "미수집" },
  { value: "COLLECTED", label: "수집됨" },
  { value: "FAILED", label: "실패" },
  { value: "EXCLUDED", label: "제외" },
];

const STATUS_LABEL: Record<VideoCollectionStatus, string> = {
  UNCOLLECTED: "미수집",
  COLLECTED: "수집됨",
  FAILED: "실패",
  EXCLUDED: "제외",
};

function formatDuration(sec: number | null): string {
  if (sec == null) return "-";
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

/**
 * 영상 추가·수집 화면 (P1-3-3).
 *
 * "출제" 토글은 화면에서 뺐다 — 영상이 곡과 매칭되기 시작하는 시점(P1-3-6)부터
 * 다시 다룬다. 백엔드 API(enable/disable)는 그대로 남아 있다.
 */
export function AdminVideosPage() {
  const [statusFilter, setStatusFilter] = useState<
    VideoCollectionStatus | "ALL"
  >("ALL");
  const [videos, setVideos] = useState<AdminVideoResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<string | null>(null);
  const [urlInput, setUrlInput] = useState("");
  const [collecting, setCollecting] = useState(false);
  const [excludeTarget, setExcludeTarget] = useState<AdminVideoResponse | null>(
    null,
  );

  // effect 안에서 곧바로 setState를 부르면 안 된다는 lint 규칙(react-hooks/set-state-in-effect)
  // 때문에, 여기서는 setLoading(true)를 동기로 부르지 않는다 — 로딩 표시는 처음 한 번(초기값
  // true)만 뜨고, 이후 필터를 바꾸거나 액션 후 다시 부를 때는 목록이 갱신될 때 조용히 바뀐다.
  const refresh = useCallback(() => {
    fetchVideos(statusFilter === "ALL" ? undefined : statusFilter)
      .then(setVideos)
      .catch(() => setMessage("목록을 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [statusFilter]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function handleAdd(event: FormEvent) {
    event.preventDefault();
    const videoId = extractYoutubeVideoId(urlInput);
    if (!videoId) {
      setMessage("올바른 유튜브 URL이 아닙니다.");
      return;
    }
    try {
      await addVideo(videoId);
      setUrlInput("");
      setMessage(null);
      refresh();
    } catch (error) {
      if (error instanceof ApiError && error.errorCode === "CONFLICT") {
        setMessage("이미 등록된 영상입니다.");
        return;
      }
      setMessage("등록에 실패했습니다.");
    }
  }

  async function handleCollect() {
    setCollecting(true);
    setMessage(null);
    try {
      await collectPendingVideos();
      refresh();
    } catch (error) {
      if (
        error instanceof ApiError &&
        error.errorCode === "COLLECTION_IN_PROGRESS"
      ) {
        setMessage("이미 수집이 진행 중입니다.");
        return;
      }
      setMessage("수집에 실패했습니다.");
    } finally {
      setCollecting(false);
    }
  }

  function handleAction(action: () => Promise<void>) {
    action()
      .then(refresh)
      .catch(() => setMessage("요청이 실패했습니다."));
  }

  function handleExcludeConfirm(reason: string) {
    if (!excludeTarget) return;
    const targetId = excludeTarget.id;
    setExcludeTarget(null);
    handleAction(() => excludeVideo(targetId, reason));
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6 p-6">
      <h1 className="text-lg font-medium">영상 추가·수집</h1>

      <form onSubmit={handleAdd} className="flex gap-2">
        <input
          value={urlInput}
          onChange={(event) => setUrlInput(event.target.value)}
          placeholder="유튜브 URL을 붙여넣으세요"
          className="h-8 flex-1 rounded-lg border border-border bg-background px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        />
        <Button type="submit">추가</Button>
      </form>

      <div className="flex items-center justify-between gap-2">
        <select
          value={statusFilter}
          onChange={(event) =>
            setStatusFilter(event.target.value as VideoCollectionStatus | "ALL")
          }
          className="h-8 rounded-lg border border-border bg-background px-2.5 text-sm"
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <Button variant="outline" onClick={handleCollect} disabled={collecting}>
          {collecting ? "수집 중..." : "지금 수집"}
        </Button>
      </div>

      {message ? <p className="text-sm text-destructive">{message}</p> : null}

      {loading ? (
        <p className="text-sm text-muted-foreground">불러오는 중...</p>
      ) : videos.length === 0 ? (
        <p className="text-sm text-muted-foreground">영상이 없습니다.</p>
      ) : (
        <table className="w-full table-fixed text-sm">
          {/*
            table-layout: fixed + colgroup으로 컬럼 폭을 미리 고정한다. auto 레이아웃에서
            제목 칸에 max-width만 주는 방식은 내용에 따라 td 자체 폭이 흔들리는 문제가 있어서
            (피드백, 2026-09-14) 폭을 아예 레이아웃 단계에서 못 박는 쪽으로 바꿨다.
          */}
          <colgroup>
            <col className="w-20" />
            <col className="w-[550px]" />
            <col className="w-32" />
            <col className="w-16" />
            <col className="w-32" />
          </colgroup>
          <thead>
            <tr className="border-b border-border text-left text-muted-foreground">
              <th className="py-2 font-normal">상태</th>
              <th className="py-2 font-normal">제목</th>
              <th className="py-2 font-normal">채널</th>
              <th className="py-2 font-normal">길이</th>
              <th className="py-2 font-normal" />
            </tr>
          </thead>
          <tbody>
            {videos.map((video) => (
              <VideoRow
                key={video.id}
                video={video}
                onAction={handleAction}
                onRequestExclude={() => setExcludeTarget(video)}
              />
            ))}
          </tbody>
        </table>
      )}

      {excludeTarget ? (
        <ExcludeDialog
          video={excludeTarget}
          onCancel={() => setExcludeTarget(null)}
          onConfirm={handleExcludeConfirm}
        />
      ) : null}
    </div>
  );
}

function VideoRow({
  video,
  onAction,
  onRequestExclude,
}: {
  video: AdminVideoResponse;
  onAction: (action: () => Promise<void>) => void;
  onRequestExclude: () => void;
}) {
  return (
    <tr className="border-b border-border/50">
      <td className="py-2">{STATUS_LABEL[video.collectionStatus]}</td>
      <td className="py-2 pr-2">
        <div
          className="truncate"
          title={video.titleSnapshot ?? video.youtubeVideoId}
        >
          {video.titleSnapshot ?? video.youtubeVideoId}
        </div>
        {video.collectionStatus === "EXCLUDED" && video.excludeReason ? (
          <p
            className="truncate text-xs text-muted-foreground"
            title={`제외 사유: ${video.excludeReason}`}
          >
            제외 사유: {video.excludeReason}
          </p>
        ) : null}
      </td>
      <td className="py-2 pr-2">
        <div className="truncate" title={video.channelName ?? "-"}>
          {video.channelName ?? "-"}
        </div>
      </td>
      <td className="py-2">{formatDuration(video.durationSec)}</td>
      <td className="py-2 text-right">
        <div className="inline-flex flex-wrap justify-end gap-1">
          {video.collectionStatus === "FAILED" ? (
            <Button
              variant="outline"
              size="sm"
              onClick={() => onAction(() => requeueVideo(video.id))}
            >
              다시 대기
            </Button>
          ) : null}
          {video.collectionStatus === "EXCLUDED" ? (
            <Button
              variant="outline"
              size="sm"
              onClick={() => onAction(() => restoreVideo(video.id))}
            >
              복구
            </Button>
          ) : (
            <Button variant="outline" size="sm" onClick={onRequestExclude}>
              제외
            </Button>
          )}
          <Button
            variant="destructive"
            size="sm"
            onClick={() => {
              if (window.confirm("정말 삭제할까요? 되돌릴 수 없습니다.")) {
                onAction(() => deleteVideo(video.id));
              }
            }}
          >
            삭제
          </Button>
        </div>
      </td>
    </tr>
  );
}

/**
 * 제외 사유를 받는 모달. `@base-ui/react`의 dialog 프리미티브를 쓰지 않고 직접
 * 만들었다 — 이 세션에서는 빌드를 돌려 API 표면을 확인할 수 없어서, 추측으로 쓰다
 * 틀리는 위험보다 고정 오버레이 + 평범한 state로 최소하게 만드는 쪽을 택했다.
 */
function ExcludeDialog({
  video,
  onCancel,
  onConfirm,
}: {
  video: AdminVideoResponse;
  onCancel: () => void;
  onConfirm: (reason: string) => void;
}) {
  const [reason, setReason] = useState("");
  const title = video.titleSnapshot ?? video.youtubeVideoId;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-sm space-y-3 rounded-lg bg-background p-4 shadow-lg">
        <p className="text-sm font-medium">
          [{title}] 영상을 제외하는 사유를 작성해주세요
        </p>
        <textarea
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          rows={3}
          autoFocus
          className="w-full resize-none rounded-lg border border-border bg-background px-2.5 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        />
        <div className="flex justify-end gap-2">
          <Button variant="outline" size="sm" onClick={onCancel}>
            취소
          </Button>
          <Button
            variant="destructive"
            size="sm"
            disabled={reason.trim().length === 0}
            onClick={() => onConfirm(reason.trim())}
          >
            제외
          </Button>
        </div>
      </div>
    </div>
  );
}

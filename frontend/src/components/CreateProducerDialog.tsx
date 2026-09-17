import { useState } from "react";
import { ApiError } from "@/api/client";
import { createProducer, type CreateProducerNameInput } from "@/api/producers";
import type { AdminLanguageResponse, AdminProducerResponse } from "@/api/types";
import { Button } from "@/components/ui/button";

const fieldClass =
  "h-8 w-full rounded-lg border border-border bg-background px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50";

interface NameRow {
  languageId: number;
  name: string;
  included: boolean;
}

function initialRows(languages: AdminLanguageResponse[]): NameRow[] {
  return languages.map((language) => ({
    languageId: language.id,
    name: "",
    included: true,
  }));
}

/**
 * 프로듀서 만들기 모달. 곡 만들기 화면(`AdminSongNewPage`)의 "이름" 카드와 같은 모양이다 —
 * 언어마다 정확히 1행, 필요 없는 언어는 ×로 뺀다. 이름이 있는 언어마다 그 이름이 대표가
 * 되고(D-073), 이름은 1개 이상 있어야 한다.
 *
 * <p>언어 하나에 이름을 여러 개(별칭) 넣는 "이름 추가" 버튼은 지금 범위가 아니다 — 나중에
 * 붙을 자리로 남겨 둔다.
 *
 * <p>`@base-ui/react`의 dialog 프리미티브를 쓰지 않고 `AdminVideosPage`의 `ExcludeDialog`와
 * 같은 방식(고정 오버레이 + 평범한 state)으로 직접 만들었다 — 곡 만들기·편집 화면이 똑같이
 * 쓰므로 여기 한 곳으로 뺐다.
 */
export function CreateProducerDialog({
  languages,
  onCancel,
  onCreated,
}: {
  languages: AdminLanguageResponse[];
  onCancel: () => void;
  onCreated: (producer: AdminProducerResponse) => void;
}) {
  const [rows, setRows] = useState<NameRow[]>(() => initialRows(languages));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function updateName(languageId: number, name: string) {
    setRows((prev) =>
      prev.map((row) =>
        row.languageId === languageId ? { ...row, name } : row,
      ),
    );
  }

  function toggleIncluded(languageId: number) {
    setRows((prev) =>
      prev.map((row) =>
        row.languageId === languageId
          ? { ...row, included: !row.included }
          : row,
      ),
    );
  }

  async function handleSubmit() {
    const names: CreateProducerNameInput[] = rows
      .filter((row) => row.included && row.name.trim().length > 0)
      .map((row) => ({
        languageId: row.languageId,
        name: row.name.trim(),
        primary: true,
      }));

    if (names.length === 0) {
      setError("이름을 하나 이상 입력해주세요.");
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const producer = await createProducer(names);
      onCreated(producer);
    } catch (err) {
      if (err instanceof ApiError && err.errorCode === "CONFLICT") {
        setError("이미 있는 이름입니다.");
        return;
      }
      setError("프로듀서를 만들지 못했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md space-y-3 rounded-lg bg-background p-4 shadow-lg">
        <p className="text-sm font-medium">프로듀서 만들기</p>
        <p className="text-xs text-muted-foreground">
          언어마다 이름은 1개까지입니다. 이름은 최소 1개 있어야 합니다 — 필요
          없는 언어는 ×로 빼세요.
        </p>
        <div className="space-y-2">
          {rows.map((row) => {
            const language = languages.find((l) => l.id === row.languageId);

            if (!row.included) {
              return (
                <div
                  key={row.languageId}
                  className="flex items-center gap-2 text-sm text-muted-foreground"
                >
                  <span className="w-20 shrink-0">{language?.name}</span>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => toggleIncluded(row.languageId)}
                  >
                    추가
                  </Button>
                </div>
              );
            }

            return (
              <div key={row.languageId} className="flex items-center gap-2">
                <span className="w-20 shrink-0 text-sm text-muted-foreground">
                  {language?.name}
                </span>
                <input
                  value={row.name}
                  onChange={(event) =>
                    updateName(row.languageId, event.target.value)
                  }
                  placeholder="이름"
                  className={`${fieldClass} min-w-0 flex-1`}
                />
                <Button
                  type="button"
                  variant="outline"
                  size="icon-sm"
                  aria-label={`${language?.name} 이름 빼기`}
                  onClick={() => toggleIncluded(row.languageId)}
                >
                  ×
                </Button>
              </div>
            );
          })}
        </div>
        {error ? <p className="text-xs text-destructive">{error}</p> : null}
        <div className="flex justify-end gap-2">
          <Button type="button" variant="outline" size="sm" onClick={onCancel}>
            취소
          </Button>
          <Button
            type="button"
            size="sm"
            disabled={submitting}
            onClick={handleSubmit}
          >
            만들기
          </Button>
        </div>
      </div>
    </div>
  );
}

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";
import { Link, useParams } from "react-router";
import { ApiError } from "@/api/client";
import { fetchLanguages } from "@/api/languages";
import { checkAnswerPattern } from "@/api/answerPatterns";
import { createProducer, searchProducers } from "@/api/producers";
import { fetchSong, updateSong, type UpdateSongNameInput } from "@/api/songs";
import type {
  AdminLanguageResponse,
  AdminProducerResponse,
  AdminSongDetailResponse,
  CheckAnswerPatternResponse,
  SongStatus,
} from "@/api/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const STATUS_OPTIONS: { value: SongStatus; label: string }[] = [
  { value: "DRAFT", label: "초안" },
  { value: "PUBLISHED", label: "공개" },
];

const fieldClass =
  "h-8 w-full rounded-lg border border-border bg-background px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50";

interface NameRow {
  languageId: number;
  id: number | null;
  name: string;
  included: boolean;
}

/**
 * 언어마다 정확히 1행 — commit 6(`AdminSongNewPage`)과 같은 모양이다. 이미 이름이 있는
 * 언어는 그 값으로 채우고 included를 켠다. 이 화면은 언어당 이름이 이미 1개(대표)뿐인
 * 곡만 정확히 반영한다 — 언어 하나에 이름이 여러 개(예: 비대표 번역명) 있는 데이터는
 * 지금 UI로는 표현할 수 없어 저장 시 그 중 하나만 남고 나머지는 지워진다. 지금까지는
 * 이 화면(과 commit 6)이 이름을 만드는 유일한 경로라 실제로 일어날 수 없다.
 */
function buildNameRows(
  languages: AdminLanguageResponse[],
  existing: AdminSongDetailResponse["names"],
): NameRow[] {
  const byLanguageId = new Map(existing.map((n) => [n.languageId, n]));
  return languages.map((language) => {
    const found = byLanguageId.get(language.id);
    return found
      ? {
          languageId: language.id,
          id: found.id,
          name: found.name,
          included: true,
        }
      : { languageId: language.id, id: null, name: "", included: false };
  });
}

/**
 * 곡 수정 화면 (P1-3-4). commit 6의 곡 만들기 화면과 카드 구성·이름 행 방식을 그대로
 * 따른다 — 다른 점은 서버에서 기존 값을 받아와 채우는 것과, 상태(status)를 여기서
 * 고른다는 것 뿐이다. 새로 만들 때는 항상 DRAFT라(commit 6) 상태를 바꾸는 유일한
 * 경로가 이 화면이다.
 */
export function AdminSongEditPage() {
  const { id } = useParams<{ id: string }>();
  const songId = id ? Number(id) : NaN;

  const [phase, setPhase] = useState<
    "loading" | "ready" | "not-found" | "error"
  >("loading");

  const [languages, setLanguages] = useState<AdminLanguageResponse[]>([]);
  const [allProducers, setAllProducers] = useState<AdminProducerResponse[]>([]);

  const [names, setNames] = useState<NameRow[]>([]);
  const [languageIds, setLanguageIds] = useState<Set<number>>(new Set());

  const [producerQuery, setProducerQuery] = useState("");
  const [producerResults, setProducerResults] = useState<
    AdminProducerResponse[]
  >([]);
  const [selectedProducers, setSelectedProducers] = useState<
    AdminProducerResponse[]
  >([]);
  const [producerMessage, setProducerMessage] = useState<string | null>(null);

  const [answerPattern, setAnswerPattern] = useState("");
  const [patternCheck, setPatternCheck] =
    useState<CheckAnswerPatternResponse | null>(null);
  const [patternError, setPatternError] = useState<string | null>(null);
  const [checkingPattern, setCheckingPattern] = useState(false);

  const [status, setStatus] = useState<SongStatus>("DRAFT");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [missingConditions, setMissingConditions] = useState<string[]>([]);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);

  const answerPatternRef = useRef<HTMLTextAreaElement>(null);

  // 드래그로 늘리는 칸이 아니라, 입력한 줄 수에 맞춰 스스로 커지는 칸이다.
  // ref로 DOM을 직접 건드릴 뿐 setState를 부르지 않으니 set-state-in-effect
  // 규칙에 걸리지 않는다 — React 문서가 이 훅의 전형적인 용례로 드는 것과 같다.
  useEffect(() => {
    const el = answerPatternRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${el.scrollHeight}px`;
  }, [answerPattern]);

  const applyDetail = useCallback(
    (
      detail: AdminSongDetailResponse,
      loadedLanguages: AdminLanguageResponse[],
      loadedProducers: AdminProducerResponse[],
    ) => {
      setNames(buildNameRows(loadedLanguages, detail.names));
      setLanguageIds(new Set(detail.languageIds));
      setSelectedProducers(
        detail.producerIds
          .map((producerId) => loadedProducers.find((p) => p.id === producerId))
          .filter((p): p is AdminProducerResponse => p != null),
      );
      setAnswerPattern(detail.answerPattern ?? "");
      setStatus(detail.status);
    },
    [],
  );

  // effect 안에서 곧바로 setState를 부르면 안 된다는 lint 규칙(react-hooks/set-state-in-effect)
  // 때문에, songId 유효성 검사도 포함해 전부 프라미스 체인 안(비동기 경계)에서만 setState를
  // 부른다 — phase의 "loading"은 useState 초기값으로만 뜨고, 다시 부를 때(재시도)는 조용히
  // 바뀐다. AdminVideosPage.tsx의 refresh()와 같은 패턴이다.
  const load = useCallback(() => {
    Promise.resolve()
      .then(() => {
        if (!Number.isInteger(songId)) {
          throw new Error(`invalid song id: ${id}`);
        }
        return Promise.all([
          fetchLanguages(),
          searchProducers(""),
          fetchSong(songId),
        ]);
      })
      .then(([loadedLanguages, loadedProducers, detail]) => {
        setLanguages(loadedLanguages);
        setAllProducers(loadedProducers);
        applyDetail(detail, loadedLanguages, loadedProducers);
        setPhase("ready");
      })
      .catch((error: unknown) => {
        if (error instanceof ApiError && error.status === 404) {
          setPhase("not-found");
          return;
        }
        setPhase("error");
      });
  }, [songId, id, applyDetail]);

  useEffect(() => {
    load();
  }, [load]);

  // 검색어가 바뀔 때마다 바로 조회하지 않고 잠깐 기다린다. setState는 전부 타임아웃
  // 콜백(비동기 경계) 안에서만 부른다 — effect 본문에서 곧바로 부르지 않는다.
  useEffect(() => {
    const query = producerQuery.trim();
    const timer = setTimeout(() => {
      if (!query) {
        setProducerResults([]);
        return;
      }
      searchProducers(query)
        .then(setProducerResults)
        .catch(() => setProducerMessage("프로듀서 검색에 실패했습니다."));
    }, 300);
    return () => clearTimeout(timer);
  }, [producerQuery]);

  function updateNameText(languageId: number, name: string) {
    setNames((rows) =>
      rows.map((row) =>
        row.languageId === languageId ? { ...row, name } : row,
      ),
    );
  }

  function toggleNameIncluded(languageId: number) {
    setNames((rows) =>
      rows.map((row) =>
        row.languageId === languageId
          ? { ...row, included: !row.included }
          : row,
      ),
    );
  }

  function toggleLanguage(id: number) {
    setLanguageIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  function handleAddProducer(producer: AdminProducerResponse) {
    setSelectedProducers((prev) =>
      prev.some((p) => p.id === producer.id) ? prev : [...prev, producer],
    );
    setProducerQuery("");
    setProducerResults([]);
    setProducerMessage(null);
  }

  function handleRemoveProducer(id: number) {
    setSelectedProducers((prev) => prev.filter((p) => p.id !== id));
  }

  async function handleCreateProducer() {
    const name = producerQuery.trim();
    if (!name) return;
    try {
      const producer = await createProducer(name);
      setAllProducers((prev) => [...prev, producer]);
      handleAddProducer(producer);
    } catch (error) {
      if (error instanceof ApiError && error.errorCode === "CONFLICT") {
        setProducerMessage("이미 있는 이름입니다.");
        return;
      }
      setProducerMessage("프로듀서를 만들지 못했습니다.");
    }
  }

  async function handleCheckPattern() {
    setCheckingPattern(true);
    setPatternError(null);
    try {
      setPatternCheck(await checkAnswerPattern(answerPattern));
    } catch (error) {
      setPatternCheck(null);
      setPatternError(
        error instanceof ApiError ? error.message : "패턴 검사에 실패했습니다.",
      );
    } finally {
      setCheckingPattern(false);
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    setMissingConditions([]);
    setSaveMessage(null);

    const nameInputs: UpdateSongNameInput[] = names
      .filter((row) => row.included && row.name.trim().length > 0)
      .map((row) => ({
        id: row.id,
        languageId: row.languageId,
        name: row.name.trim(),
        primary: true,
      }));

    if (nameInputs.length === 0) {
      setFormError("이름을 하나 이상 입력해주세요.");
      return;
    }

    setSubmitting(true);
    try {
      const detail = await updateSong(songId, {
        names: nameInputs,
        languageIds: [...languageIds],
        producerIds: selectedProducers.map((p) => p.id),
        answerPattern: answerPattern.trim() ? answerPattern : null,
        status,
      });
      // 방금 새로 생긴 이름은 서버가 매긴 id를 여기서 받아 와야 한다 — 안 그러면
      // 다음 저장에서 또 id 없이 같은 이름을 보내 새 행을 만들려다 그 언어의
      // 기존 행(방금 만든 그 행)이 "요청에 없는 id"로 지워지는 꼴이 된다 (B2).
      applyDetail(detail, languages, allProducers);
      setSaveMessage("저장했습니다.");
    } catch (error) {
      if (error instanceof ApiError) {
        setFormError(error.message);
        setMissingConditions(error.missingConditions);
        return;
      }
      setFormError("곡을 고치지 못했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  if (phase === "loading") {
    return (
      <div className="mx-auto max-w-6xl p-6">
        <p className="text-sm text-muted-foreground">불러오는 중...</p>
      </div>
    );
  }

  if (phase === "not-found") {
    return (
      <div className="mx-auto max-w-6xl space-y-4 p-6">
        <h1 className="text-lg font-medium">곡 수정</h1>
        <Card>
          <CardContent className="space-y-3 pt-4">
            <p className="text-sm">곡을 찾을 수 없습니다 (id: {id}).</p>
            <Link
              to="/"
              className="text-sm text-primary underline-offset-4 hover:underline"
            >
              메뉴로
            </Link>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (phase === "error") {
    return (
      <div className="mx-auto max-w-6xl space-y-4 p-6">
        <h1 className="text-lg font-medium">곡 수정</h1>
        <Card>
          <CardContent className="space-y-3 pt-4">
            <p className="text-sm text-destructive">
              곡을 불러오지 못했습니다.
            </p>
            <Button type="button" variant="outline" onClick={load}>
              다시 시도
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="mx-auto max-w-6xl space-y-6 p-6">
      <div>
        <h1 className="text-lg font-medium">곡 수정</h1>
        <p className="text-xs text-muted-foreground">id: {songId}</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>이름</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <p className="text-xs text-muted-foreground">
            언어마다 이름은 1개까지입니다. 이름은 최소 1개 있어야 합니다 — 필요
            없는 언어는 ×로 빼세요.
          </p>

          <div className="space-y-2">
            {names.map((row) => {
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
                      onClick={() => toggleNameIncluded(row.languageId)}
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
                      updateNameText(row.languageId, event.target.value)
                    }
                    placeholder="이름"
                    className={`${fieldClass} min-w-0 flex-1`}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    size="icon-sm"
                    aria-label={`${language?.name} 이름 빼기`}
                    onClick={() => toggleNameIncluded(row.languageId)}
                  >
                    ×
                  </Button>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>곡 언어</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          <p className="text-xs text-muted-foreground">
            이름과 별개로 고릅니다 — 자동으로 채워지지 않습니다.
          </p>
          <div className="flex flex-wrap gap-3">
            {languages.map((language) => (
              <label
                key={language.id}
                className="flex items-center gap-1.5 text-sm"
              >
                <input
                  type="checkbox"
                  checked={languageIds.has(language.id)}
                  onChange={() => toggleLanguage(language.id)}
                />
                {language.name}
              </label>
            ))}
          </div>
        </CardContent>
      </Card>

      <Card className="overflow-visible">
        <CardHeader>
          <CardTitle>프로듀서</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          <div className="flex gap-2">
            <div className="relative min-w-0 flex-1">
              <input
                value={producerQuery}
                onChange={(event) => setProducerQuery(event.target.value)}
                placeholder="이름으로 검색"
                className={fieldClass}
              />
              {producerResults.length > 0 ? (
                <ul className="absolute inset-x-0 top-full z-10 mt-1 max-h-56 space-y-0.5 overflow-auto rounded-lg border border-border bg-background p-1 shadow-lg">
                  {producerResults.map((producer) => (
                    <li key={producer.id}>
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="w-full justify-start"
                        onClick={() => handleAddProducer(producer)}
                      >
                        {producer.name} 추가
                      </Button>
                    </li>
                  ))}
                </ul>
              ) : null}
            </div>
            <Button
              type="button"
              variant="outline"
              onClick={handleCreateProducer}
            >
              새로 만들기
            </Button>
          </div>
          {producerMessage ? (
            <p className="text-xs text-destructive">{producerMessage}</p>
          ) : null}
          {selectedProducers.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {selectedProducers.map((producer) => (
                <span
                  key={producer.id}
                  className="inline-flex items-center gap-1 rounded-full bg-muted px-2.5 py-1 text-xs"
                >
                  {producer.name}
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon-xs"
                    aria-label={`${producer.name} 제거`}
                    onClick={() => handleRemoveProducer(producer.id)}
                  >
                    ×
                  </Button>
                </span>
              ))}
            </div>
          ) : null}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>정답 패턴</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          <textarea
            ref={answerPatternRef}
            value={answerPattern}
            onChange={(event) => setAnswerPattern(event.target.value)}
            rows={3}
            placeholder="예: (히토|인간|사람)(마니아|매니아)"
            className="w-full resize-none overflow-hidden rounded-lg border border-border bg-background px-2.5 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
          />
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleCheckPattern}
            disabled={!answerPattern.trim() || checkingPattern}
          >
            {checkingPattern ? "검사 중..." : "검사"}
          </Button>
          {patternError ? (
            <p className="text-xs text-destructive">{patternError}</p>
          ) : null}
          {patternCheck ? (
            <div className="space-y-1 text-xs text-muted-foreground">
              <p>
                {patternCheck.count}개
                {patternCheck.warning
                  ? " — 20개를 넘습니다, 패턴을 확인해주세요"
                  : ""}
              </p>
              <p className="break-words">{patternCheck.results.join(", ")}</p>
            </div>
          ) : null}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>상태</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          <select
            value={status}
            onChange={(event) => setStatus(event.target.value as SongStatus)}
            className={`${fieldClass} max-w-xs`}
          >
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <p className="text-xs text-muted-foreground">
            공개로 바꾸려면 이름·정답 패턴·수집된 원곡(ORIGINAL) 영상이 모두
            있어야 합니다(D-062). 빠진 조건은 저장을 시도하면 알려줍니다.
          </p>
        </CardContent>
      </Card>

      {formError ? (
        <div className="space-y-1">
          <p className="text-sm text-destructive">{formError}</p>
          {missingConditions.length > 0 ? (
            <ul className="list-inside list-disc text-sm text-destructive">
              {missingConditions.map((condition) => (
                <li key={condition}>{condition}</li>
              ))}
            </ul>
          ) : null}
        </div>
      ) : null}
      {saveMessage ? (
        <p className="text-sm text-primary">{saveMessage}</p>
      ) : null}

      <Button type="submit" disabled={submitting}>
        {submitting ? "저장 중..." : "저장"}
      </Button>
    </form>
  );
}

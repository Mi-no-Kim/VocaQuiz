import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link } from "react-router";
import { ApiError } from "@/api/client";
import { fetchLanguages } from "@/api/languages";
import { checkAnswerPattern } from "@/api/answerPatterns";
import { createProducer, searchProducers } from "@/api/producers";
import { createSong, type CreateSongNameInput } from "@/api/songs";
import type {
  AdminLanguageResponse,
  AdminProducerResponse,
  CheckAnswerPatternResponse,
} from "@/api/types";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const fieldClass =
  "h-8 w-full rounded-lg border border-border bg-background px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50";

interface NameRow {
  languageId: number;
  name: string;
  included: boolean;
}

function initialNameRows(languages: AdminLanguageResponse[]): NameRow[] {
  return languages.map((language) => ({
    languageId: language.id,
    name: "",
    included: true,
  }));
}

/**
 * 곡 만들기 화면 (P1-3-4). 이름이 1개 이상 있는 것이 최소 입력이다 (D-072).
 * 곡 언어(song_language)는 이름과 완전히 별개로 고른다 — 자동으로 안 채워진다 (B3).
 *
 * <p>이름 입력은 언어마다 정확히 1행이다 — 여러 이름을 붙이는 UI는 아직 없어, 포함된
 * 행은 그대로 대표 이름이 된다.
 *
 * <p>여기서 만드는 곡은 항상 초안(DRAFT)이다 — 공개는 수집된 원곡(ORIGINAL) 영상이
 * 있어야 해(D-062), 새로 만드는 시점엔 항상 조건 미충족이라 이 화면에서는 아예
 * 묻지 않는다. 영상을 붙인 뒤 편집 화면(`/admin/songs/{id}`)에서 공개로 바꾼다.
 */
export function AdminSongNewPage() {
  const [languages, setLanguages] = useState<AdminLanguageResponse[]>([]);
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

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [missingConditions, setMissingConditions] = useState<string[]>([]);
  const [createdId, setCreatedId] = useState<number | null>(null);

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

  useEffect(() => {
    fetchLanguages()
      .then((loaded) => {
        setLanguages(loaded);
        setNames(initialNameRows(loaded));
      })
      .catch(() => setFormError("언어 목록을 불러오지 못했습니다."));
  }, []);

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

    const nameInputs: CreateSongNameInput[] = names
      .filter((row) => row.included && row.name.trim().length > 0)
      .map((row) => ({
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
      const song = await createSong({
        names: nameInputs,
        languageIds: [...languageIds],
        producerIds: selectedProducers.map((p) => p.id),
        answerPattern: answerPattern.trim() ? answerPattern : null,
        status: "DRAFT",
      });
      setCreatedId(song.id);
    } catch (error) {
      if (error instanceof ApiError) {
        setFormError(error.message);
        setMissingConditions(error.missingConditions);
        return;
      }
      setFormError("곡을 만들지 못했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  function handleReset() {
    setNames(initialNameRows(languages));
    setLanguageIds(new Set());
    setSelectedProducers([]);
    setProducerQuery("");
    setProducerResults([]);
    setAnswerPattern("");
    setPatternCheck(null);
    setPatternError(null);
    setFormError(null);
    setMissingConditions([]);
    setCreatedId(null);
  }

  if (createdId != null) {
    return (
      <div className="mx-auto max-w-6xl space-y-4 p-6">
        <h1 className="text-lg font-medium">곡 만들기</h1>
        <Card>
          <CardContent className="space-y-4 pt-4">
            <p className="text-sm">곡을 만들었습니다 (id: {createdId}).</p>
            <div className="flex flex-wrap gap-2">
              <Link
                to={`/admin/songs/${createdId}`}
                className={buttonVariants({ variant: "default" })}
              >
                이동
              </Link>
              <Button type="button" variant="outline" onClick={handleReset}>
                이어서
              </Button>
              <Link to="/" className={buttonVariants({ variant: "outline" })}>
                메뉴로
              </Link>
              <Button
                type="button"
                variant="ghost"
                disabled
                title="곡 목록 화면(P1-3-5)이 만들어지면 연결됩니다"
              >
                나가기
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="mx-auto max-w-6xl space-y-6 p-6">
      <h1 className="text-lg font-medium">곡 만들기</h1>

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

      <Button type="submit" disabled={submitting}>
        {submitting ? "만드는 중..." : "곡 만들기"}
      </Button>
    </form>
  );
}

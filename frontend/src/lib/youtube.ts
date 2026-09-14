/**
 * 유튜브 URL에서 videoId를 뽑는다 (videoId 추출은 프론트, 03-PROMPTS.md P1-3-3).
 * watch?v=, youtu.be/, shorts/ 세 형태만 다룬다 — 그 외는 등록 대상이 아니다.
 */
export function extractYoutubeVideoId(input: string): string | null {
  let url: URL;
  try {
    url = new URL(input.trim());
  } catch {
    return null;
  }

  if (url.hostname === "youtu.be") {
    return url.pathname.slice(1) || null;
  }

  if (url.hostname.endsWith("youtube.com")) {
    if (url.pathname === "/watch") {
      return url.searchParams.get("v");
    }
    const shortsMatch = url.pathname.match(/^\/shorts\/([^/?]+)/);
    if (shortsMatch) {
      return shortsMatch[1];
    }
  }

  return null;
}

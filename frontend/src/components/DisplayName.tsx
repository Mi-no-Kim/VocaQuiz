import type { AdminDisplayNameResponse } from "@/api/types";

/**
 * main/sub 표기법(D-074)으로 정한 이름을 보여준다. `secondary`가 있으면 굵기·색·
 * 크기로 구분해 같이 보여주고, "메인"·"서브"라는 말은 쓰지 않는다.
 */
export function DisplayName({ name }: { name: AdminDisplayNameResponse }) {
  return (
    <span>
      <span className="font-medium">{name.primary ?? "(이름 없음)"}</span>
      {name.secondary ? (
        <span className="ml-1.5 text-xs font-normal text-muted-foreground">
          {name.secondary}
        </span>
      ) : null}
    </span>
  );
}

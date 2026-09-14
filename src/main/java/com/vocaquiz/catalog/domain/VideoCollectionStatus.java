package com.vocaquiz.catalog.domain;

/**
 * 영상 메타데이터 수집 상태 (D-060·D-066).
 *
 * <p>DB는 varchar, 서버는 이 enum으로 다룬다 (D-045와 같은 방식) — 값이 늘어도 DDL이
 * 필요 없다.
 */
public enum VideoCollectionStatus {

    /** URL만 등록된 상태. videos.list를 아직 부르지 않았다. */
    UNCOLLECTED,

    /** videos.list 응답을 받아 메타데이터를 채웠다. */
    COLLECTED,

    /** videos.list 응답에 이 id가 없었다 (삭제·비공개 등). [다시 대기]로 UNCOLLECTED로 되돌릴 수 있다. */
    FAILED,

    /** 사람이 배치 대상에서 뺐다 (D-066). 스케줄·수동 수집 어느 쪽도 다시 건드리지 않는다. */
    EXCLUDED
}

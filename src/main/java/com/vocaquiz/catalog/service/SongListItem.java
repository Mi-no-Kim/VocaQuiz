package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.SongStatus;

import java.time.Instant;
import java.util.List;

/**
 * 관리자 곡 목록의 행 하나 (D-070, D-074, ARCH §5.4). {@link SongSummary}·{@link SongDetail}과
 * 달리 표시 이름과 미작업 사유까지 담는다 — 목록 화면 전용이라 화면이 그대로 쓸 모양으로 만든다.
 */
public record SongListItem(
    Long id,
    SongStatus status,
    ResolvedName displayName,
    List<String> missingConditions,
    Instant createdAt
) {
}

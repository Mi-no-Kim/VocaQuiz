package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.domain.SongStatus;
import com.vocaquiz.catalog.service.SongListItem;

import java.time.Instant;
import java.util.List;

/** 관리자 곡 목록의 행 하나 응답 (P1-3-5). {@code missingConditions}가 비어 있으면 작업 완료다. */
public record AdminSongListItemResponse(
    Long id,
    SongStatus status,
    AdminDisplayNameResponse name,
    List<String> missingConditions,
    Instant createdAt
) {

    public static AdminSongListItemResponse from(SongListItem item) {
        return new AdminSongListItemResponse(
            item.id(),
            item.status(),
            AdminDisplayNameResponse.from(item.displayName()),
            item.missingConditions(),
            item.createdAt());
    }
}

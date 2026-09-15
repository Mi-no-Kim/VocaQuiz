package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.domain.SongStatus;
import com.vocaquiz.catalog.service.SongSummary;

public record AdminSongResponse(
    Long id,
    Long originalLanguageId,
    SongStatus status
) {

    public static AdminSongResponse from(SongSummary summary) {
        return new AdminSongResponse(summary.id(), summary.originalLanguageId(), summary.status());
    }
}

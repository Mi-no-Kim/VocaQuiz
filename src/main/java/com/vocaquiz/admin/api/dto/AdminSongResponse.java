package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.domain.SongStatus;
import com.vocaquiz.catalog.service.SongSummary;

public record AdminSongResponse(
    Long id,
    SongStatus status
) {

    public static AdminSongResponse from(SongSummary summary) {
        return new AdminSongResponse(summary.id(), summary.status());
    }
}

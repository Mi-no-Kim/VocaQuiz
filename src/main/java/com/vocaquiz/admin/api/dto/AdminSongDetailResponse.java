package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.domain.SongStatus;
import com.vocaquiz.catalog.service.SongDetail;

import java.util.List;

/** 곡 조회·수정 응답 — 이름·언어·프로듀서·정답 패턴까지 전부 담는다. */
public record AdminSongDetailResponse(
    Long id,
    SongStatus status,
    List<AdminSongNameResponse> names,
    List<Long> languageIds,
    List<Long> producerIds,
    String answerPattern
) {

    public static AdminSongDetailResponse from(SongDetail detail) {
        return new AdminSongDetailResponse(
            detail.id(),
            detail.status(),
            detail.names().stream().map(AdminSongNameResponse::from).toList(),
            detail.languageIds(),
            detail.producerIds(),
            detail.answerPattern());
    }

    public record AdminSongNameResponse(Long id, Long languageId, String name, boolean primary) {

        static AdminSongNameResponse from(SongDetail.SongNameDetail name) {
            return new AdminSongNameResponse(name.id(), name.languageId(), name.name(), name.primary());
        }
    }
}

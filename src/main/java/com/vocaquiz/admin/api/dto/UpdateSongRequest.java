package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.domain.SongStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 곡 수정 요청 — PUT 전체교체 (B2). {@code names}는 id로 diff하고, {@code languageIds}·
 * {@code producerIds}·{@code answerPattern}은 통째로 교체한다. {@code answerPattern}을
 * 비우면 기존 패턴을 지운다.
 */
public record UpdateSongRequest(
    @NotNull Long originalLanguageId,
    @NotEmpty List<@Valid UpdateSongNameRequest> names,
    List<Long> languageIds,
    List<Long> producerIds,
    String answerPattern,
    @NotNull SongStatus status
) {
}

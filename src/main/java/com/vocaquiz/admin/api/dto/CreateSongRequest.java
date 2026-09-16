package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.domain.SongStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 곡 만들기 요청. 이름 1개 이상이 최소 입력이다 (D-072). {@code languageIds}는 곡 언어
 * (song_language) 다중선택이다.
 */
public record CreateSongRequest(
    @NotEmpty List<@Valid CreateSongNameRequest> names,
    List<Long> languageIds,
    List<Long> producerIds,
    String answerPattern,
    @NotNull SongStatus status
) {
}

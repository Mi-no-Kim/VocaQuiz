package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.domain.SongStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 곡 만들기 요청. {@code languageId}(원제 언어)와 그 언어의 대표 이름 하나가 최소 입력이다
 * (D-061). {@code languageIds}는 곡 언어(song_language) 다중선택이고, 원제 언어는 여기에
 * 자동으로 들어가지 않는다 — 완전히 별개로 고른다.
 */
public record CreateSongRequest(
    @NotNull Long originalLanguageId,
    @NotEmpty List<@Valid CreateSongNameRequest> names,
    List<Long> languageIds,
    List<Long> producerIds,
    String answerPattern,
    @NotNull SongStatus status
) {
}

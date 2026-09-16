package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.SongName;
import com.vocaquiz.catalog.domain.SongStatus;

import java.util.List;

/**
 * 곡 하나의 전체 상세 — 조회·수정 화면이 쓴다 (D-070). {@link SongSummary}와 달리 이름·언어·
 * 프로듀서·정답 패턴까지 담는다. 서비스가 트랜잭션 안에서 여러 테이블을 모아 만들어 돌려준다.
 */
public record SongDetail(
    Long id,
    SongStatus status,
    List<SongNameDetail> names,
    List<Long> languageIds,
    List<Long> producerIds,
    String answerPattern
) {

    /** 패턴이 없으면 {@code answerPattern}은 {@code null}이다. */
    public record SongNameDetail(Long id, Long languageId, String name, boolean primary) {

        static SongNameDetail from(SongName songName) {
            return new SongNameDetail(
                songName.getId(),
                songName.getLanguage().getId(),
                songName.getName(),
                songName.isPrimary());
        }
    }
}

package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.Song;
import com.vocaquiz.catalog.domain.SongStatus;

/**
 * 카탈로그가 곡 하나에 대해 아는 것 (D-070). 서비스가 트랜잭션 안에서 만들어 돌려준다.
 *
 * <p>song 테이블 자신의 컬럼만 담는다. 이름·언어·크레딧은 다른 테이블에 걸쳐 있어
 * 조회 화면이 필요해지면 그때 따로 채운다 — VideoSummary와 같은 이유로,
 * 화면 모양에 맞춰 이 record를 미리 불리지 않는다.
 */
public record SongSummary(
    Long id,
    SongStatus status
) {

    /** 엔티티를 읽는 유일한 지점이라 패키지 밖으로 열지 않는다 (VideoSummary와 같은 이유). */
    static SongSummary from(Song song) {
        return new SongSummary(
            song.getId(),
            song.getStatus());
    }
}

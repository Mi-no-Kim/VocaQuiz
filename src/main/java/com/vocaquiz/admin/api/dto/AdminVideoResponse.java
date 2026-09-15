package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.domain.VideoCollectionStatus;
import com.vocaquiz.catalog.domain.VideoKind;
import com.vocaquiz.catalog.service.VideoSummary;

import java.time.Instant;

/**
 * 관리자 영상 목록 · 등록 응답.
 *
 * <p>이름에 {@code Admin}이 붙는 이유는 D-070이다 — 도메인형 패키지라 {@code catalog} 쪽에도
 * 같은 엔티티 이름을 쓰는 응답이 생길 수 있고, 그때 한 파일에서 둘을 쓰면 FQN을 써야 한다.
 */
public record AdminVideoResponse(
    Long id,
    String youtubeVideoId,
    VideoCollectionStatus collectionStatus,
    VideoKind kind,
    boolean playable,
    Long songId,
    String titleSnapshot,
    Integer durationSec,
    Long viewCount,
    Instant publishedAt,
    String channelName,
    String excludeReason
) {
    public static AdminVideoResponse from(VideoSummary video) {
        return new AdminVideoResponse(
            video.id(),
            video.youtubeVideoId(),
            video.collectionStatus(),
            video.kind(),
            video.playable(),
            video.songId(),
            video.titleSnapshot(),
            video.durationSec(),
            video.viewCount(),
            video.publishedAt(),
            video.channelName(),
            video.excludeReason());
    }
}

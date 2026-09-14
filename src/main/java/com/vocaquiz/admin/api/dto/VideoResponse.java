package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.domain.Video;
import com.vocaquiz.catalog.domain.VideoCollectionStatus;
import com.vocaquiz.catalog.domain.VideoKind;

import java.time.Instant;

public record VideoResponse(
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
    public static VideoResponse from(Video video) {
        return new VideoResponse(
            video.getId(),
            video.getYoutubeVideoId(),
            video.getCollectionStatus(),
            video.getKind(),
            video.isPlayable(),
            video.getSong() != null ? video.getSong().getId() : null,
            video.getTitleSnapshot(),
            video.getDurationSec(),
            video.getViewCount(),
            video.getPublishedAt(),
            video.getChannel() != null ? video.getChannel().getName() : null,
            video.getExcludeReason()
        );
    }
}

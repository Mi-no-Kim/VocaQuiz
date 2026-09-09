package com.vocaquiz.youtube.dto;

import java.time.Instant;

public record VideoInfo(
    String youtubeVideoId,
    String title,
    String description,
    int durationSec,
    Long viewCount,
    Instant publishedAt,
    String channelId
) {
}

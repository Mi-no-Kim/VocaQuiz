package com.vocaquiz.youtube.dto;

import java.time.Instant;
import java.util.List;

public record VideoListResponse(
    List<Item> items
) {
    public record Item(String id, Snippet snippet, ContentDetails contentDetails, Statistics statistics) {}
    public record Snippet(Instant publishedAt, String channelId, String title, String description) {}
    public record ContentDetails(String duration) {}
    public record Statistics(String viewCount) {}
}

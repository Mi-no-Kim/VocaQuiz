package com.vocaquiz.youtube;

import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;
import com.vocaquiz.youtube.config.YoutubeProperties;
import com.vocaquiz.youtube.dto.VideoInfo;
import com.vocaquiz.youtube.dto.VideoListResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class YoutubeDataClient {
    private static final int MAX_IDS_PER_CALL = 50;

    private final RestClient client;
    private final String apiKey;

    public YoutubeDataClient(RestClient.Builder builder, YoutubeProperties props) {
        this.client = builder.baseUrl(props.baseUrl()).build();
        this.apiKey = props.apiKey();
    }

    public List<VideoInfo> fetchVideos(List<String> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) return List.of();

        List<VideoInfo> result = new ArrayList<>();
        for (int i = 0; i < videoIds.size(); i += MAX_IDS_PER_CALL) {
            int end = Math.min(i + MAX_IDS_PER_CALL, videoIds.size());
            result.addAll(fetchChunk(videoIds.subList(i, end)));
        }
        return result;
    }

    private List<VideoInfo> fetchChunk(List<String> chunk) {
        VideoListResponse res = client.get()
            .uri(uriBuilder -> uriBuilder
                .path("/videos")
                .queryParam("part", "snippet,contentDetails,statistics")
                .queryParam("id", String.join(",", chunk))
                .queryParam("key", apiKey)
                .build())
            .retrieve()
            .onStatus(
                HttpStatusCode::isError, (request, response) -> {
                    throw new ApiException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "videos.list 실패: " + response.getStatusCode()
                    );
                }
            )
            .body(VideoListResponse.class);

        if (res == null || res.items() == null) return List.of();

        return res.items().stream()
            .map(YoutubeDataClient::toVideoInfo)
            .toList();
    }

    private static VideoInfo toVideoInfo(VideoListResponse.Item item) {
        VideoListResponse.Snippet snippet = item.snippet();
        VideoListResponse.Statistics stats = item.statistics();

        int durationSec = Math.toIntExact(
            Duration.parse(item.contentDetails().duration()).toSeconds());

        Long viewCount = (stats == null || stats.viewCount() == null)
            ? null
            : Long.parseLong(stats.viewCount());

        return new VideoInfo(
            item.id(),
            snippet.title(),
            snippet.description(),
            durationSec,
            viewCount,
            snippet.publishedAt(),
            snippet.channelId()
        );
    }
}

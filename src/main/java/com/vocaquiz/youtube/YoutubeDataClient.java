package com.vocaquiz.youtube;

import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;
import com.vocaquiz.youtube.config.YoutubeProperties;
import com.vocaquiz.youtube.dto.VideoInfo;
import com.vocaquiz.youtube.dto.VideoListResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class YoutubeDataClient {

    /**
     * videos.list 한 번에 넘길 수 있는 id 개수 (D-034 — id 50개당 1 unit).
     *
     * <p>공개해 두는 이유는 호출자가 이 값으로 <b>자기 쪽 묶음</b>을 나누기 때문이다
     * (D-069 — 수집은 이 크기로 조회하고 곧바로 저장한다). 숫자를 두 곳에 적지 않으려고
     * 여기 하나만 둔다.
     */
    public static final int MAX_IDS_PER_CALL = 50;

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

    /**
     * 외부 호출 실패는 전부 {@code ApiException(EXTERNAL_API_ERROR)}로 통일한다.
     *
     * <p>{@code onStatus}가 잡는 건 HTTP 에러 응답뿐이고, 연결 실패·타임아웃은
     * {@link RestClientException}으로 따로 올라온다. 둘을 한 종류로 묶어야
     * 호출자가 "외부 호출이 실패했다"를 조건 하나로 다룰 수 있다 (D-069의 묶음 단위
     * 실패 처리가 이걸 전제로 한다).
     */
    private List<VideoInfo> fetchChunk(List<String> chunk) {
        VideoListResponse res;
        try {
            res = client.get()
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
        } catch (RestClientException e) {
            throw new ApiException(ErrorCode.EXTERNAL_API_ERROR, "videos.list 호출 실패: " + e.getMessage());
        }

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
            snippet.channelId(),
            snippet.channelTitle()
        );
    }
}

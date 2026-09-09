package com.vocaquiz.youtube;

import com.vocaquiz.youtube.config.YoutubeProperties;
import com.vocaquiz.youtube.dto.VideoInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "YOUTUBE_API_KEY", matches = ".+")
public class YoutubeDataClientLiveTest {

    private static final String VIDEO_ID = "19y8YTbvri8";   // メズマライザー

    @Test
    @DisplayName("실제 videos.list를 부르면 제목·길이·조회수가 온다")
    void callsRealApi() {
        String apiKey = System.getenv("YOUTUBE_API_KEY");
        assertThat(apiKey).as("환경변수 YOUTUBE_API_KEY").isNotBlank();

        YoutubeDataClient client = new YoutubeDataClient(
            RestClient.builder(),
            new YoutubeProperties(apiKey, "https://www.googleapis.com/youtube/v3")
        );

        List<VideoInfo> result = client.fetchVideos(List.of(VIDEO_ID));

        assertThat(result).hasSize(1);
        VideoInfo v = result.get(0);

        System.out.println("title       : " + v.title());
        System.out.println("durationSec : " + v.durationSec());
        System.out.println("viewCount   : " + v.viewCount());
        System.out.println("publishedAt : " + v.publishedAt());
        System.out.println("channelId   : " + v.channelId());

        assertThat(v.youtubeVideoId()).isEqualTo(VIDEO_ID);
        assertThat(v.title()).isNotBlank();
        assertThat(v.durationSec()).isPositive();
        assertThat(v.viewCount()).isPositive();
    }
}

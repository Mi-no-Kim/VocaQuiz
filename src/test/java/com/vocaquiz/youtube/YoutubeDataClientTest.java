package com.vocaquiz.youtube;

import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.youtube.config.YoutubeProperties;
import com.vocaquiz.youtube.dto.VideoInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class YoutubeDataClientTest {

    private MockRestServiceServer server;
    private YoutubeDataClient client;
    private static final String EXPECTED_ID = "19y8YTbvri8,AqI97zHMoQw,dHXC_ahjtEE,cQKGUgOfD8U,Soy4jGPHr3g,kbNdx0yqbZE,eSW2LVbPThw,NTrm_idbhUk,D6DVTLvOupE,2b1IexhKPz4,I1mOeAtPkgk,NIABFcVB3Is,ktWFYyVaDEk";
    private static final List<String> idList = List.of(
        "19y8YTbvri8",
        "AqI97zHMoQw",
        "dHXC_ahjtEE",
        "cQKGUgOfD8U",
        "Soy4jGPHr3g",
        "kbNdx0yqbZE",
        "eSW2LVbPThw",
        "NTrm_idbhUk",
        "D6DVTLvOupE",
        "2b1IexhKPz4",
        "I1mOeAtPkgk",
        "NIABFcVB3Is",
        "ktWFYyVaDEk"
    );

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new YoutubeDataClient(
            builder,
            new YoutubeProperties("test-key", "https://youtube.test/v3")
        );
    }

    @Test
    @DisplayName("응답을 VideoInfo로 바꾼다")
    void parsesResponse() throws IOException {
        server.expect(requestTo(containsString("/videos")))
            .andExpect(queryParam("id", EXPECTED_ID))
            .andRespond(withSuccess(json("youtube_response.json"), MediaType.APPLICATION_JSON));

        List<VideoInfo> result = client.fetchVideos(idList);

        assertThat(result).hasSize(idList.size());
        VideoInfo v = result.get(0);
        assertThat(v.youtubeVideoId()).isEqualTo("19y8YTbvri8");
        assertThat(v.title()).isEqualTo("メズマライザー / 初音ミク・重音テトSV - Mesmerizer");
        assertThat(v.durationSec()).isEqualTo(157);
        assertThat(v.viewCount()).isEqualTo(220486285L);
        assertThat(v.channelId()).isEqualTo("UCtmi2O7lp0C_i53hFIExAaA");
        assertThat(v.description()).contains("■Vocal / 初音ミク・重音テトSV");

        server.verify();
    }

    @Test
    @DisplayName("id가 50개를 넘으면 호출을 나눈다")
    void splitsIntoChunksOf50() {
        List<String> ids = IntStream.rangeClosed(1, 51)
            .mapToObj(i -> "id" + i)
            .toList();

        server.expect(ExpectedCount.times(2), requestTo(containsString("/videos")))
            .andRespond(withSuccess(
                """
                    {"items":[]}
                    """, MediaType.APPLICATION_JSON
            ));

        client.fetchVideos(ids);

        server.verify();
    }

    @Test
    @DisplayName("statistics가 없으면 viewCount는 null이다")
    void viewCountIsNullWhenStatisticsMissing() throws IOException {
        server.expect(requestTo(containsString("/videos")))
            .andRespond(withSuccess(json("youtube_response_except_statistics.json"), MediaType.APPLICATION_JSON));

        List<VideoInfo> result = client.fetchVideos(idList);

        assertThat(result).hasSize(idList.size());
        assertThat(result.get(0).viewCount()).isNull();
    }

    @Test
    @DisplayName("유튜브가 에러를 주면 ApiException이 난다")
    void throwsApiExceptionOnError() {
        server.expect(requestTo(containsString("/videos")))
            .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.fetchVideos(idList))
            .isInstanceOf(ApiException.class);
    }

    private String json(String fileName) throws IOException {
        return new ClassPathResource("youtube/" + fileName)
            .getContentAsString(StandardCharsets.UTF_8);
    }
}

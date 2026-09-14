package com.vocaquiz.catalog;

import com.vocaquiz.catalog.domain.Video;
import com.vocaquiz.catalog.domain.VideoCollectionStatus;
import com.vocaquiz.catalog.domain.VideoKind;
import com.vocaquiz.catalog.repository.ChannelRepository;
import com.vocaquiz.catalog.repository.VideoRepository;
import com.vocaquiz.catalog.service.VideoIngestService;
import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;
import com.vocaquiz.youtube.YoutubeDataClient;
import com.vocaquiz.youtube.dto.VideoInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * P1-3-3 완료 기준 2·3·4, 결정 3(동시 수집 처리)을 검증한다.
 *
 * <p>{@code YoutubeDataClient}는 목이다 — 여기서 검증하는 건 videos.list 응답의
 * 파싱이 아니라 {@link VideoIngestService}의 상태 전이·동시성 로직이다. 파싱 자체는
 * {@code YoutubeDataClientTest}가 실제 응답 픽스처로 이미 검증한다 (CLAUDE.md §3-6).
 *
 * <p>클래스 레벨 {@code @Transactional}을 쓰지 않는다 — 동시성 테스트가 별도 스레드에서
 * DB를 건드리는데, 테스트 트랜잭션 롤백 방식과 스레드가 섞이면 그 스레드가 메인
 * 스레드의 미커밋 데이터를 못 본다. 대신 {@code @AfterEach}로 직접 치운다.
 */
@SpringBootTest
@ActiveProfiles("test")
class VideoIngestServiceTest {

    @Autowired VideoIngestService videoIngestService;
    @Autowired VideoRepository videoRepository;
    @Autowired ChannelRepository channelRepository;

    @MockitoBean
    YoutubeDataClient youtubeDataClient;

    @AfterEach
    void cleanUp() {
        videoRepository.deleteAll();
        channelRepository.deleteAll();
    }

    @Test
    @DisplayName("완료 기준 4 — 응답에 없는 id는 FAILED가 된다")
    void marksMissingIdsAsFailed() {
        Video ok = videoRepository.save(Video.createUncollected("ok_id"));
        Video missing = videoRepository.save(Video.createUncollected("missing_id"));

        when(youtubeDataClient.fetchVideos(anyList())).thenReturn(List.of(
            new VideoInfo("ok_id", "제목", "설명", 180, 1000L, Instant.now(), "UCxxxx", "채널명")
        ));

        videoIngestService.collectPendingManually();

        // channel은 지연 로딩이라 트랜잭션 밖(테스트 메서드)에서 그냥 findById로 읽으면
        // LazyInitializationException이 난다 — channel을 같이 가져오는 조회로 읽는다.
        Video okAfter = videoRepository.findWithChannelById(ok.getId()).orElseThrow();
        assertThat(okAfter.getCollectionStatus()).isEqualTo(VideoCollectionStatus.COLLECTED);
        assertThat(okAfter.getChannel().getName()).isEqualTo("채널명");
        assertThat(okAfter.getDurationSec()).isEqualTo(180);

        assertThat(videoRepository.findById(missing.getId()).orElseThrow().getCollectionStatus())
            .isEqualTo(VideoCollectionStatus.FAILED);
    }

    @Test
    @DisplayName("완료 기준 2 — 이미 등록된 videoId는 409")
    void rejectsDuplicateVideoId() {
        videoIngestService.addVideo("dup_id");

        assertThatThrownBy(() -> videoIngestService.addVideo("dup_id"))
            .isInstanceOf(ApiException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("완료 기준 3 — 실패한 영상은 [다시 대기]로 UNCOLLECTED가 된다")
    void requeueBringsBackFailedVideo() {
        Video failed = videoRepository.save(Video.createUncollected("failed_id"));
        failed.markFailed();
        videoRepository.save(failed);

        videoIngestService.requeue(failed.getId());

        assertThat(videoRepository.findById(failed.getId()).orElseThrow().getCollectionStatus())
            .isEqualTo(VideoCollectionStatus.UNCOLLECTED);
    }

    @Test
    @DisplayName("결정 1 — 제외된 영상은 EXCLUDED가 되고 사유가 저장된다")
    void excludeMarksVideoExcluded() {
        Video video = videoRepository.save(Video.createUncollected("exclude_id"));

        videoIngestService.exclude(video.getId(), "노래 아님");

        Video after = videoRepository.findById(video.getId()).orElseThrow();
        assertThat(after.getCollectionStatus()).isEqualTo(VideoCollectionStatus.EXCLUDED);
        assertThat(after.getExcludeReason()).isEqualTo("노래 아님");
    }

    @Test
    @DisplayName("피드백 — 제외된 적 없는(한 번도 수집 안 된) 영상을 복구하면 UNCOLLECTED가 된다")
    void restoreNeverCollectedVideoGoesToUncollected() {
        Video video = videoRepository.save(Video.createUncollected("restore_uncollected_id"));
        videoIngestService.exclude(video.getId(), "일단 제외");

        videoIngestService.restore(video.getId());

        Video after = videoRepository.findById(video.getId()).orElseThrow();
        assertThat(after.getCollectionStatus()).isEqualTo(VideoCollectionStatus.UNCOLLECTED);
        assertThat(after.getExcludeReason()).isNull();
    }

    @Test
    @DisplayName("피드백 — 예전에 수집된 적 있는 영상을 복구하면 다시 조회하지 않고 COLLECTED가 된다")
    void restorePreviouslyCollectedVideoGoesToCollected() {
        Video video = videoRepository.save(Video.createUncollected("restore_collected_id"));
        video.markCollected(null, 180, Instant.now(), "제목", 1000L);
        videoRepository.save(video);
        videoIngestService.exclude(video.getId(), "확인 필요");

        videoIngestService.restore(video.getId());

        Video after = videoRepository.findById(video.getId()).orElseThrow();
        assertThat(after.getCollectionStatus()).isEqualTo(VideoCollectionStatus.COLLECTED);
        assertThat(after.getExcludeReason()).isNull();
    }

    @Test
    @DisplayName("피드백 — 등록 직후 kind는 null이 아니라 UNDEFINED다")
    void newVideoStartsWithUndefinedKind() {
        Video video = videoIngestService.addVideo("undefined_kind_id");

        assertThat(video.getKind()).isEqualTo(VideoKind.UNDEFINED);
    }

    @Test
    @DisplayName("피드백 — playable을 껐다 켤 수 있다")
    void togglesPlayable() {
        Video video = videoRepository.save(Video.createUncollected("toggle_id"));
        assertThat(video.isPlayable()).isTrue();

        videoIngestService.disablePlayable(video.getId());
        assertThat(videoRepository.findById(video.getId()).orElseThrow().isPlayable()).isFalse();

        videoIngestService.enablePlayable(video.getId());
        assertThat(videoRepository.findById(video.getId()).orElseThrow().isPlayable()).isTrue();
    }

    @Test
    @DisplayName("피드백 — 대기 중인 영상이 50개를 넘어도 전부 수집 대상이다")
    void collectsMoreThanFiftyPendingAtOnce() {
        int total = 55;
        IntStream.range(0, total)
            .mapToObj(i -> "bulk_id_" + i)
            .forEach(id -> videoRepository.save(Video.createUncollected(id)));

        when(youtubeDataClient.fetchVideos(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) invocation.getArgument(0);
            // 넘어온 id 개수 자체가 이 테스트의 핵심 — 서비스가 50개로 자르지 않고
            // 전부 한 번에 YoutubeDataClient에 넘기는지 확인한다.
            assertThat(ids).hasSize(total);
            return ids.stream()
                .map(id -> new VideoInfo(id, "제목", "설명", 100, 10L, Instant.now(), "UCxxxx", "채널명"))
                .toList();
        });

        videoIngestService.collectPendingManually();

        assertThat(videoRepository.findByCollectionStatusOrderByIdAsc(VideoCollectionStatus.COLLECTED))
            .hasSize(total);
    }

    @Test
    @DisplayName("결정 3 — 수집이 도는 중에 수동으로 다시 요청하면 409")
    void rejectsConcurrentManualCollection() throws Exception {
        videoRepository.save(Video.createUncollected("slow_id"));

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        when(youtubeDataClient.fetchVideos(anyList())).thenAnswer(invocation -> {
            started.countDown();
            release.await(2, TimeUnit.SECONDS);
            return List.of();
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> firstRun = executor.submit(videoIngestService::collectPendingManually);

            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> videoIngestService.collectPendingManually())
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COLLECTION_IN_PROGRESS);

            release.countDown();
            firstRun.get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }
    }
}

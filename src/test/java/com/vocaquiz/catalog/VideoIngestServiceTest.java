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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * P1-3-3 완료 기준 2·3·4와 D-069(묶음 수집·부분 실패·동시 실행)를 검증한다.
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
    @DisplayName("D-066 — 제외된 영상은 EXCLUDED가 되고 사유가 저장된다")
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
    @DisplayName("D-067 — 등록 직후 kind는 null이 아니라 UNDEFINED다")
    void newVideoStartsWithUndefinedKind() {
        Video video = videoIngestService.addVideo("undefined_kind_id");

        assertThat(video.getKind()).isEqualTo(VideoKind.UNDEFINED);
    }

    @Test
    @DisplayName("D-069 — 대기 중인 영상이 50개를 넘으면 50개 묶음으로 나눠 부르고 전부 수집한다")
    void collectsInChunksOfFifty() {
        int total = 55;
        savePending(total);

        List<Integer> chunkSizes = new ArrayList<>();
        when(youtubeDataClient.fetchVideos(anyList())).thenAnswer(invocation -> {
            List<String> ids = invocation.getArgument(0);
            chunkSizes.add(ids.size());
            return collectedInfoOf(ids);
        });

        videoIngestService.collectPendingManually();

        assertThat(chunkSizes).containsExactly(YoutubeDataClient.MAX_IDS_PER_CALL, 5);
        assertThat(videoRepository.findByCollectionStatusOrderByIdAsc(VideoCollectionStatus.COLLECTED))
            .hasSize(total);
    }

    /**
     * 되돌아온 버그 (D-069). {@code collectPending()}이 대기 중인 것을 전부 모아 한 번에
     * 넘기고 그 결과를 나중에 저장하기 때문에, 호출 하나가 실패하면 <b>저장이 시작되기도
     * 전에</b> 예외가 올라가 앞서 받아 둔 응답까지 통째로 버려진다.
     *
     * <p>목은 실제 클라이언트를 흉내 낸다 — 한 번에
     * {@link YoutubeDataClient#MAX_IDS_PER_CALL}개까지만 받고, 두 번째 호출은 실패한다.
     * 고치기 전에는 이 테스트가 단언에 닿지도 못하고 {@code ApiException}을 그대로 받는다.
     * 그게 "저장을 시작조차 안 했다"는 증거다.
     */
    @Test
    @DisplayName("D-069 — 뒷 묶음 호출이 실패해도 앞 묶음은 저장된다")
    void keepsEarlierChunksWhenLaterCallFails() {
        savePending(55);

        AtomicInteger calls = new AtomicInteger();
        when(youtubeDataClient.fetchVideos(anyList())).thenAnswer(invocation -> {
            List<String> ids = invocation.getArgument(0);
            if (ids.size() > YoutubeDataClient.MAX_IDS_PER_CALL || calls.incrementAndGet() == 2) {
                throw new ApiException(ErrorCode.EXTERNAL_API_ERROR, "쿼터 초과");
            }
            return collectedInfoOf(ids);
        });

        videoIngestService.collectPendingManually();

        assertThat(videoRepository.findByCollectionStatusOrderByIdAsc(VideoCollectionStatus.COLLECTED))
            .hasSize(YoutubeDataClient.MAX_IDS_PER_CALL);
        // 실패한 묶음은 FAILED가 아니라 UNCOLLECTED로 남아야 다음 실행이 다시 시도한다.
        // FAILED는 "응답에 그 id가 없었다"는 뜻이라 호출 실패와 섞으면 안 된다 (D-060·D-066).
        assertThat(videoRepository.findByCollectionStatusOrderByIdAsc(VideoCollectionStatus.UNCOLLECTED))
            .hasSize(5);
    }

    private void savePending(int count) {
        IntStream.range(0, count)
            .mapToObj(i -> "bulk_id_" + i)
            .forEach(id -> videoRepository.save(Video.createUncollected(id)));
    }

    private List<VideoInfo> collectedInfoOf(List<String> ids) {
        return ids.stream()
            .map(id -> new VideoInfo(id, "제목", "설명", 100, 10L, Instant.now(), "UCxxxx", "채널명"))
            .toList();
    }

    @Test
    @DisplayName("D-069 — 수집이 도는 중에 수동으로 다시 요청하면 409")
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

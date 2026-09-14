package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.Channel;
import com.vocaquiz.catalog.domain.Video;
import com.vocaquiz.catalog.domain.VideoCollectionStatus;
import com.vocaquiz.catalog.domain.VideoKind;
import com.vocaquiz.catalog.repository.ChannelRepository;
import com.vocaquiz.catalog.repository.VideoCreditRepository;
import com.vocaquiz.catalog.repository.VideoRepository;
import com.vocaquiz.catalog.repository.VideoVocalRepository;
import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;
import com.vocaquiz.youtube.YoutubeDataClient;
import com.vocaquiz.youtube.dto.VideoInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 영상 URL 등록과 메타데이터 일괄 수집 (D-059·D-060·D-066·D-069).
 *
 * <p>등록은 URL(videoId)만 받아 {@link VideoCollectionStatus#UNCOLLECTED}로 쌓는다.
 * 실제 메타데이터는 {@link #collectPendingManually()}([지금 수집] 버튼)와
 * {@link #collectPendingScheduled()}(매일 오전 9시 KST, D-069)가 채운다.
 *
 * <p><b>동시 실행 (D-069):</b> 둘은 같은 락({@link #collecting})을 쓴다.
 * 스케줄이 락을 못 잡으면(수동이 도는 중) 보여줄 응답 대상이 없으므로 조용히
 * 이번 틱을 건너뛴다. 수동이 락을 못 잡으면(스케줄이 도는 중) 컨트롤러가 409를
 * 돌려준다 — 어느 쪽이 "두 번째로 왔느냐"만 다르게 반응하는 대칭적인 락이다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoIngestService {

    private final VideoRepository videoRepository;
    private final ChannelRepository channelRepository;
    private final VideoVocalRepository videoVocalRepository;
    private final VideoCreditRepository videoCreditRepository;
    private final YoutubeDataClient youtubeDataClient;
    private final SongCatalogService songCatalogService;

    private final AtomicBoolean collecting = new AtomicBoolean(false);

    /** URL(videoId)만으로 미수집 영상을 만든다. 이미 있으면 409. */
    @Transactional
    public Video addVideo(String youtubeVideoId) {
        videoRepository.findByYoutubeVideoId(youtubeVideoId).ifPresent(existing -> {
            throw new ApiException(ErrorCode.CONFLICT, "이미 등록된 영상: " + youtubeVideoId);
        });
        return videoRepository.save(Video.createUncollected(youtubeVideoId));
    }

    /** [지금 수집] 버튼. 이미 도는 중이면 409(COLLECTION_IN_PROGRESS). */
    public void collectPendingManually() {
        if (!collecting.compareAndSet(false, true)) {
            throw new ApiException(ErrorCode.COLLECTION_IN_PROGRESS, "이미 수집이 진행 중입니다");
        }
        try {
            collectPending();
        } finally {
            collecting.set(false);
        }
    }

    /** 매일 오전 9시(KST) 1회 (D-069). 이미 도는 중이면 조용히 건너뛰고 다음 날을 기다린다. */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void collectPendingScheduled() {
        if (!collecting.compareAndSet(false, true)) {
            return;
        }
        try {
            collectPending();
        } finally {
            collecting.set(false);
        }
    }

    /** FAILED → UNCOLLECTED. [다시 대기] 버튼. */
    @Transactional
    public void requeue(Long videoId) {
        loadVideo(videoId).requeue();
    }

    /** 어떤 상태에서든 → EXCLUDED (D-066). 배치·수동 수집이 이후 다시 건드리지 않는다. */
    @Transactional
    public void exclude(Long videoId, String reason) {
        loadVideo(videoId).exclude(reason);
    }

    /** EXCLUDED를 취소한다. 상세 판단 기준은 {@link Video#restore()} 참고. */
    @Transactional
    public void restore(Long videoId) {
        loadVideo(videoId).restore();
    }

    /** 출제 목록에서 뺀다 (D-010). */
    @Transactional
    public void disablePlayable(Long videoId) {
        loadVideo(videoId).disablePlayable();
    }

    /** 다시 출제 대상으로 켠다. */
    @Transactional
    public void enablePlayable(Long videoId) {
        loadVideo(videoId).enablePlayable();
    }

    /**
     * 영상을 지운다. 곡에 붙어 있어도 지운다 (D-068).
     *
     * <p>video_credit·video_vocal을 먼저 지우고, 지우려는 영상이 ORIGINAL이면서
     * 곡에 붙어 있었다면 지운 뒤에 그 곡의 song_vocal을 다시 계산한다 — D-050이 정한
     * 갱신 경로({@link SongCatalogService})를 그대로 탄다.
     *
     * <p>segment(P1-4)는 아직 코드에 없어 지금은 정리할 게 없다. P1-4에서
     * segment.video_id가 생기면 영상 삭제 시 그 영상의 구간도 같이 지워야 한다.
     */
    @Transactional
    public void delete(Long videoId) {
        Video video = loadVideo(videoId);

        videoCreditRepository.deleteByVideoId(videoId);
        videoVocalRepository.deleteByVideoId(videoId);

        boolean wasOriginalWithSong = video.getKind() == VideoKind.ORIGINAL && video.getSong() != null;
        Long songId = wasOriginalWithSong ? video.getSong().getId() : null;

        videoRepository.delete(video);

        if (wasOriginalWithSong) {
            songCatalogService.recalculateSongVocal(songId);
        }
    }

    private Video loadVideo(Long videoId) {
        return videoRepository.findById(videoId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "video " + videoId));
    }

    /**
     * 대기 중인 미수집 영상을 videos.list로 조회해 채운다 (D-069).
     *
     * <p><b>묶음 단위로 끊어서 조회하고 곧바로 저장한다.</b> 예전에는 대기 중인 것을
     * 전부 모아 한 번에 넘기고 그 결과를 나중에 저장했는데, 그러면 마지막 호출 하나가
     * 실패했을 때 <b>앞서 성공한 묶음까지 통째로 버려졌다</b> — 저장이 시작되기도 전에
     * 예외가 올라갔기 때문이다. 묶음마다 저장을 끝내 두면 120개 중 뒷 묶음이 실패해도
     * 앞 묶음은 남는다.
     *
     * <p>묶음 크기는 {@link YoutubeDataClient#MAX_IDS_PER_CALL}을 그대로 쓴다. 이 숫자는
     * videos.list의 호출당 id 제한이자 이제 저장·재시도 단위이기도 하다.
     *
     * <p>외부 호출을 트랜잭션 밖에서 먼저 한다 (TIL 2026-09-11과 같은 원리) — 실패해도
     * DB 상태를 아무것도 바꾸지 않는다.
     */
    private void collectPending() {
        List<Video> pending = videoRepository.findByCollectionStatusOrderByIdAsc(VideoCollectionStatus.UNCOLLECTED);

        for (int from = 0; from < pending.size(); from += YoutubeDataClient.MAX_IDS_PER_CALL) {
            int to = Math.min(from + YoutubeDataClient.MAX_IDS_PER_CALL, pending.size());
            collectChunk(pending.subList(from, to));
        }
    }

    /**
     * 묶음 하나를 조회해 반영한다.
     *
     * <p>호출이 실패하면 이 묶음만 포기하고 다음 묶음으로 넘어간다 — 이 묶음의 영상들은
     * UNCOLLECTED로 남아 다음 실행이 다시 시도한다. 여기서 예외를 삼키는 건 "일어날 리
     * 없는 케이스"를 감싸는 게 아니라, <b>부분 실패를 부분 실패로 끝내기 위한</b> 것이다
     * (D-069). 대신 조용히 사라지지 않도록 반드시 남긴다.
     *
     * <p>응답에 없는 id(삭제·비공개 등)는 호출 실패와 다르다 — 그건 FAILED로 확정한다
     * (D-060·D-066). 둘을 섞으면 멀쩡한 영상이 FAILED로 찍힌다.
     */
    private void collectChunk(List<Video> chunk) {
        List<VideoInfo> fetched;
        try {
            fetched = youtubeDataClient.fetchVideos(chunk.stream().map(Video::getYoutubeVideoId).toList());
        } catch (ApiException e) {
            log.warn("videos.list 묶음 실패 — {}건을 UNCOLLECTED로 남긴다: {}", chunk.size(), e.getMessage());
            return;
        }

        Map<String, VideoInfo> byYoutubeId = fetched.stream()
            .collect(Collectors.toMap(VideoInfo::youtubeVideoId, Function.identity()));

        for (Video video : chunk) {
            applyResult(video, byYoutubeId.get(video.getYoutubeVideoId()));
        }
    }

    /**
     * {@code video}는 {@link #collectPending()}이 이미 끝난 조회에서 받아 온 것이라
     * 이 시점엔 detached 상태다. {@code save}가 병합(merge)한다.
     *
     * <p>일부러 이 메서드에 {@code @Transactional}을 달지 않는다. 같은 클래스 안에서
     * {@code this.applyResult(...)}로 부르면 self-invocation이라 프록시를 안 거쳐
     * 트랜잭션이 아예 안 걸린다 — 대신 각 저장을 {@code save()} 한 번의 독립된
     * 트랜잭션으로 둔다. 한 영상이 실패해도 앞서 저장된 것들은 그대로 남는 편이
     * 배치 성격에 맞다.
     */
    private void applyResult(Video video, VideoInfo info) {
        if (info == null) {
            video.markFailed();
        } else {
            Channel channel = channelRepository.findByYoutubeChannelId(info.channelId())
                .orElseGet(() -> channelRepository.save(Channel.create(info.channelId(), info.channelTitle())));
            video.markCollected(channel, info.durationSec(), info.publishedAt(), info.title(), info.viewCount());
        }
        videoRepository.save(video);
    }
}

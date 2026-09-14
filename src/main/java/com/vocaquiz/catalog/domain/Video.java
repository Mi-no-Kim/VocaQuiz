package com.vocaquiz.catalog.domain;

import com.vocaquiz.common.domain.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "video",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_video_youtube_video_id",
        columnNames = "youtube_video_id"),
    indexes = @Index(
        name = "idx_video_song_id",
        columnList = "song_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 곡 없이도 존재한다 (D-059). 편집(P1-3-6)에서 연결한다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    private Channel channel;

    @Column(length = 32, nullable = false)
    private String youtubeVideoId;

    /**
     * 출제 대상은 ORIGINAL로 제한한다 (D-055). song_vocal 재계산도 이 값을 본다 (D-050).
     * 등록 시점엔 모른다 — 영상 편집(P1-3-6)에서 사람이 고르기 전까지 {@link VideoKind#UNDEFINED}다
     * (D-059, P1-3-3 결정 5 — null 대신 값을 두는 게 낫다는 피드백을 반영해 nullable 컬럼 대신
     * sentinel enum 값으로 바꿨다. 컬럼 자체는 그대로 NOT NULL이라 기존 값(전부 ORIGINAL)에도
     * 영향이 없다).
     */
    @Column(length = 32, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private VideoKind kind;

    /** 출제 필터는 이것만 본다 (D-010). */
    @Column(nullable = false)
    private boolean playable;

    /** videos.list 수집 상태 (D-060). DB는 varchar, 서버는 enum (D-045와 같은 방식). */
    @Column(length = 20, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private VideoCollectionStatus collectionStatus;

    /** 수집 전에는 NULL이다 (D-060). */
    private Integer durationSec;

    private Long viewCount;

    private Instant statsUpdatedAt;

    /** videos.list snippet.title 원문. 검수·추적용이고 표시에는 쓰지 않는다. */
    @Column(length = 300)
    private String titleSnapshot;

    private Instant publishedAt;

    /** {@link #exclude(String)}에서만 채워진다. {@link #restore()}하면 비운다. */
    @Column(length = 500)
    private String excludeReason;

    /** 곡·채널·길이를 이미 아는 상태로 바로 만든다 — 지금은 테스트에서만 쓴다. */
    public static Video create(
        Song song,
        Channel channel,
        String youtubeVideoId,
        VideoKind kind,
        int durationSec,
        Instant publishedAt,
        String titleSnapshot) {

        Video video = new Video();
        video.song = song;
        video.channel = channel;
        video.youtubeVideoId = youtubeVideoId;
        video.kind = kind;
        video.durationSec = durationSec;
        video.publishedAt = publishedAt;
        video.titleSnapshot = titleSnapshot;
        video.playable = true;
        video.collectionStatus = VideoCollectionStatus.COLLECTED;

        return video;
    }

    /** URL(videoId)만 받고 만드는 미수집 영상 (P1-3-3, D-059·D-060). song·channel·kind·길이는 아직 없다. */
    public static Video createUncollected(String youtubeVideoId) {
        Video video = new Video();
        video.youtubeVideoId = youtubeVideoId;
        video.kind = VideoKind.UNDEFINED;
        video.playable = true;
        video.collectionStatus = VideoCollectionStatus.UNCOLLECTED;

        return video;
    }

    /** videos.list 응답으로 메타데이터를 채운다. 채널도 여기서 함께 연결한다. */
    public void markCollected(Channel channel, int durationSec, Instant publishedAt, String titleSnapshot, Long viewCount) {
        this.channel = channel;
        this.durationSec = durationSec;
        this.publishedAt = publishedAt;
        this.titleSnapshot = titleSnapshot;
        this.viewCount = viewCount;
        this.statsUpdatedAt = Instant.now();
        this.collectionStatus = VideoCollectionStatus.COLLECTED;
    }

    /** videos.list 응답에 이 id가 없었다 (삭제·비공개 등, D-060). */
    public void markFailed() {
        this.collectionStatus = VideoCollectionStatus.FAILED;
    }

    /** FAILED → UNCOLLECTED. [다시 대기] 버튼. */
    public void requeue() {
        this.collectionStatus = VideoCollectionStatus.UNCOLLECTED;
    }

    /** 어떤 상태에서든 → EXCLUDED (P1-3-3 결정 1). 이후 배치·수동 수집이 다시 건드리지 않는다. */
    public void exclude(String reason) {
        this.collectionStatus = VideoCollectionStatus.EXCLUDED;
        this.excludeReason = reason;
    }

    /**
     * EXCLUDED를 취소한다. 제외되기 전 정확한 상태는 기록해 두지 않으므로, 길이가
     * 있으면(=한 번이라도 수집됐던 적이 있으면) COLLECTED로, 없으면 UNCOLLECTED로
     * 되돌린다 — 다음 배치가 불필요하게 다시 조회하지 않도록.
     */
    public void restore() {
        this.collectionStatus = this.durationSec != null
            ? VideoCollectionStatus.COLLECTED
            : VideoCollectionStatus.UNCOLLECTED;
        this.excludeReason = null;
    }

    /** 조회수는 하루 1회 배치로 갱신한다 (D-034). 등록 직후에도 한 번 부른다. */
    public void updateStats(Long viewCount) {
        this.viewCount = viewCount;
        this.statsUpdatedAt = Instant.now();
    }

    /** 출제 목록에서 뺀다 (D-010). 되돌리려면 {@link #enablePlayable()}. */
    public void disablePlayable() {
        this.playable = false;
    }

    /** 다시 출제 대상으로 켠다. */
    public void enablePlayable() {
        this.playable = true;
    }
}

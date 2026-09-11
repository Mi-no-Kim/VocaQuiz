package com.vocaquiz.catalog.domain;

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
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    private Channel channel;

    @Column(length = 32, nullable = false)
    private String youtubeVideoId;

    /** 출제 대상은 ORIGINAL로 제한한다 (D-055). song_vocal 재계산도 이 값을 본다 (D-050). */
    @Column(length = 32, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private VideoKind kind;

    /** 출제 필터는 이것만 본다 (D-010). */
    @Column(nullable = false)
    private boolean playable;

    @Column(nullable = false)
    private int durationSec;

    private Long viewCount;

    private Instant statsUpdatedAt;

    /** videos.list snippet.title 원문. 검수·추적용이고 표시에는 쓰지 않는다. */
    @Column(length = 300)
    private String titleSnapshot;

    private Instant publishedAt;

    @Column(nullable = false)
    private Instant createdAt;

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
        video.createdAt = Instant.now();

        return video;
    }

    /** 조회수는 하루 1회 배치로 갱신한다 (D-034). 등록 직후에도 한 번 부른다. */
    public void updateStats(Long viewCount) {
        this.viewCount = viewCount;
        this.statsUpdatedAt = Instant.now();
    }
}

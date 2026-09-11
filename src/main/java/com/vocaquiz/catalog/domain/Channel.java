package com.vocaquiz.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "channel",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_channel_youtube_channel_id",
        columnNames = "youtube_channel_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String youtubeChannelId;

    /** channels.list contentDetails로 채운다. Phase 3까지 null이다 (D-034). */
    @Column(length = 64)
    private String uploadsPlaylistId;

    @Column(length = 200)
    private String name;

    /** 곡 등록 시 자동 기록되지만 감시는 사람이 켠다 (D-031). */
    @Column(nullable = false)
    private boolean watch;

    /** RSS 폴링 워터마크. */
    private Instant lastVideoPublishedAt;

    private Instant lastCheckedAt;

    @Column(nullable = false)
    private Instant createdAt;

    public static Channel create(String youtubeChannelId, String name) {
        Channel channel = new Channel();
        channel.youtubeChannelId = youtubeChannelId;
        channel.name = name;
        channel.watch = false;
        channel.createdAt = Instant.now();

        return channel;
    }
}

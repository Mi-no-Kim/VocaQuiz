package com.vocaquiz.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "song_answer",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_song_answer",
        columnNames = {"song_id", "normalized"}),
    indexes = @Index(
        name = "idx_song_answer_normalized",
        columnList = "normalized"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(length = 300, nullable = false)
    private String normalized;

    @Column(nullable = false)
    private Instant createdAt;

    public static SongAnswer create(Song song, String normalized) {
        SongAnswer songAnswer = new SongAnswer();
        songAnswer.song = song;
        songAnswer.normalized = normalized;
        songAnswer.createdAt = Instant.now();

        return songAnswer;
    }
}

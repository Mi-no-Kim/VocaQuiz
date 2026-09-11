package com.vocaquiz.catalog.domain;

import com.vocaquiz.common.domain.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
public class SongAnswer extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(length = 300, nullable = false)
    private String normalized;

    public static SongAnswer create(Song song, String normalized) {
        SongAnswer songAnswer = new SongAnswer();
        songAnswer.song = song;
        songAnswer.normalized = normalized;

        return songAnswer;
    }
}

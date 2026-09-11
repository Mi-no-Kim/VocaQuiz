package com.vocaquiz.catalog.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 사람이 쓰고 고치는 정답 패턴 원문 (D-052, D-056). <b>진실</b>이다.
 *
 * <p>곡당 한 행이다. 여러 표기는 한 텍스트 안에 줄로 나열한다.
 * 이 값이 바뀌면 그 곡의 song_answer를 통째로 다시 만들어야 한다 —
 * 그 책임은 SongCatalogService에 있다.
 */
@Entity
@Table(name = "song_answer_pattern",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_song_answer_pattern",
        columnNames = "song_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongAnswerPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(length = 2000, nullable = false)
    private String pattern;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static SongAnswerPattern create(Song song, String pattern) {
        SongAnswerPattern songAnswerPattern = new SongAnswerPattern();
        songAnswerPattern.song = song;
        songAnswerPattern.pattern = pattern;
        songAnswerPattern.update();
        songAnswerPattern.createdAt = songAnswerPattern.updatedAt;

        return songAnswerPattern;
    }

    public void updatePattern(String pattern) {
        this.pattern = pattern;
    }

    @PreUpdate
    public void update() {
        this.updatedAt = Instant.now();
    }
}

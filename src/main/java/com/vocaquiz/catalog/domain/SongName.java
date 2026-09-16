package com.vocaquiz.catalog.domain;

import com.vocaquiz.common.domain.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "song_name",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_song_name",
        columnNames = {"song_id", "language_id", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongName extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(length = 300, nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean isPrimary;

    public static SongName create(Song song, Language language, String name, boolean isPrimary) {
        SongName songName = new SongName();
        songName.song = song;
        songName.language = language;
        songName.name = name;
        songName.isPrimary = isPrimary;

        return songName;
    }

    /** 곡 수정(PUT)에서 이 행을 고친다 — id가 있는 이름은 새로 만들지 않고 이 메서드로 바뀐다 (B2). */
    public void update(Language language, String name, boolean isPrimary) {
        this.language = language;
        this.name = name;
        this.isPrimary = isPrimary;
    }
}

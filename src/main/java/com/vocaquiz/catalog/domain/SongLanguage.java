package com.vocaquiz.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "song_language",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_song_language",
        columnNames = {"song_id", "language_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    public static SongLanguage create(Song song, Language language) {
        SongLanguage songLanguage = new SongLanguage();
        songLanguage.song = song;
        songLanguage.language = language;

        return songLanguage;
    }

}

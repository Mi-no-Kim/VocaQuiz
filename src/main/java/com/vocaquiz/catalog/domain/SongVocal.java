package com.vocaquiz.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 그 곡의 ORIGINAL 영상들이 가진 video_vocal의 합집합 (D-050). <b>파생</b>이다.
 *
 * <p>손으로 고치지 않는다. video_vocal이 바뀔 때 통째로 다시 만든다.
 */
@Entity
@Table(name = "song_vocal",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_song_vocal",
        columnNames = {"song_id", "vocal_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongVocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocal_id", nullable = false)
    private Vocal vocal;

    public static SongVocal create(Song song, Vocal vocal) {
        SongVocal songVocal = new SongVocal();
        songVocal.song = song;
        songVocal.vocal = vocal;

        return songVocal;
    }
}

package com.vocaquiz.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이 영상에서 실제로 부른 보컬 (D-050). <b>진실</b>이다.
 *
 * <p>보컬은 곡이 아니라 음원의 속성이다. 여기가 바뀌고 그 영상이 ORIGINAL이면
 * song_vocal을 다시 계산한다 — 그 책임은 SongCatalogService에 있다.
 */
@Entity
@Table(name = "video_vocal",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_video_vocal",
        columnNames = {"video_id", "vocal_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoVocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocal_id", nullable = false)
    private Vocal vocal;

    public static VideoVocal create(Video video, Vocal vocal) {
        VideoVocal videoVocal = new VideoVocal();
        videoVocal.video = video;
        videoVocal.vocal = vocal;

        return videoVocal;
    }
}

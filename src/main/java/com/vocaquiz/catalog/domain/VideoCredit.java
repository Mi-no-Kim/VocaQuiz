package com.vocaquiz.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리믹서와 커버 제작자가 여기에 들어간다 (D-053). 곡이 아니라 그 영상의 크레딧이다.
 *
 * <p>원곡 영상이라도 곡 크레딧을 자동으로 상속하지 않는다. 관리자 화면의
 * "곡 크레딧과 동일" 버튼으로 복사한다.
 */
@Entity
@Table(name = "video_credit",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_video_credit",
        columnNames = {"video_id", "producer_id", "role"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producer_id", nullable = false)
    private Producer producer;

    @Column(length = 32, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private CreditRole role;

    public static VideoCredit create(Video video, Producer producer, CreditRole role) {
        VideoCredit videoCredit = new VideoCredit();
        videoCredit.video = video;
        videoCredit.producer = producer;
        videoCredit.role = role;

        return videoCredit;
    }
}

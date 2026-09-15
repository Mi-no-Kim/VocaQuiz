package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.Video;
import com.vocaquiz.catalog.domain.VideoCollectionStatus;
import com.vocaquiz.catalog.domain.VideoKind;

import java.time.Instant;

/**
 * 카탈로그가 영상 하나에 대해 아는 것 (D-070). 서비스가 트랜잭션 안에서 만들어 돌려준다.
 *
 * <p>엔티티를 경계 밖으로 내보내지 않으려고 둔다. {@code open-in-view: false}라 컨트롤러에는
 * 영속성 컨텍스트가 없어서, 엔티티를 그대로 넘기면 지연 로딩이 컨트롤러에서 터진다.
 *
 * <p><b>API 응답 DTO와 같은 타입이 아니다.</b> 여기에는 카탈로그가 아는 것만 담는다.
 * 화면이 더 필요하면 admin이 자기 DTO에서 채운다 — 화면 모양에 맞춰 이 record의 필드를
 * 늘리기 시작하면 타입만 하나 늘고 결합은 그대로다.
 *
 * <p>지금은 {@code AdminVideoResponse}와 필드가 똑같다. 둘이 끝내 안 갈라지면 위 단서를
 * 못 지킨 것이고, 그때는 D-070의 두 번째 선택지로 되돌리는 편이 정직하다.
 */
public record VideoSummary(
    Long id,
    String youtubeVideoId,
    VideoCollectionStatus collectionStatus,
    VideoKind kind,
    boolean playable,
    Long songId,
    String titleSnapshot,
    Integer durationSec,
    Long viewCount,
    Instant publishedAt,
    String channelName,
    String excludeReason
) {

    /**
     * 엔티티를 읽는 유일한 지점이라 패키지 밖으로 열지 않는다. 이 변환은 반드시 트랜잭션
     * 안에서 일어나야 하는데, 공개하면 컨트롤러가 엔티티를 들고 와 부를 수 있게 된다.
     */
    static VideoSummary from(Video video) {
        return new VideoSummary(
            video.getId(),
            video.getYoutubeVideoId(),
            video.getCollectionStatus(),
            video.getKind(),
            video.isPlayable(),
            video.getSong() != null ? video.getSong().getId() : null,
            video.getTitleSnapshot(),
            video.getDurationSec(),
            video.getViewCount(),
            video.getPublishedAt(),
            video.getChannel() != null ? video.getChannel().getName() : null,
            video.getExcludeReason());
    }
}

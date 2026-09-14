package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.Video;
import com.vocaquiz.catalog.domain.VideoCollectionStatus;
import com.vocaquiz.catalog.domain.VideoKind;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findBySongIdAndKind(Long songId, VideoKind kind);

    Optional<Video> findByYoutubeVideoId(String youtubeVideoId);

    /** channel을 같이 가져온다 — 트랜잭션 밖에서 channel을 읽어야 할 때(테스트 등) 지연 로딩 예외를 막는다. */
    @EntityGraph(attributePaths = "channel")
    Optional<Video> findWithChannelById(Long id);

    /**
     * 배치 수집 대상 — UNCOLLECTED 전체 (D-060). videos.list 호출 1회당 id 50개 제한은
     * {@code YoutubeDataClient.fetchVideos}가 알아서 나눠 부르므로 여기서 개수를 자르지 않는다.
     */
    List<Video> findByCollectionStatusOrderByIdAsc(VideoCollectionStatus collectionStatus);

    /** 관리자 목록. channel을 같이 가져와 화면에서 지연 로딩 예외가 나지 않게 한다. */
    @EntityGraph(attributePaths = "channel")
    List<Video> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = "channel")
    List<Video> findByCollectionStatusOrderByIdDesc(VideoCollectionStatus collectionStatus);
}

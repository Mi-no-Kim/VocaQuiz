package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.Song;
import com.vocaquiz.catalog.domain.SongStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SongRepository extends JpaRepository<Song, Long> {

    /** 관리자 곡 목록 — 검색어 없이 전체(status 필터만) (D-072, ARCH §5.4). */
    @Query("select s from Song s where (:status is null or s.status = :status)")
    Page<Song> findAllByOptionalStatus(@Param("status") SongStatus status, Pageable pageable);

    /**
     * 관리자 곡 목록 — 정답 패턴에서 전개된 {@code song_answer.normalized}로 검색한다
     * (ARCH §5.4). {@code SongAnswer}에서 {@code song} 연관관계를 타고 들어가는 표준 JPQL
     * 경로 조인이라 패턴이 없는 곡은 절대 안 걸린다 — 완료 기준 2.
     *
     * <p>{@code normalized}는 호출자({@link com.vocaquiz.catalog.service.SongService})가
     * {@code %}로 감싸고 LIKE 와일드카드까지 이스케이프해서 넘긴다.
     */
    @Query(
        value = "select distinct sa.song from SongAnswer sa "
            + "where sa.normalized like :normalized escape '\\' "
            + "and (:status is null or sa.song.status = :status)",
        countQuery = "select count(distinct sa.song.id) from SongAnswer sa "
            + "where sa.normalized like :normalized escape '\\' "
            + "and (:status is null or sa.song.status = :status)")
    Page<Song> searchByNormalizedAndOptionalStatus(
        @Param("normalized") String normalized, @Param("status") SongStatus status, Pageable pageable);
}

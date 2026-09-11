package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.*;
import com.vocaquiz.catalog.repository.*;
import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 파생 테이블 둘의 <b>유일한 갱신 경로</b>다.
 *
 * <ul>
 *   <li>song_answer ← song_answer_pattern.pattern (D-052, D-056)
 *   <li>song_vocal ← ORIGINAL 영상들의 video_vocal (D-050)
 * </ul>
 *
 * <p>이 클래스를 거치지 않고 원본을 바꾸면 파생이 뒤처진다. 자동완성에서 곡이 안
 * 잡히거나 범위지정 필터가 곡을 빠뜨리는데, <b>아무 에러도 나지 않는다.</b>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SongCatalogService {

    private final SongRepository songRepository;
    private final SongAnswerPatternRepository songAnswerPatternRepository;
    private final SongAnswerRepository songAnswerRepository;
    private final SongVocalRepository songVocalRepository;
    private final VideoRepository videoRepository;
    private final VideoVocalRepository videoVocalRepository;
    private final VocalRepository vocalRepository;

    /**
     * 곡의 정답 패턴 원문을 넣거나 고치고, 그 곡의 song_answer를 다시 만든다.
     *
     * <p>문법이 틀리면 전개기가 몇 번째 줄인지와 함께 던지고 트랜잭션이 통째로
     * 되돌아간다. 원문만 저장되고 파생이 뒤처지는 상태가 생기지 않는다.
     */
    @Transactional
    public void replaceAnswerPattern(Long songId, String pattern) {
        Song song = songRepository.findById(songId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "song " + songId));

        songAnswerPatternRepository.findBySongId(songId)
            .ifPresentOrElse(
                existing -> existing.updatePattern(pattern),
                () -> songAnswerPatternRepository.save(SongAnswerPattern.create(song, pattern)));

        rebuildAnswers(songId, pattern);
    }

    /** 영상의 보컬을 통째로 바꾼다. 그 영상이 ORIGINAL이면 곡의 song_vocal을 다시 계산한다. */
    @Transactional
    public void replaceVideoVocals(Long videoId, List<Long> vocalIds) {
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "video " + videoId));

        // 벌크 삭제가 영속성 컨텍스트를 비우므로 필요한 값을 미리 꺼내 둔다.
        VideoKind kind = video.getKind();
        Long songId = video.getSong().getId();
        Set<Long> wanted = new LinkedHashSet<>(vocalIds);

        List<Vocal> vocals = vocalRepository.findAllById(wanted);
        if (vocals.size() != wanted.size()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "vocal " + vocalIds);
        }

        videoVocalRepository.deleteByVideoId(videoId);

        Video videoRef = videoRepository.getReferenceById(videoId);
        List<VideoVocal> rows = new ArrayList<>(vocals.size());
        vocals.forEach(vocal -> rows.add(VideoVocal.create(videoRef, vocalRepository.getReferenceById(vocal.getId()))));
        videoVocalRepository.saveAll(rows);

        if (kind == VideoKind.ORIGINAL) {
            rebuildSongVocals(songId);
        }
    }

    /**
     * 원문을 전개하고 정규화해 song_answer를 통째로 다시 만든다.
     *
     * <p>지우고 다시 넣는다 — song_answer의 id를 참조하는 곳이 없어 유지할 이유가 없다.
     * 빈 후보는 버린다. {@code ()} 같은 줄이 내는 빈 문자열은 아무것도 매칭하지 않는다.
     *
     * <p>{@code deleteBySongId}는 벌크 삭제이고 {@code flushAutomatically}로 먼저
     * 내보낸다. 이게 없으면 Hibernate가 insert를 delete보다 앞세워
     * UNIQUE (song_id, normalized)에 걸린다.
     */
    private void rebuildAnswers(Long songId, String pattern) {
        Set<String> keys = new LinkedHashSet<>();
        for (String candidate : AnswerPatternExpander.expand(pattern)) {
            String normalized = TextNormalizer.normalize(candidate);
            if (!normalized.isEmpty()) {
                keys.add(normalized);
            }
        }

        songAnswerRepository.deleteBySongId(songId);

        Song songRef = songRepository.getReferenceById(songId);
        List<SongAnswer> answers = new ArrayList<>(keys.size());
        keys.forEach(key -> answers.add(SongAnswer.create(songRef, key)));
        songAnswerRepository.saveAll(answers);
    }

    /**
     * 그 곡의 ORIGINAL 영상들이 가진 video_vocal의 합집합으로 song_vocal을 다시 만든다.
     *
     * <p>ORIGINAL 영상이 하나도 없으면 빈 집합이 된다. 그런 곡은 정합성 점검에서
     * 제외한다 (D-050).
     */
    private void rebuildSongVocals(Long songId) {
        List<Long> originalVideoIds = videoRepository
            .findBySongIdAndKind(songId, VideoKind.ORIGINAL)
            .stream()
            .map(Video::getId)
            .toList();

        Set<Long> vocalIds = new LinkedHashSet<>();
        if (!originalVideoIds.isEmpty()) {
            videoVocalRepository.findByVideoIdIn(originalVideoIds)
                .forEach(videoVocal -> vocalIds.add(videoVocal.getVocal().getId()));
        }

        songVocalRepository.deleteBySongId(songId);

        if (vocalIds.isEmpty()) {
            return;
        }

        Song songRef = songRepository.getReferenceById(songId);
        List<SongVocal> rows = new ArrayList<>(vocalIds.size());
        vocalIds.forEach(vocalId -> rows.add(SongVocal.create(songRef, vocalRepository.getReferenceById(vocalId))));
        songVocalRepository.saveAll(rows);
    }
}

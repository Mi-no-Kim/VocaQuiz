package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.CreditRole;
import com.vocaquiz.catalog.domain.Language;
import com.vocaquiz.catalog.domain.Producer;
import com.vocaquiz.catalog.domain.Song;
import com.vocaquiz.catalog.domain.SongAnswerPattern;
import com.vocaquiz.catalog.domain.SongCredit;
import com.vocaquiz.catalog.domain.SongLanguage;
import com.vocaquiz.catalog.domain.SongName;
import com.vocaquiz.catalog.domain.SongStatus;
import com.vocaquiz.catalog.domain.VideoCollectionStatus;
import com.vocaquiz.catalog.domain.VideoKind;
import com.vocaquiz.catalog.repository.LanguageRepository;
import com.vocaquiz.catalog.repository.ProducerRepository;
import com.vocaquiz.catalog.repository.SongAnswerPatternRepository;
import com.vocaquiz.catalog.repository.SongCreditRepository;
import com.vocaquiz.catalog.repository.SongLanguageRepository;
import com.vocaquiz.catalog.repository.SongNameRepository;
import com.vocaquiz.catalog.repository.SongRepository;
import com.vocaquiz.catalog.repository.VideoRepository;
import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 곡 생성·조회·수정 (D-061). PUBLISHED 전환 조건 검사(D-062)를 한 곳({@link
 * #missingConditionsFor})에 모아 생성·수정 양쪽에서 부른다.
 *
 * <p>song_answer는 여기서 건드리지 않는다 — {@link SongCatalogService#replaceAnswerPattern}·
 * {@link SongCatalogService#clearAnswerPattern}이 유일한 경로다 (D-052).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SongService {

    private final SongRepository songRepository;
    private final LanguageRepository languageRepository;
    private final SongNameRepository songNameRepository;
    private final SongLanguageRepository songLanguageRepository;
    private final SongCreditRepository songCreditRepository;
    private final SongAnswerPatternRepository songAnswerPatternRepository;
    private final ProducerRepository producerRepository;
    private final VideoRepository videoRepository;
    private final SongCatalogService songCatalogService;

    /**
     * 곡을 만든다. 최소 입력은 원제 언어와 그 언어의 대표 이름이다 (D-061).
     *
     * <p>{@code status}로 PUBLISHED를 주면 {@link #missingConditionsFor}를 통과해야 한다.
     * 새로 만드는 곡은 수집된 ORIGINAL 영상이 있을 수 없어(D-062 조건 3) 이 경로로는 항상
     * 걸린다 — 즉 지금은 DRAFT로 만든 뒤 조건이 갖춰지면 수정(PUT)으로 PUBLISHED 전환하는
     * 것만 가능하다.
     *
     * <p>{@code languageIds}·{@code producerIds}의 중복은 조용히 걷어낸다 — 다중 선택 UI가
     * 같은 값을 두 번 보내는 것은 흔한 실수지, 막을 값이 있는 요청이 아니다.
     */
    @Transactional
    public SongSummary create(
            Long originalLanguageId,
            List<SongNameInput> names,
            List<Long> languageIds,
            List<Long> producerIds,
            String answerPattern,
            SongStatus status) {
        List<SongNameInput> nameList = names == null ? List.of() : names;
        Set<Long> uniqueLanguageIds = new LinkedHashSet<>(languageIds == null ? List.of() : languageIds);
        Set<Long> uniqueProducerIds = new LinkedHashSet<>(producerIds == null ? List.of() : producerIds);

        validateNames(originalLanguageId, nameList);

        Language originalLanguage = languageRepository.findById(originalLanguageId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "language " + originalLanguageId));

        Song song = songRepository.save(Song.create(originalLanguage, status));

        nameList.forEach(n -> songNameRepository.save(
            SongName.create(song, languageRef(n.languageId()), n.name(), n.primary())));

        uniqueLanguageIds.forEach(languageId -> songLanguageRepository.save(
            SongLanguage.create(song, languageRef(languageId))));

        uniqueProducerIds.forEach(producerId -> songCreditRepository.save(
            SongCredit.create(song, producerRef(producerId), CreditRole.PRODUCER)));

        if (answerPattern != null && !answerPattern.isBlank()) {
            songCatalogService.replaceAnswerPattern(song.getId(), answerPattern);
        }

        requirePublishReady(song.getId(), originalLanguageId, status);

        return SongSummary.from(song);
    }

    /** 곡 하나의 전체 상세를 준다 — 수정 화면이 초기값으로 쓴다. */
    public SongDetail get(Long id) {
        Song song = songRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "song " + id));

        return songDetailOf(song);
    }

    /**
     * 곡을 고친다 — PUT 전체교체다 (B2). {@code names}는 id로 diff한다: id 없는 항목은
     * 새로 만들고, id 있는 항목은 그 행을 고치고, 요청에 없는 기존 id는 지운다.
     * {@code languageIds}·{@code producerIds}는 통째로 지우고 다시 쓴다 — song_language·
     * song_credit은 자기 필드가 없는 순수 연결 테이블이라 diff할 이유가 없다.
     *
     * <p>{@code answerPattern}을 비우면 기존 패턴을 지운다({@link
     * SongCatalogService#clearAnswerPattern}) — PUT은 전체 교체가 관례라, 필드를 안 보내는
     * 것도 "비어 있음"으로 본다.
     *
     * <p>{@code status}로 PUBLISHED를 주면 {@link #missingConditionsFor}를 통과해야 한다.
     */
    @Transactional
    public SongDetail update(
            Long id,
            Long originalLanguageId,
            List<UpdateSongNameInput> names,
            List<Long> languageIds,
            List<Long> producerIds,
            String answerPattern,
            SongStatus status) {
        Song song = songRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "song " + id));

        List<UpdateSongNameInput> nameList = names == null ? List.of() : names;
        Set<Long> uniqueLanguageIds = new LinkedHashSet<>(languageIds == null ? List.of() : languageIds);
        Set<Long> uniqueProducerIds = new LinkedHashSet<>(producerIds == null ? List.of() : producerIds);

        validateNames(originalLanguageId, nameList.stream()
            .map(n -> new SongNameInput(n.languageId(), n.name(), n.primary()))
            .toList());

        Language originalLanguage = languageRepository.findById(originalLanguageId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "language " + originalLanguageId));
        song.changeOriginalLanguage(originalLanguage);
        song.changeStatus(status);

        replaceNames(song, nameList);

        songLanguageRepository.deleteBySongId(id);
        uniqueLanguageIds.forEach(languageId -> songLanguageRepository.save(
            SongLanguage.create(song, languageRef(languageId))));

        songCreditRepository.deleteBySongId(id);
        uniqueProducerIds.forEach(producerId -> songCreditRepository.save(
            SongCredit.create(song, producerRef(producerId), CreditRole.PRODUCER)));

        if (answerPattern == null || answerPattern.isBlank()) {
            songCatalogService.clearAnswerPattern(id);
        } else {
            songCatalogService.replaceAnswerPattern(id, answerPattern);
        }

        requirePublishReady(id, originalLanguageId, status);

        return songDetailOf(song);
    }

    /**
     * 이름이 있는 언어마다 대표 이름은 정확히 1개, 원제 언어에는 대표 이름이 반드시 있어야
     * 한다 (D-061). DB로는 막지 못해 여기서 막는다 — 부분 유니크 인덱스를 MySQL도 H2도
     * 지원하지 않는다.
     */
    private void validateNames(Long originalLanguageId, List<SongNameInput> names) {
        Map<Long, Long> totalCountByLanguage = new LinkedHashMap<>();
        Map<Long, Long> primaryCountByLanguage = new LinkedHashMap<>();
        for (SongNameInput n : names) {
            totalCountByLanguage.merge(n.languageId(), 1L, Long::sum);
            if (n.primary()) {
                primaryCountByLanguage.merge(n.languageId(), 1L, Long::sum);
            }
        }

        for (Long languageId : totalCountByLanguage.keySet()) {
            long primaryCount = primaryCountByLanguage.getOrDefault(languageId, 0L);
            if (primaryCount != 1) {
                throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "언어 " + languageId + "의 대표 이름은 정확히 1개여야 한다 (현재 " + primaryCount + "개)");
            }
        }

        if (!totalCountByLanguage.containsKey(originalLanguageId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "원제 언어의 대표 이름이 필요하다");
        }
    }

    /**
     * song_name을 id 기준으로 diff한다 (B2) — id 없으면 insert, id 있으면 그 행을 update,
     * 기존 행 중 요청에 없는 id는 delete. {@code existingById}는 {@link
     * SongNameRepository#findBySongId}로 이미 이 곡 것만 걸러져 있어 다른 곡의 id를 주면
     * 자연히 NOT_FOUND가 된다.
     *
     * <p>delete를 insert보다 먼저 반영한다 — song_name은 (song_id, language_id, name)
     * 유니크라, 지워질 행과 같은 값으로 새 행을 넣으면(예: id 없이 같은 이름을 다시 보낸
     * 경우) insert가 먼저 나가는 순서에서는 유니크 제약에 걸린다. IDENTITY 채번이라 save()가
     * 곧바로 insert를 내보내므로, 중간에 flush로 delete를 실제 반영해야 한다.
     */
    private void replaceNames(Song song, List<UpdateSongNameInput> names) {
        Map<Long, SongName> existingById = songNameRepository.findBySongId(song.getId()).stream()
            .collect(Collectors.toMap(SongName::getId, sn -> sn));

        Set<Long> keepIds = new LinkedHashSet<>();
        List<UpdateSongNameInput> toInsert = new ArrayList<>();
        for (UpdateSongNameInput n : names) {
            if (n.id() == null) {
                toInsert.add(n);
                continue;
            }

            SongName existing = existingById.get(n.id());
            if (existing == null) {
                throw new ApiException(ErrorCode.NOT_FOUND, "song name " + n.id());
            }
            existing.update(languageRef(n.languageId()), n.name(), n.primary());
            keepIds.add(n.id());
        }

        existingById.keySet().stream()
            .filter(existingId -> !keepIds.contains(existingId))
            .forEach(songNameRepository::deleteById);
        songNameRepository.flush();

        toInsert.forEach(n -> songNameRepository.save(
            SongName.create(song, languageRef(n.languageId()), n.name(), n.primary())));
    }

    private void requirePublishReady(Long songId, Long originalLanguageId, SongStatus status) {
        if (status != SongStatus.PUBLISHED) {
            return;
        }

        List<String> missing = missingConditionsFor(songId, originalLanguageId);
        if (!missing.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "PUBLISHED 조건을 채우지 못했다", missing);
        }
    }

    /**
     * PUBLISHED 전환 조건 3가지 (D-062) — (1) 원제 언어의 대표 이름, (2) 정답 패턴,
     * (3) 수집된 ORIGINAL 영상 1개 이상. 빠진 것만 문구로 돌려준다(빈 리스트면 충족).
     */
    private List<String> missingConditionsFor(Long songId, Long originalLanguageId) {
        List<String> missing = new ArrayList<>();

        if (!songNameRepository.existsBySongIdAndLanguageIdAndIsPrimary(songId, originalLanguageId, true)) {
            missing.add("원제 언어의 대표 이름이 없다");
        }
        if (songAnswerPatternRepository.findBySongId(songId).isEmpty()) {
            missing.add("정답 패턴이 없다");
        }
        if (!videoRepository.existsBySongIdAndKindAndCollectionStatus(
                songId, VideoKind.ORIGINAL, VideoCollectionStatus.COLLECTED)) {
            missing.add("수집된 원곡(ORIGINAL) 영상이 없다");
        }

        return missing;
    }

    private SongDetail songDetailOf(Song song) {
        List<SongDetail.SongNameDetail> names = songNameRepository.findBySongId(song.getId()).stream()
            .map(SongDetail.SongNameDetail::from)
            .toList();
        List<Long> languageIds = songLanguageRepository.findBySongId(song.getId()).stream()
            .map(sl -> sl.getLanguage().getId())
            .toList();
        List<Long> producerIds = songCreditRepository.findBySongId(song.getId()).stream()
            .map(sc -> sc.getProducer().getId())
            .toList();
        String answerPattern = songAnswerPatternRepository.findBySongId(song.getId())
            .map(SongAnswerPattern::getPattern)
            .orElse(null);

        return new SongDetail(
            song.getId(),
            song.getOriginalLanguage().getId(),
            song.getStatus(),
            names,
            languageIds,
            producerIds,
            answerPattern);
    }

    private Language languageRef(Long languageId) {
        if (!languageRepository.existsById(languageId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "language " + languageId);
        }
        return languageRepository.getReferenceById(languageId);
    }

    private Producer producerRef(Long producerId) {
        if (!producerRepository.existsById(producerId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "producer " + producerId);
        }
        return producerRepository.getReferenceById(producerId);
    }
}

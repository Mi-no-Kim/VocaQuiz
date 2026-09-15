package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.CreditRole;
import com.vocaquiz.catalog.domain.Language;
import com.vocaquiz.catalog.domain.Producer;
import com.vocaquiz.catalog.domain.Song;
import com.vocaquiz.catalog.domain.SongCredit;
import com.vocaquiz.catalog.domain.SongLanguage;
import com.vocaquiz.catalog.domain.SongName;
import com.vocaquiz.catalog.domain.SongStatus;
import com.vocaquiz.catalog.repository.LanguageRepository;
import com.vocaquiz.catalog.repository.ProducerRepository;
import com.vocaquiz.catalog.repository.SongCreditRepository;
import com.vocaquiz.catalog.repository.SongLanguageRepository;
import com.vocaquiz.catalog.repository.SongNameRepository;
import com.vocaquiz.catalog.repository.SongRepository;
import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 곡 생성 (D-061). 조회·수정·PUBLISHED 전환 조건 검사(D-062)는 다음 커밋에서 더한다.
 *
 * <p>song_answer는 여기서 건드리지 않는다 — {@link SongCatalogService#replaceAnswerPattern}이
 * 유일한 경로다 (D-052).
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
    private final ProducerRepository producerRepository;
    private final SongCatalogService songCatalogService;

    /**
     * 곡을 만든다. 최소 입력은 원제 언어와 그 언어의 대표 이름이다 (D-061).
     *
     * <p>{@code status}를 그대로 받는다 — PUBLISHED를 골라도 지금은 막지 않는다. 새로 만드는
     * 곡은 수집된 ORIGINAL 영상이 있을 수 없어(D-062 조건 3) 그 검사가 들어오면 이 경로는
     * 항상 걸리게 되는데, 그 조건 검사는 조회·수정과 함께 다음 커밋에서 한 곳에 모아 넣고
     * 이 메서드도 그때 다시 부르게 고친다.
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

        return SongSummary.from(song);
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

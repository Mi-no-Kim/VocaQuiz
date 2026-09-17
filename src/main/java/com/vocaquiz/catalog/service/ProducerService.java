package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.Language;
import com.vocaquiz.catalog.domain.Producer;
import com.vocaquiz.catalog.domain.ProducerName;
import com.vocaquiz.catalog.repository.LanguageRepository;
import com.vocaquiz.catalog.repository.ProducerNameRepository;
import com.vocaquiz.catalog.repository.ProducerRepository;
import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 프로듀서 자동완성과 생성 (D-063, D-073).
 *
 * <p>이름은 이제 언어별로 여러 개다. 유일함의 기준은 프로듀서 전체가 아니라 언어 안이다 —
 * {@code producer_name}의 (language_id, name) UNIQUE가 이를 보장한다 (D-073).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProducerService {

    private final ProducerRepository producerRepository;
    private final ProducerNameRepository producerNameRepository;
    private final LanguageRepository languageRepository;

    /**
     * {@code q}가 없으면 전체를 준다. 이름이 더는 프로듀서 자신의 컬럼이 아니라 여러 개라
     * 이름순 정렬은 포기하고 id순으로 준다.
     *
     * <p>검색은 언어 구분 없이 모든 언어의 이름을 다 뒤진다 (D-073) — 관리자가 어느 언어로
     * 검색하든 그 프로듀서를 찾을 수 있어야 자동완성의 중복 방지 역할(D-063)이 유지된다.
     *
     * <p>{@code q}에 든 {@code %}·{@code _}는 LIKE 와일드카드가 아니라 리터럴로 다룬다.
     */
    public List<ProducerSummary> search(String q) {
        List<Long> producerIds = (q == null || q.isBlank())
            ? producerRepository.findAll().stream().map(Producer::getId).toList()
            : producerNameRepository.searchProducerIdsByName("%" + escapeLike(q) + "%");

        return producerIds.stream()
            .map(id -> ProducerSummary.of(id, producerNameRepository.findByProducerId(id)))
            .toList();
    }

    /**
     * 프로듀서를 만든다. 최소 입력은 이름 1개 이상, 이름이 있는 언어마다 대표 이름은 정확히
     * 1개다 (D-073) — song_name(D-061, D-072)과 같은 규칙이다.
     *
     * <p>같은 언어에 같은 표기가 이미 있으면 409 (D-063, D-073).
     */
    @Transactional
    public ProducerSummary create(List<ProducerNameInput> names) {
        List<ProducerNameInput> nameList = names == null ? List.of() : names;
        validateNames(nameList);

        Producer producer = producerRepository.save(Producer.create());

        List<ProducerName> saved = nameList.stream()
            .map(n -> producerNameRepository.save(
                ProducerName.create(producer, languageRef(n.languageId()), n.name(), n.primary())))
            .toList();

        return ProducerSummary.of(producer.getId(), saved);
    }

    /**
     * 이름이 1개 이상 있어야 하고, 이름이 있는 언어마다 대표 이름은 정확히 1개다 (D-073).
     * DB로는 "언어마다 대표 1개"를 막지 못해 여기서 막는다 — 부분 유니크 인덱스를 MySQL도
     * H2도 지원하지 않는다(song_name의 validateNames와 같은 이유).
     *
     * <p>(language_id, name) 중복은 DB UNIQUE로도 막히지만, 409로 명확히 알려주려고 여기서
     * 먼저 검사한다 (D-063).
     */
    private void validateNames(List<ProducerNameInput> names) {
        if (names.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이름이 하나 이상 있어야 한다");
        }

        Map<Long, Long> totalCountByLanguage = new LinkedHashMap<>();
        Map<Long, Long> primaryCountByLanguage = new LinkedHashMap<>();
        for (ProducerNameInput n : names) {
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

        for (ProducerNameInput n : names) {
            if (producerNameRepository.existsByLanguageIdAndName(n.languageId(), n.name())) {
                throw new ApiException(ErrorCode.CONFLICT, "이미 있는 프로듀서 이름: " + n.name());
            }
        }
    }

    private Language languageRef(Long languageId) {
        if (!languageRepository.existsById(languageId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "language " + languageId);
        }
        return languageRepository.getReferenceById(languageId);
    }

    /** LIKE의 이스케이프 문자 자신부터 이스케이프해야 뒤에 붙이는 {@code %}·{@code _}가 겹치지 않는다. */
    private static String escapeLike(String raw) {
        return raw
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}

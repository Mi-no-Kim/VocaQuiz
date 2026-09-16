package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.Producer;
import com.vocaquiz.catalog.repository.ProducerRepository;
import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 프로듀서 자동완성과 생성 (D-013·D-053·D-063).
 *
 * <p>이름은 원어 표기 하나만 관리하고 {@code producer.name}에 UNIQUE가 걸려 있다 (D-063).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProducerService {

    private final ProducerRepository producerRepository;

    /**
     * {@code q}가 없으면 전체를 이름순으로 준다.
     *
     * <p>{@code q}에 든 {@code %}·{@code _}는 LIKE 와일드카드가 아니라 리터럴로 다룬다 —
     * 이스케이프하지 않으면 "de_o" 같은 검색어가 밑줄 자리에 아무 글자나 매치해 버린다.
     */
    public List<ProducerSummary> search(String q) {
        String pattern = "%" + escapeLike(q == null ? "" : q) + "%";
        return producerRepository.searchByName(pattern).stream()
            .map(ProducerSummary::from)
            .toList();
    }

    /** 같은 이름이 있으면 409 (D-063). */
    @Transactional
    public ProducerSummary create(String name) {
        producerRepository.findByName(name).ifPresent(existing -> {
            throw new ApiException(ErrorCode.CONFLICT, "이미 있는 프로듀서: " + name);
        });
        return ProducerSummary.from(producerRepository.save(Producer.create(name)));
    }

    /** LIKE의 이스케이프 문자 자신부터 이스케이프해야 뒤에 붙이는 {@code %}·{@code _}가 겹치지 않는다. */
    private static String escapeLike(String raw) {
        return raw
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}

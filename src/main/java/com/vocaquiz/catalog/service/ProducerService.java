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

    /** {@code q}가 없으면 전체를 이름순으로 준다. */
    public List<ProducerSummary> search(String q) {
        return producerRepository.searchByName(q == null ? "" : q).stream()
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
}

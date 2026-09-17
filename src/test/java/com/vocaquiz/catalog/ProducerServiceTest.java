package com.vocaquiz.catalog;

import com.vocaquiz.catalog.repository.LanguageRepository;
import com.vocaquiz.catalog.service.ProducerNameInput;
import com.vocaquiz.catalog.service.ProducerService;
import com.vocaquiz.catalog.service.ProducerSummary;
import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProducerServiceTest {

    @Autowired ProducerService producerService;
    @Autowired LanguageRepository languageRepository;

    private Long koreanId;
    private Long japaneseId;

    @BeforeEach
    void setUp() {
        koreanId = languageRepository.findByCode("KO").orElseThrow().getId();
        japaneseId = languageRepository.findByCode("JA").orElseThrow().getId();
    }

    @Test
    @DisplayName("완료 기준 4 — 없는 프로듀서를 언어별 이름으로 새로 만들 수 있다 (D-073)")
    void createsNewProducer() {
        ProducerSummary created = producerService.create(
            List.of(new ProducerNameInput(japaneseId, "wowaka", true)));

        assertThat(created.id()).isNotNull();
        assertThat(created.names())
            .extracting(ProducerSummary.ProducerNameSummary::name)
            .containsExactly("wowaka");
    }

    @Test
    @DisplayName("이름 없이 만들면 400 (D-073)")
    void rejectsCreationWithoutAnyName() {
        assertThatThrownBy(() -> producerService.create(List.of()))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("한 언어에 대표가 둘이면 400 (D-073)")
    void rejectsTwoPrimaryNamesInSameLanguage() {
        assertThatThrownBy(() -> producerService.create(List.of(
            new ProducerNameInput(japaneseId, "wowaka", true),
            new ProducerNameInput(japaneseId, "ぼかろP", true))))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("같은 언어에 같은 표기로 다시 만들면 409 (D-063, D-073)")
    void rejectsDuplicateNameInSameLanguage() {
        producerService.create(List.of(new ProducerNameInput(japaneseId, "DECO*27", true)));

        assertThatThrownBy(() -> producerService.create(
            List.of(new ProducerNameInput(japaneseId, "DECO*27", true))))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("UNIQUE 범위는 언어별이라, 다른 언어에서는 같은 표기를 다시 쓸 수 있다 (D-073)")
    void allowsSameNameInDifferentLanguages() {
        producerService.create(List.of(new ProducerNameInput(japaneseId, "DECO*27", true)));

        ProducerSummary created = producerService.create(
            List.of(new ProducerNameInput(koreanId, "DECO*27", true)));

        assertThat(created.id()).isNotNull();
    }

    @Test
    @DisplayName("검색은 대소문자를 구분하지 않는다 (자동완성이 표기 흔들림을 막는 자리, D-057·D-063)")
    void searchIsCaseInsensitive() {
        producerService.create(List.of(new ProducerNameInput(japaneseId, "DECO*27", true)));

        assertThat(producerService.search("deco"))
            .flatMap(ProducerSummary::names)
            .extracting(ProducerSummary.ProducerNameSummary::name)
            .containsExactly("DECO*27");
    }

    @Test
    @DisplayName("검색은 언어 구분 없이 모든 언어의 이름을 다 뒤진다 (D-073)")
    void searchMatchesAnyLanguage() {
        ProducerSummary created = producerService.create(List.of(
            new ProducerNameInput(japaneseId, "ぼかろP", true),
            new ProducerNameInput(koreanId, "보카로피", true)));

        assertThat(producerService.search("보카로"))
            .extracting(ProducerSummary::id)
            .containsExactly(created.id());
    }

    @Test
    @DisplayName("검색어가 없으면 전체를 준다")
    void searchWithoutQueryReturnsAll() {
        ProducerSummary wowaka = producerService.create(
            List.of(new ProducerNameInput(japaneseId, "wowaka", true)));
        ProducerSummary deco27 = producerService.create(
            List.of(new ProducerNameInput(japaneseId, "DECO*27", true)));

        assertThat(producerService.search(null))
            .extracting(ProducerSummary::id)
            .containsExactly(wowaka.id(), deco27.id());
    }

    @Test
    @DisplayName("검색어의 %·_는 와일드카드가 아니라 리터럴로 다룬다")
    void searchTreatsWildcardCharactersAsLiteral() {
        producerService.create(List.of(new ProducerNameInput(japaneseId, "de_o", true)));
        producerService.create(List.of(new ProducerNameInput(japaneseId, "deXo", true)));

        assertThat(producerService.search("de_o"))
            .flatMap(ProducerSummary::names)
            .extracting(ProducerSummary.ProducerNameSummary::name)
            .containsExactly("de_o");
    }
}

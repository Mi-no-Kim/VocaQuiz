package com.vocaquiz.catalog;

import com.vocaquiz.catalog.service.ProducerService;
import com.vocaquiz.catalog.service.ProducerSummary;
import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProducerServiceTest {

    @Autowired ProducerService producerService;

    @Test
    @DisplayName("완료 기준 4 — 없는 프로듀서를 새로 만들 수 있다")
    void createsNewProducer() {
        ProducerSummary created = producerService.create("wowaka");

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("wowaka");
    }

    @Test
    @DisplayName("완료 기준 4 — 같은 이름으로 다시 만들면 409")
    void rejectsDuplicateName() {
        producerService.create("DECO*27");

        assertThatThrownBy(() -> producerService.create("DECO*27"))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("검색은 대소문자를 구분하지 않는다 (자동완성이 표기 흔들림을 막는 자리, D-057·D-063)")
    void searchIsCaseInsensitive() {
        producerService.create("DECO*27");

        assertThat(producerService.search("deco"))
            .extracting(ProducerSummary::name)
            .containsExactly("DECO*27");
    }

    @Test
    @DisplayName("검색어가 없으면 전체를 이름순으로 준다")
    void searchWithoutQueryReturnsAllSortedByName() {
        producerService.create("wowaka");
        producerService.create("DECO*27");

        assertThat(producerService.search(null))
            .extracting(ProducerSummary::name)
            .containsExactly("DECO*27", "wowaka");
    }

    @Test
    @DisplayName("검색어의 %·_는 와일드카드가 아니라 리터럴로 다룬다")
    void searchTreatsWildcardCharactersAsLiteral() {
        producerService.create("de_o");
        producerService.create("deXo");

        assertThat(producerService.search("de_o"))
            .extracting(ProducerSummary::name)
            .containsExactly("de_o");
    }
}

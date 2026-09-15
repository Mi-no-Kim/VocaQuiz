package com.vocaquiz.catalog;

import com.vocaquiz.catalog.domain.Producer;
import com.vocaquiz.catalog.domain.SongStatus;
import com.vocaquiz.catalog.repository.LanguageRepository;
import com.vocaquiz.catalog.repository.ProducerRepository;
import com.vocaquiz.catalog.repository.SongAnswerRepository;
import com.vocaquiz.catalog.repository.SongCreditRepository;
import com.vocaquiz.catalog.repository.SongLanguageRepository;
import com.vocaquiz.catalog.service.SongNameInput;
import com.vocaquiz.catalog.service.SongService;
import com.vocaquiz.catalog.service.SongSummary;
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
class SongServiceTest {

    @Autowired SongService songService;
    @Autowired LanguageRepository languageRepository;
    @Autowired ProducerRepository producerRepository;
    @Autowired SongAnswerRepository songAnswerRepository;
    @Autowired SongCreditRepository songCreditRepository;
    @Autowired SongLanguageRepository songLanguageRepository;

    private Long koreanId;
    private Long japaneseId;

    @BeforeEach
    void setUp() {
        koreanId = languageRepository.findByCode("KO").orElseThrow().getId();
        japaneseId = languageRepository.findByCode("JA").orElseThrow().getId();
    }

    @Test
    @DisplayName("완료 기준 1 — 원제 언어의 대표 이름 없이 곡을 만들면 400")
    void rejectsCreationWithoutOriginalPrimaryName() {
        assertThatThrownBy(() -> songService.create(
            koreanId,
            List.of(new SongNameInput(japaneseId, "千本桜", true)),
            List.of(), List.of(), null, SongStatus.DRAFT))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("완료 기준 1 — 한 언어에 대표가 둘이면 400")
    void rejectsCreationWithTwoPrimaryNamesInSameLanguage() {
        assertThatThrownBy(() -> songService.create(
            koreanId,
            List.of(
                new SongNameInput(koreanId, "천본앵", true),
                new SongNameInput(koreanId, "천본사쿠라", true)),
            List.of(), List.of(), null, SongStatus.DRAFT))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("원제 언어의 대표 이름만 있으면 최소 입력으로 만들어진다")
    void createsWithMinimumInput() {
        SongSummary summary = songService.create(
            koreanId,
            List.of(new SongNameInput(koreanId, "천본앵", true)),
            List.of(), List.of(), null, SongStatus.DRAFT);

        assertThat(summary.id()).isNotNull();
        assertThat(summary.originalLanguageId()).isEqualTo(koreanId);
        assertThat(summary.status()).isEqualTo(SongStatus.DRAFT);
    }

    @Test
    @DisplayName("완료 기준 5 — 저장한 곡의 song_answer가 패턴 전개 결과와 같다")
    void createsSongAnswerFromPattern() {
        SongSummary summary = songService.create(
            koreanId,
            List.of(new SongNameInput(koreanId, "히토마니아", true)),
            List.of(), List.of(),
            "(히토|인간|사람)(마니아|매니아)",
            SongStatus.DRAFT);

        assertThat(songAnswerRepository.findBySongId(summary.id())).hasSize(6);
    }

    @Test
    @DisplayName("프로듀서 크레딧을 함께 만든다")
    void createsCreditsForGivenProducers() {
        Long producerId = producerRepository.save(Producer.create("wowaka")).getId();

        SongSummary summary = songService.create(
            koreanId,
            List.of(new SongNameInput(koreanId, "천본앵", true)),
            List.of(), List.of(producerId), null, SongStatus.DRAFT);

        assertThat(songCreditRepository.findAll())
            .filteredOn(credit -> credit.getSong().getId().equals(summary.id()))
            .extracting(credit -> credit.getProducer().getId())
            .containsExactly(producerId);
    }

    @Test
    @DisplayName("같은 프로듀서를 두 번 주면 크레딧은 한 번만 만들어진다")
    void deduplicatesRepeatedProducerIds() {
        Long producerId = producerRepository.save(Producer.create("wowaka")).getId();

        SongSummary summary = songService.create(
            koreanId,
            List.of(new SongNameInput(koreanId, "천본앵", true)),
            List.of(), List.of(producerId, producerId), null, SongStatus.DRAFT);

        assertThat(songCreditRepository.findAll())
            .filteredOn(credit -> credit.getSong().getId().equals(summary.id()))
            .hasSize(1);
    }

    @Test
    @DisplayName("B3 — 원제 언어는 song_language에 자동으로 들어가지 않는다")
    void doesNotAutoAddOriginalLanguageToSongLanguage() {
        SongSummary summary = songService.create(
            koreanId,
            List.of(new SongNameInput(koreanId, "천본앵", true)),
            List.of(japaneseId),
            List.of(), null, SongStatus.DRAFT);

        assertThat(songLanguageRepository.findAll())
            .filteredOn(sl -> sl.getSong().getId().equals(summary.id()))
            .extracting(sl -> sl.getLanguage().getId())
            .containsExactly(japaneseId);
    }

    @Test
    @DisplayName("없는 언어를 원제 언어로 주면 404")
    void rejectsUnknownOriginalLanguage() {
        assertThatThrownBy(() -> songService.create(
            999_999L,
            List.of(new SongNameInput(999_999L, "x", true)),
            List.of(), List.of(), null, SongStatus.DRAFT))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
    }
}

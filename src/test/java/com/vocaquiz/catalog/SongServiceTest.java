package com.vocaquiz.catalog;

import com.vocaquiz.catalog.domain.Producer;
import com.vocaquiz.catalog.domain.Song;
import com.vocaquiz.catalog.domain.SongStatus;
import com.vocaquiz.catalog.domain.Video;
import com.vocaquiz.catalog.domain.VideoKind;
import com.vocaquiz.catalog.repository.LanguageRepository;
import com.vocaquiz.catalog.repository.ProducerRepository;
import com.vocaquiz.catalog.repository.SongAnswerRepository;
import com.vocaquiz.catalog.repository.SongCreditRepository;
import com.vocaquiz.catalog.repository.SongLanguageRepository;
import com.vocaquiz.catalog.repository.SongNameRepository;
import com.vocaquiz.catalog.repository.SongRepository;
import com.vocaquiz.catalog.repository.VideoRepository;
import com.vocaquiz.catalog.service.SongDetail;
import com.vocaquiz.catalog.service.SongNameInput;
import com.vocaquiz.catalog.service.SongService;
import com.vocaquiz.catalog.service.SongSummary;
import com.vocaquiz.catalog.service.UpdateSongNameInput;
import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SongServiceTest {

    @Autowired SongService songService;
    @Autowired SongRepository songRepository;
    @Autowired LanguageRepository languageRepository;
    @Autowired ProducerRepository producerRepository;
    @Autowired SongAnswerRepository songAnswerRepository;
    @Autowired SongCreditRepository songCreditRepository;
    @Autowired SongLanguageRepository songLanguageRepository;
    @Autowired SongNameRepository songNameRepository;
    @Autowired VideoRepository videoRepository;

    private Long koreanId;
    private Long japaneseId;

    @BeforeEach
    void setUp() {
        koreanId = languageRepository.findByCode("KO").orElseThrow().getId();
        japaneseId = languageRepository.findByCode("JA").orElseThrow().getId();
    }

    @Test
    @DisplayName("완료 기준 1 — 이름 없이 곡을 만들면 400 (D-072)")
    void rejectsCreationWithoutAnyName() {
        assertThatThrownBy(() -> songService.create(
            List.of(),
            List.of(), List.of(), null, SongStatus.DRAFT))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("완료 기준 1 — 한 언어에 대표가 둘이면 400")
    void rejectsCreationWithTwoPrimaryNamesInSameLanguage() {
        assertThatThrownBy(() -> songService.create(
            List.of(
                new SongNameInput(koreanId, "천본앵", true),
                new SongNameInput(koreanId, "천본사쿠라", true)),
            List.of(), List.of(), null, SongStatus.DRAFT))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("이름 하나만 있으면 최소 입력으로 만들어진다 (D-072)")
    void createsWithMinimumInput() {
        SongSummary summary = songService.create(
            List.of(new SongNameInput(koreanId, "천본앵", true)),
            List.of(), List.of(), null, SongStatus.DRAFT);

        assertThat(summary.id()).isNotNull();
        assertThat(summary.status()).isEqualTo(SongStatus.DRAFT);
    }

    @Test
    @DisplayName("완료 기준 5 — 저장한 곡의 song_answer가 패턴 전개 결과와 같다")
    void createsSongAnswerFromPattern() {
        SongSummary summary = songService.create(
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
            List.of(new SongNameInput(koreanId, "천본앵", true)),
            List.of(), List.of(producerId, producerId), null, SongStatus.DRAFT);

        assertThat(songCreditRepository.findAll())
            .filteredOn(credit -> credit.getSong().getId().equals(summary.id()))
            .hasSize(1);
    }

    @Test
    @DisplayName("언어를 주지 않으면 song_language는 비어 있다")
    void doesNotAutoAddAnyLanguageToSongLanguage() {
        SongSummary summary = songService.create(
            List.of(new SongNameInput(koreanId, "천본앵", true)),
            List.of(japaneseId),
            List.of(), null, SongStatus.DRAFT);

        assertThat(songLanguageRepository.findAll())
            .filteredOn(sl -> sl.getSong().getId().equals(summary.id()))
            .extracting(sl -> sl.getLanguage().getId())
            .containsExactly(japaneseId);
    }

    @Test
    @DisplayName("없는 언어로 이름을 주면 404")
    void rejectsUnknownLanguageInName() {
        assertThatThrownBy(() -> songService.create(
            List.of(new SongNameInput(999_999L, "x", true)),
            List.of(), List.of(), null, SongStatus.DRAFT))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("D-062 — 조건을 하나도 못 채운 채 PUBLISHED로 만들면 400에 빠진 조건이 담긴다")
    void createRejectsPublishedWhenConditionsMissing() {
        assertThatThrownBy(() -> songService.create(
            List.of(new SongNameInput(koreanId, "천본앵", true)),
            List.of(), List.of(), null, SongStatus.PUBLISHED))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> {
                ApiException apiException = (ApiException) e;
                assertThat(apiException.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
                assertThat(apiException.getMissingConditions())
                    .contains("정답 패턴이 없다", "수집된 원곡(ORIGINAL) 영상이 없다");
            });
    }

    @Test
    @DisplayName("조회는 이름·언어·프로듀서·정답 패턴을 전부 돌려준다")
    void getReturnsFullDetail() {
        Long producerId = producerRepository.save(Producer.create("wowaka")).getId();

        SongSummary summary = songService.create(
            List.of(
                new SongNameInput(koreanId, "천본앵", true),
                new SongNameInput(japaneseId, "千本桜", true)),
            List.of(japaneseId),
            List.of(producerId),
            "(천본앵|천본사쿠라)",
            SongStatus.DRAFT);

        SongDetail detail = songService.get(summary.id());

        assertThat(detail.status()).isEqualTo(SongStatus.DRAFT);
        assertThat(detail.names()).hasSize(2);
        assertThat(detail.languageIds()).containsExactly(japaneseId);
        assertThat(detail.producerIds()).containsExactly(producerId);
        assertThat(detail.answerPattern()).isEqualTo("(천본앵|천본사쿠라)");
    }

    @Test
    @DisplayName("B2 — 수정 시 id 없는 이름은 추가, id 있는 이름은 갱신, 빠진 id는 삭제")
    void updateDiffsNamesByIdAddKeepAndRemove() {
        SongSummary summary = songService.create(
            List.of(
                new SongNameInput(koreanId, "천본앵", true),
                new SongNameInput(japaneseId, "千本桜", true)),
            List.of(), List.of(), null, SongStatus.DRAFT);

        Long keepId = songNameRepository.findBySongId(summary.id()).stream()
            .filter(n -> n.getLanguage().getId().equals(koreanId))
            .findFirst().orElseThrow().getId();

        songService.update(
            summary.id(),
            List.of(
                new UpdateSongNameInput(keepId, koreanId, "천본앵(고침)", true),
                new UpdateSongNameInput(null, koreanId, "센본자쿠라", false)),
            List.of(), List.of(), null, SongStatus.DRAFT);

        assertThat(songNameRepository.findBySongId(summary.id()))
            .extracting(n -> n.getName())
            .containsExactlyInAnyOrder("천본앵(고침)", "센본자쿠라");
    }

    @Test
    @DisplayName("다른 곡의(또는 없는) song_name id로 수정하면 404")
    void updateWithUnknownNameIdIsNotFound() {
        SongSummary summary = songService.create(
            List.of(new SongNameInput(koreanId, "천본앵", true)),
            List.of(), List.of(), null, SongStatus.DRAFT);

        assertThatThrownBy(() -> songService.update(
            summary.id(),
            List.of(new UpdateSongNameInput(999_999L, koreanId, "x", true)),
            List.of(), List.of(), null, SongStatus.DRAFT))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("수정에서 answerPattern을 비우면 기존 패턴과 song_answer를 지운다")
    void updateClearsAnswerPatternWhenBlank() {
        SongSummary summary = songService.create(
            List.of(new SongNameInput(koreanId, "천본앵", true)),
            List.of(), List.of(), "천본앵", SongStatus.DRAFT);
        assertThat(songAnswerRepository.findBySongId(summary.id())).isNotEmpty();
        Long nameId = songService.get(summary.id()).names().get(0).id();

        songService.update(
            summary.id(),
            List.of(new UpdateSongNameInput(nameId, koreanId, "천본앵", true)),
            List.of(), List.of(), "", SongStatus.DRAFT);

        assertThat(songAnswerRepository.findBySongId(summary.id())).isEmpty();
    }

    @Test
    @DisplayName("완료 기준 3 — 수집된 원곡 영상이 없으면 PUBLISHED 전환이 그 조건 하나만 담아 실패한다")
    void rejectsPublishTransitionWithoutOriginalVideo() {
        SongSummary summary = songService.create(
            List.of(new SongNameInput(koreanId, "천본앵", true)),
            List.of(), List.of(), "천본앵", SongStatus.DRAFT);
        Long nameId = songService.get(summary.id()).names().get(0).id();

        assertThatThrownBy(() -> songService.update(
            summary.id(),
            List.of(new UpdateSongNameInput(nameId, koreanId, "천본앵", true)),
            List.of(), List.of(), "천본앵", SongStatus.PUBLISHED))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> assertThat(((ApiException) e).getMissingConditions())
                .containsExactly("수집된 원곡(ORIGINAL) 영상이 없다"));
    }

    @Test
    @DisplayName("조건 3개를 전부 채우면 PUBLISHED로 전환된다")
    void publishesWhenAllConditionsAreMet() {
        SongSummary summary = songService.create(
            List.of(new SongNameInput(koreanId, "천본앵", true)),
            List.of(), List.of(), "천본앵", SongStatus.DRAFT);
        Long nameId = songService.get(summary.id()).names().get(0).id();

        Song song = songRepository.findById(summary.id()).orElseThrow();
        videoRepository.save(Video.create(
            song, null, "sm9", VideoKind.ORIGINAL, 195, Instant.now(), "初音ミクの消失"));

        SongDetail detail = songService.update(
            summary.id(),
            List.of(new UpdateSongNameInput(nameId, koreanId, "천본앵", true)),
            List.of(), List.of(), "천본앵", SongStatus.PUBLISHED);

        assertThat(detail.status()).isEqualTo(SongStatus.PUBLISHED);
    }
}

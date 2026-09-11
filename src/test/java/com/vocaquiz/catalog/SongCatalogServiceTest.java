package com.vocaquiz.catalog;

import com.vocaquiz.catalog.domain.*;
import com.vocaquiz.catalog.repository.*;
import com.vocaquiz.catalog.service.SongCatalogService;
import com.vocaquiz.common.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SongCatalogServiceTest {

    @Autowired SongCatalogService songCatalogService;
    @Autowired LanguageRepository languageRepository;
    @Autowired SongRepository songRepository;
    @Autowired SongAnswerRepository songAnswerRepository;
    @Autowired SongAnswerPatternRepository songAnswerPatternRepository;
    @Autowired SongVocalRepository songVocalRepository;
    @Autowired VocalRepository vocalRepository;
    @Autowired VideoRepository videoRepository;

    private Long songId;

    @BeforeEach
    void setUp() {
        Language korean = languageRepository.findByCode("KO").orElseThrow();
        songId = songRepository.save(Song.create(korean, SongStatus.DRAFT)).getId();
    }

    @Test
    @DisplayName("시드가 언어 셋을 넣는다")
    void seedsLanguages() {
        assertThat(languageRepository.findByCode("KO")).isPresent();
        assertThat(languageRepository.findByCode("EN")).isPresent();
        assertThat(languageRepository.findByCode("JA")).isPresent();
    }

    @Test
    @DisplayName("감사가 생성 시각을 채운다")
    void auditingFillsCreatedAt() {
        Song song = songRepository.findById(songId).orElseThrow();

        assertThat(song.getCreatedAt()).isNotNull();
        assertThat(song.getUpdatedAt()).isNotNull();
        assertThat(song.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("완료 기준 4 — 패턴을 저장하면 후보가 들어가고 중복은 한 행만 남는다")
    void buildsAnswersWithoutDuplicates() {
        songCatalogService.replaceAnswerPattern(songId, """
            (히토|인간|사람)마니아
            HITO Mania
            히토마니아
            """);

        assertThat(normalizedOf(songId))
            .containsExactlyInAnyOrder("히토마니아", "인간마니아", "사람마니아", "hitomania");
    }

    @Test
    @DisplayName("빈 후보는 버린다")
    void dropsEmptyCandidates() {
        songCatalogService.replaceAnswerPattern(songId, "()\n천본앵");

        assertThat(normalizedOf(songId)).containsExactly("천본앵");
    }

    @Test
    @DisplayName("패턴을 고치면 옛 후보가 남지 않는다")
    void replacesInsteadOfAppending() {
        songCatalogService.replaceAnswerPattern(songId, "천본앵");
        songCatalogService.replaceAnswerPattern(songId, "千本桜");

        assertThat(normalizedOf(songId)).containsExactly("千本桜");
        assertThat(songAnswerPatternRepository.findBySongId(songId).orElseThrow().getPattern())
            .isEqualTo("千本桜");
    }

    @Test
    @DisplayName("문법이 틀리면 원문도 저장되지 않는다")
    void keepsPatternAndAnswersTogether() {
        assertThatThrownBy(() -> songCatalogService.replaceAnswerPattern(songId, "(안닫힘"))
            .isInstanceOf(ApiException.class);

        assertThat(songAnswerPatternRepository.findBySongId(songId)).isEmpty();
        assertThat(normalizedOf(songId)).isEmpty();
    }

    @Test
    @DisplayName("완료 기준 5 — ORIGINAL 영상의 보컬이 곡의 보컬이 된다")
    void rebuildsSongVocalsFromOriginal() {
        Long miku = vocalRepository.save(Vocal.create("HATSUNE_MIKU")).getId();
        Long rin = vocalRepository.save(Vocal.create("KAGAMINE_RIN")).getId();
        Long videoId = saveVideo(VideoKind.ORIGINAL, "vid_original");

        songCatalogService.replaceVideoVocals(videoId, List.of(miku, rin));

        assertThat(vocalIdsOf(songId)).containsExactlyInAnyOrder(miku, rin);
    }

    @Test
    @DisplayName("ORIGINAL이 아닌 영상은 곡의 보컬을 바꾸지 않는다")
    void ignoresNonOriginal() {
        Long miku = vocalRepository.save(Vocal.create("HATSUNE_MIKU")).getId();
        Long coverId = saveVideo(VideoKind.COVER, "vid_cover");

        songCatalogService.replaceVideoVocals(coverId, List.of(miku));

        assertThat(vocalIdsOf(songId)).isEmpty();
    }

    @Test
    @DisplayName("보컬을 바꾸면 옛 보컬이 남지 않는다")
    void replacesVocalsInsteadOfAppending() {
        Long miku = vocalRepository.save(Vocal.create("HATSUNE_MIKU")).getId();
        Long rin = vocalRepository.save(Vocal.create("KAGAMINE_RIN")).getId();
        Long videoId = saveVideo(VideoKind.ORIGINAL, "vid_original");

        songCatalogService.replaceVideoVocals(videoId, List.of(miku, rin));
        songCatalogService.replaceVideoVocals(videoId, List.of(rin));

        assertThat(vocalIdsOf(songId)).containsExactly(rin);
    }

    private Long saveVideo(VideoKind kind, String youtubeVideoId) {
        Song song = songRepository.getReferenceById(songId);
        return videoRepository.save(
            Video.create(song, null, youtubeVideoId, kind, 242, Instant.now(), "제목")).getId();
    }

    private List<String> normalizedOf(Long songId) {
        return songAnswerRepository.findBySongId(songId).stream()
            .map(SongAnswer::getNormalized)
            .toList();
    }

    private List<Long> vocalIdsOf(Long songId) {
        return songVocalRepository.findBySongId(songId).stream()
            .map(songVocal -> songVocal.getVocal().getId())
            .toList();
    }
}

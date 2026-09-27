package com.vocaquiz.admin.api;

import com.vocaquiz.admin.api.dto.AdminSongDetailResponse;
import com.vocaquiz.admin.api.dto.AdminSongListItemResponse;
import com.vocaquiz.admin.api.dto.AdminSongResponse;
import com.vocaquiz.admin.api.dto.CreateSongRequest;
import com.vocaquiz.admin.api.dto.UpdateSongRequest;
import com.vocaquiz.catalog.domain.SongStatus;
import com.vocaquiz.catalog.service.NamePreference;
import com.vocaquiz.catalog.service.SongDetail;
import com.vocaquiz.catalog.service.SongNameInput;
import com.vocaquiz.catalog.service.SongService;
import com.vocaquiz.catalog.service.SongSummary;
import com.vocaquiz.catalog.service.UpdateSongNameInput;
import com.vocaquiz.user.service.DisplayPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 곡 만들기·조회·수정·목록 (D-072, D-074, D-062). 리포지토리를 직접 주입받지 않는다 (D-070).
 */
@RestController
@RequestMapping("/api/v1/admin/songs")
@RequiredArgsConstructor
public class AdminSongController {

    private final SongService songService;
    private final DisplayPreferenceService displayPreferenceService;

    @PostMapping
    public ResponseEntity<AdminSongResponse> create(@RequestBody @Valid CreateSongRequest request) {
        SongSummary summary = songService.create(
            request.names().stream()
                .map(n -> new SongNameInput(n.languageId(), n.name(), n.primary()))
                .toList(),
            request.languageIds(),
            request.producerIds(),
            request.answerPattern(),
            request.status());

        return ResponseEntity.status(HttpStatus.CREATED).body(AdminSongResponse.from(summary));
    }

    /**
     * 목록. 검색어({@code q})는 정답 패턴(D-056)에서만 찾는다 — 곡 이름으로는 찾지 않는다
     * (ARCH §5.4). {@code sort}는 {@code LATEST}(최신순, 기본)·{@code OLDEST}(오래된순).
     * 페이지 크기는 50 고정이다.
     */
    @GetMapping
    public PagedModel<AdminSongListItemResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SongStatus status,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication) {
        Sort.Direction direction = "OLDEST".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        NamePreference songNamePreference = displayPreferenceService.songNamePreference(authentication);

        return new PagedModel<>(
            songService.list(q, status, direction, page, songNamePreference)
                .map(AdminSongListItemResponse::from));
    }

    @GetMapping("/{id}")
    public AdminSongDetailResponse get(@PathVariable Long id) {
        return AdminSongDetailResponse.from(songService.get(id));
    }

    @PutMapping("/{id}")
    public AdminSongDetailResponse update(@PathVariable Long id, @RequestBody @Valid UpdateSongRequest request) {
        SongDetail detail = songService.update(
            id,
            request.names().stream()
                .map(n -> new UpdateSongNameInput(n.id(), n.languageId(), n.name(), n.primary()))
                .toList(),
            request.languageIds(),
            request.producerIds(),
            request.answerPattern(),
            request.status());

        return AdminSongDetailResponse.from(detail);
    }
}

package com.vocaquiz.admin.api;

import com.vocaquiz.admin.api.dto.AdminSongResponse;
import com.vocaquiz.admin.api.dto.CreateSongRequest;
import com.vocaquiz.catalog.service.SongNameInput;
import com.vocaquiz.catalog.service.SongService;
import com.vocaquiz.catalog.service.SongSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 곡 만들기 (D-061). 리포지토리를 직접 주입받지 않는다 (D-070).
 */
@RestController
@RequestMapping("/api/v1/admin/songs")
@RequiredArgsConstructor
public class AdminSongController {

    private final SongService songService;

    @PostMapping
    public ResponseEntity<AdminSongResponse> create(@RequestBody @Valid CreateSongRequest request) {
        SongSummary summary = songService.create(
            request.originalLanguageId(),
            request.names().stream()
                .map(n -> new SongNameInput(n.languageId(), n.name(), n.primary()))
                .toList(),
            request.languageIds(),
            request.producerIds(),
            request.answerPattern(),
            request.status());

        return ResponseEntity.status(HttpStatus.CREATED).body(AdminSongResponse.from(summary));
    }
}

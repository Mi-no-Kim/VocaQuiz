package com.vocaquiz.admin.api;

import com.vocaquiz.admin.api.dto.AdminProducerResponse;
import com.vocaquiz.admin.api.dto.CreateProducerRequest;
import com.vocaquiz.catalog.service.NamePreference;
import com.vocaquiz.catalog.service.ProducerNameInput;
import com.vocaquiz.catalog.service.ProducerService;
import com.vocaquiz.user.service.DisplayPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 프로듀서 자동완성·생성 (D-063, D-073, D-074).
 *
 * <p>리포지토리를 직접 주입받지 않는다 (D-070). 중복 검사도 {@link ProducerService}가 한다.
 */
@RestController
@RequestMapping("/api/v1/admin/producers")
@RequiredArgsConstructor
public class AdminProducerController {

    private final ProducerService producerService;
    private final DisplayPreferenceService displayPreferenceService;

    @GetMapping
    public List<AdminProducerResponse> search(@RequestParam(required = false) String q, Authentication authentication) {
        NamePreference preference = displayPreferenceService.producerNamePreference(authentication);
        return producerService.search(q, preference).stream()
            .map(AdminProducerResponse::from)
            .toList();
    }

    @PostMapping
    public ResponseEntity<AdminProducerResponse> create(
            @RequestBody @Valid CreateProducerRequest request, Authentication authentication) {
        NamePreference preference = displayPreferenceService.producerNamePreference(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AdminProducerResponse.from(producerService.create(
                request.names().stream()
                    .map(n -> new ProducerNameInput(n.languageId(), n.name(), n.primary()))
                    .toList(),
                preference)));
    }
}

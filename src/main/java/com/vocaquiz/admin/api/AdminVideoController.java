package com.vocaquiz.admin.api;

import com.vocaquiz.admin.api.dto.AddVideoRequest;
import com.vocaquiz.admin.api.dto.AdminVideoResponse;
import com.vocaquiz.admin.api.dto.ExcludeVideoRequest;
import com.vocaquiz.catalog.domain.Video;
import com.vocaquiz.catalog.domain.VideoCollectionStatus;
import com.vocaquiz.catalog.repository.VideoRepository;
import com.vocaquiz.catalog.service.VideoIngestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 영상 추가·수집 (D-059·D-060·D-066·D-069). */
@RestController
@RequestMapping("/api/v1/admin/videos")
@RequiredArgsConstructor
public class AdminVideoController {

    private final VideoIngestService videoIngestService;
    private final VideoRepository videoRepository;

    @PostMapping
    public ResponseEntity<AdminVideoResponse> add(@RequestBody @Valid AddVideoRequest request) {
        Video video = videoIngestService.addVideo(request.videoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminVideoResponse.from(video));
    }

    /** song이 없는 영상만 준다 — 곡에 붙은 순간부터는 영상 편집(P1-3-6) 화면의 몫이다. */
    @GetMapping
    public List<AdminVideoResponse> list(@RequestParam(required = false) VideoCollectionStatus status) {
        List<Video> videos = status == null
            ? videoRepository.findBySongIsNullOrderByIdDesc()
            : videoRepository.findBySongIsNullAndCollectionStatusOrderByIdDesc(status);
        return videos.stream().map(AdminVideoResponse::from).toList();
    }

    /**
     * 대기 중인 미수집 영상을 즉시 수집한다(동기). 개수 제한 없이 전부 대상이고,
     * videos.list 호출과 저장만 50개 묶음으로 끊어 돈다 (D-069).
     *
     * <p>이미 도는 중이면 409 (D-069).
     */
    @PostMapping("/fetch")
    public ResponseEntity<Void> fetch() {
        videoIngestService.collectPendingManually();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/requeue")
    public ResponseEntity<Void> requeue(@PathVariable Long id) {
        videoIngestService.requeue(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/exclude")
    public ResponseEntity<Void> exclude(@PathVariable Long id, @RequestBody @Valid ExcludeVideoRequest request) {
        videoIngestService.exclude(id, request.reason());
        return ResponseEntity.ok().build();
    }

    /** EXCLUDED 취소. */
    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id) {
        videoIngestService.restore(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        videoIngestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

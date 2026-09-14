package com.vocaquiz.admin.api;

import com.vocaquiz.admin.api.dto.AddVideoRequest;
import com.vocaquiz.admin.api.dto.VideoResponse;
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

/** 영상 추가·수집 (P1-3-3, D-059·D-060). */
@RestController
@RequestMapping("/api/v1/admin/videos")
@RequiredArgsConstructor
public class AdminVideoController {

    private final VideoIngestService videoIngestService;
    private final VideoRepository videoRepository;

    @PostMapping
    public ResponseEntity<VideoResponse> add(@RequestBody @Valid AddVideoRequest request) {
        Video video = videoIngestService.addVideo(request.videoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(VideoResponse.from(video));
    }

    /** song이 없는 영상만 준다 — 곡에 붙은 순간부터는 영상 편집(P1-3-6) 화면의 몫이다. */
    @GetMapping
    public List<VideoResponse> list(@RequestParam(required = false) VideoCollectionStatus status) {
        List<Video> videos = status == null
            ? videoRepository.findBySongIsNullOrderByIdDesc()
            : videoRepository.findBySongIsNullAndCollectionStatusOrderByIdDesc(status);
        return videos.stream().map(VideoResponse::from).toList();
    }

    /** 미수집 최대 50개를 즉시 수집한다(동기). 이미 도는 중이면 409(D-060 결정 3). */
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
    public ResponseEntity<Void> exclude(@PathVariable Long id) {
        videoIngestService.exclude(id);
        return ResponseEntity.ok().build();
    }

    /** 출제 목록에서 뺀다 (D-010). */
    @PostMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable Long id) {
        videoIngestService.disablePlayable(id);
        return ResponseEntity.ok().build();
    }

    /** 다시 출제 대상으로 켠다. */
    @PostMapping("/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable Long id) {
        videoIngestService.enablePlayable(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        videoIngestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

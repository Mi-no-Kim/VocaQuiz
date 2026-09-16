package com.vocaquiz.catalog.service;

/**
 * 곡 수정에서 이름 한 줄의 입력 (D-061, B2). {@code id}가 없으면 신규 행, 있으면 그 행을
 * 고친다 — 요청에 없는 기존 id는 서비스가 삭제로 판단한다.
 */
public record UpdateSongNameInput(Long id, Long languageId, String name, boolean primary) {
}

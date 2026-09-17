package com.vocaquiz.catalog.service;

/** 프로듀서 생성에서 이름 한 줄의 입력 (D-073). 화면 위젯 모양이 아니라 서비스가 필요로 하는 최소 형태다. */
public record ProducerNameInput(Long languageId, String name, boolean primary) {
}

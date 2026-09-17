package com.vocaquiz.admin.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 프로듀서 만들기 요청. 이름 1개 이상이 최소 입력이다 (D-073). */
public record CreateProducerRequest(
    @NotEmpty List<@Valid CreateProducerNameRequest> names
) {
}

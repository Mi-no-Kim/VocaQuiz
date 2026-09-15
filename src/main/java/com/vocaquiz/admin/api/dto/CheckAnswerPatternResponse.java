package com.vocaquiz.admin.api.dto;

import java.util.List;

/** 정답 패턴을 저장하기 전에 미리 펼쳐 본 결과 (D-052). */
public record CheckAnswerPatternResponse(
    List<String> results,
    int count,
    boolean warning
) {

    private static final int WARNING_THRESHOLD = 20;

    public static CheckAnswerPatternResponse of(List<String> results) {
        return new CheckAnswerPatternResponse(results, results.size(), results.size() > WARNING_THRESHOLD);
    }
}

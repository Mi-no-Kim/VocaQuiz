package com.vocaquiz.admin.api;

import com.vocaquiz.admin.api.dto.CheckAnswerPatternRequest;
import com.vocaquiz.admin.api.dto.CheckAnswerPatternResponse;
import com.vocaquiz.catalog.service.AnswerPatternExpander;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 정답 패턴 원문을 저장하기 전에 미리 펼쳐 본다 (D-052, D-056).
 *
 * <p>{@link AnswerPatternExpander}는 엔티티도 리포지토리도 몰라 D-070이 막는 대상이 아니다 —
 * 곡과 무관하게 원문 문자열만으로 결과가 정해진다. 그래서 서비스 계층 없이 바로 부른다.
 * 문법 오류는 {@link com.vocaquiz.common.error.ApiException}으로 던져져
 * {@link com.vocaquiz.common.error.GlobalExceptionHandler}가 처리한다.
 */
@RestController
@RequestMapping("/api/v1/admin/answer-patterns")
public class AdminAnswerPatternController {

    @PostMapping("/check")
    public CheckAnswerPatternResponse check(@RequestBody CheckAnswerPatternRequest request) {
        return CheckAnswerPatternResponse.of(AnswerPatternExpander.expand(request.pattern()));
    }
}

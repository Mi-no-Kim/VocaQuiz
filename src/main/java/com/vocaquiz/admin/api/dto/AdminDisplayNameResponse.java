package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.service.ResolvedName;

/**
 * main/sub 표기법으로 정한 표시값 (D-074). {@code secondary}는 main·sub 값이 둘 다 있고
 * 서로 다를 때만 채워진다 — 화면은 있으면 둘 다 보여주되 "메인"·"서브"라는 말은 쓰지 않고
 * 굵기·색·크기로만 구분한다.
 */
public record AdminDisplayNameResponse(String primary, String secondary) {

    public static AdminDisplayNameResponse from(ResolvedName resolvedName) {
        return new AdminDisplayNameResponse(resolvedName.primary(), resolvedName.secondary());
    }
}

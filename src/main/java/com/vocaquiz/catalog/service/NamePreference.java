package com.vocaquiz.catalog.service;

/**
 * 이름을 보여줄 때 우선할 언어 (D-074) — 곡명·작곡가명 각각 따로 갖는다.
 *
 * @param mainLanguageId 필수. 이 언어의 값이 있으면 우선 보여준다.
 * @param subLanguageId 있으면(null 아니면) main과 함께·대신 보여줄 보조 언어.
 */
public record NamePreference(Long mainLanguageId, Long subLanguageId) {
}

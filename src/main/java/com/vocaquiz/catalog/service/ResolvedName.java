package com.vocaquiz.catalog.service;

/**
 * {@link NameDisplayResolver}가 정한 표시값 (D-074).
 *
 * @param primary 항상 있다.
 * @param secondary main·sub 값이 둘 다 있고 서로 다를 때만 있다. 그 외엔 {@code null}.
 */
public record ResolvedName(String primary, String secondary) {
}

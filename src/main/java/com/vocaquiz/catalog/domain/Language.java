package com.vocaquiz.catalog.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "language",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_language_code",
        columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Language {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, nullable = false)
    private String code;

    @Column(length = 50, nullable = false)
    private String name;

    /** 표시 폴백 순서 — 이 값 오름차순으로 훑어 이름이 있는 첫 언어를 보여준다. EN이 0이다 (D-071). */
    @Column(nullable = false)
    private int displayOrder;

    public static Language create(String code, String name, int displayOrder) {
        Language language = new Language();
        language.code = code;
        language.name = name;
        language.displayOrder = displayOrder;
        return language;
    }
}

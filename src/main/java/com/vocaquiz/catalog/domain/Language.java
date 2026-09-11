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

    public static Language create(String code, String name) {
        Language language = new Language();
        language.code = code;
        language.name = name;

        return language;
    }
}

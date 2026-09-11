package com.vocaquiz.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vocal_name",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_vocal_name",
        columnNames = {"vocal_id", "language_id", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VocalName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocal_id", nullable = false)
    private Vocal vocal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean isPrimary;

    public static VocalName create(Vocal vocal, Language language, String name, boolean isPrimary) {
        VocalName vocalName = new VocalName();
        vocalName.vocal = vocal;
        vocalName.language = language;
        vocalName.name = name;
        vocalName.isPrimary = isPrimary;

        return vocalName;
    }
}

package com.vocaquiz.catalog.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "vocal",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_vocal_code",
        columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(nullable = false)
    private Instant createdAt;

    public static Vocal create(String code) {
        Vocal vocal = new Vocal();
        vocal.code = code;
        vocal.createdAt = Instant.now();

        return vocal;
    }
}

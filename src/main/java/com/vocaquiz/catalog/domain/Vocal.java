package com.vocaquiz.catalog.domain;

import com.vocaquiz.common.domain.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vocal",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_vocal_code",
        columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vocal extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String code;

    public static Vocal create(String code) {
        Vocal vocal = new Vocal();
        vocal.code = code;

        return vocal;
    }
}

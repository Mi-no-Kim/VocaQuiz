package com.vocaquiz.catalog.domain;

import com.vocaquiz.common.domain.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "producer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Producer extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    public static Producer create(String name) {
        Producer producer = new Producer();
        producer.name = name;

        return producer;
    }
}

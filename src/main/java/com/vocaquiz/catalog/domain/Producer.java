package com.vocaquiz.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "producer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Producer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(nullable = false)
    private Instant createdAt;

    public static Producer create(String name) {
        Producer producer = new Producer();
        producer.name = name;
        producer.createdAt = Instant.now();

        return producer;
    }
}

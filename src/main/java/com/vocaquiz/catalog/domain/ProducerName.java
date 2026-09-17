package com.vocaquiz.catalog.domain;

import com.vocaquiz.common.domain.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "producer_name",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_producer_name",
        columnNames = {"language_id", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProducerName extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producer_id", nullable = false)
    private Producer producer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean isPrimary;

    public static ProducerName create(Producer producer, Language language, String name, boolean isPrimary) {
        ProducerName producerName = new ProducerName();
        producerName.producer = producer;
        producerName.language = language;
        producerName.name = name;
        producerName.isPrimary = isPrimary;

        return producerName;
    }
}

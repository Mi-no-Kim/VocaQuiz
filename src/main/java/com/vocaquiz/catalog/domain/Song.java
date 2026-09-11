package com.vocaquiz.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "song")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_language_id", nullable = false)
    private Language originalLanguage;

    @Column(length = 20, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private SongStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static Song create(Language originalLanguage, SongStatus status) {
        Song song = new Song();
        song.originalLanguage = originalLanguage;
        song.status = status;
        song.update();
        song.createdAt = song.updatedAt;

        return song;
    }

    @PreUpdate
    public void update() {
        this.updatedAt = Instant.now();
    }
}

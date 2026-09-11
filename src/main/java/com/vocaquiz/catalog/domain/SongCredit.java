package com.vocaquiz.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "song_credit",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_song_credit",
        columnNames = {"song_id", "producer_id", "role"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producer_id", nullable = false)
    private Producer producer;

    /** UNIQUE에 role이 들어간 이유는 한 사람이 한 곡에서 두 역할을 맡을 수 있어서다 (D-053). */
    @Column(length = 32, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private CreditRole role;

    public static SongCredit create(Song song, Producer producer, CreditRole role) {
        SongCredit songCredit = new SongCredit();
        songCredit.song = song;
        songCredit.producer = producer;
        songCredit.role = role;

        return songCredit;
    }
}

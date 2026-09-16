package com.vocaquiz.catalog.domain;

import com.vocaquiz.common.domain.TimestampedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "song")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Song extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private SongStatus status;

    public static Song create(SongStatus status) {
        Song song = new Song();
        song.status = status;

        return song;
    }

    /** 곡 수정(PUT)에서 상태를 바꾼다. PUBLISHED 조건 검사는 서비스 쪽 책임이다 (D-062). */
    public void changeStatus(SongStatus status) {
        this.status = status;
    }
}

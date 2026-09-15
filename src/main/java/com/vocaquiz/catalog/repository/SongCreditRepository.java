package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.SongCredit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongCreditRepository extends JpaRepository<SongCredit, Long> {
}

package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.Vocal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocalRepository extends JpaRepository<Vocal, Long> {
}

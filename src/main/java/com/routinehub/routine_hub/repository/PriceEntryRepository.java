package com.routinehub.routine_hub.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.routinehub.routine_hub.model.PriceEntry;

public interface  PriceEntryRepository extends JpaRepository<PriceEntry, Long>{
    Optional<PriceEntry> findTopByProductCodeOrderByScrapedAtDesc(String productCode);
    Optional<PriceEntry> findTopByProductCodeOrderByPriceAsc(String productCode);
    List<PriceEntry> findAllByProductCodeAndScrapedAtBetween(String productCode, LocalDateTime from, LocalDateTime to);
    List<PriceEntry> findAllByProductCodeOrderByScrapedAtAsc(String code);
}

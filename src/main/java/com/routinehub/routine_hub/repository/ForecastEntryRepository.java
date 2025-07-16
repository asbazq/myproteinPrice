package com.routinehub.routine_hub.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.routinehub.routine_hub.model.ForecastEntry;

public interface ForecastEntryRepository extends JpaRepository<ForecastEntry, Long>{

}
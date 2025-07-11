package com.routinehub.routine_hub.dto;

import java.time.LocalDate;
import java.util.List;

public record BestTimeDto(
    String productCode,
    LocalDate minPriceDate,
    int minPrice,
    int bestWeekOfMonth,
    List<AverageWeekDto> weeklyAverages
  ) {}


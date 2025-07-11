package com.routinehub.routine_hub.dto;

import java.time.LocalDateTime;

public record PriceHistoryDto(
    LocalDateTime scrapedAt,
    int price,
    int originPrice,
    int discountRate
  ) {}

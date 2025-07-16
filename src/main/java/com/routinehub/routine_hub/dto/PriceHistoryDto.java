package com.routinehub.routine_hub.dto;

import java.time.OffsetDateTime;

public record PriceHistoryDto(
    OffsetDateTime scrapedAt,
    int price,
    int originPrice,
    int discountRate
  ) {}

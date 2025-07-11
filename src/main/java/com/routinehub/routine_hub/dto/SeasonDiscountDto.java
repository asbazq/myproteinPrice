package com.routinehub.routine_hub.dto;

  public record SeasonDiscountDto(
    String season,
    long averageDiscountRate
  ) {}
package com.routinehub.routine_hub.dto;

  public record AverageWeekDto(
    int weekOfMonth,
    double averagePrice
  ) {}
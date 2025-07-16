package com.routinehub.routine_hub.dto;

import java.time.OffsetDateTime;

public record ForecastPoint(
    OffsetDateTime date,        // 예측 날짜
    double predictedPrice, // 예측 가격
    double dropProb        // 하락 확률
) {}

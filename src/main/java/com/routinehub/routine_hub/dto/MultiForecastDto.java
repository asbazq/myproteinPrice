package com.routinehub.routine_hub.dto;

import java.util.List;

public record MultiForecastDto(
    String code,                   // 상품 코드
    List<ForecastPoint> points    // 예측 리스트
) {}
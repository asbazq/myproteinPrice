package com.routinehub.routine_hub.dto;

/**
 * ARMA 모델 한 스텝 예측 결과 DTO
 */
public record ArmaForecastDto(
    double nextPrice,       // 예측된 다음 시점 가격
    double stdError,        // 예측 오차의 표준편차
    double dropProbability  // 다음 가격이 현재보다 낮을 확률 (0.0 ~ 1.0)
) {}

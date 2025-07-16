package com.routinehub.routine_hub.controller;

import com.routinehub.routine_hub.dto.ArmaForecastDto;
import com.routinehub.routine_hub.dto.ForecastPoint;
import com.routinehub.routine_hub.dto.MultiForecastDto;
import com.routinehub.routine_hub.service.PriceForecastService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/price/myprotein")
@RequiredArgsConstructor
public class PriceForecastController {
    private final PriceForecastService forecastService;

    @GetMapping("/arima-forecast")
    public ResponseEntity<ArmaForecastDto> getArimaForecast(@RequestParam("code") String code) {
        return ResponseEntity.ok(forecastService.forecastNext(code));
    }

    // 다음 달 중 최저가 예측
    @GetMapping("/forecast/cheapest")
    public ResponseEntity<ForecastPoint> cheapest(@RequestParam("code") String code) {
        return ResponseEntity.ok(forecastService.forecastCheapestNextMonth(code));
    }

    @GetMapping("/forecast/offset")
    public ResponseEntity<ForecastPoint> forecastOffset(@RequestParam("code") String code, @RequestParam("offset") int offset) {
        return ResponseEntity.ok(forecastService.forecastOffset(code, offset));
    }
    
    @GetMapping("/forecast/multi")
    public ResponseEntity<MultiForecastDto> forecastMulti(@RequestParam("code") String code, @RequestParam("horizonDays") int horizonDays) {
        return ResponseEntity.ok(forecastService.forecastMulti(code, horizonDays));
    }
}

package com.routinehub.routine_hub.controller;

import com.routinehub.routine_hub.dto.ArmaForecastDto;
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
}

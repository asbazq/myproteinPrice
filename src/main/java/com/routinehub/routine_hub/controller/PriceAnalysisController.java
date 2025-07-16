package com.routinehub.routine_hub.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.routinehub.routine_hub.dto.AverageWeekDto;
import com.routinehub.routine_hub.service.PriceAnalysisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/price/myprotein")
@RequiredArgsConstructor
public class PriceAnalysisController {
  private final PriceAnalysisService priceAnalysisService;

  @GetMapping("/average-lowest-week")
  public ResponseEntity<AverageWeekDto> getAverageLowestWeek(@RequestParam("code") String code) {
    return ResponseEntity.ok(priceAnalysisService.getAverageLowestWeek(code));
  }


}

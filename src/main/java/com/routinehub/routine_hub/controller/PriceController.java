package com.routinehub.routine_hub.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.routinehub.routine_hub.dto.BestTimeDto;
import com.routinehub.routine_hub.dto.PriceHistoryDto;
import com.routinehub.routine_hub.service.PriceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/price/myprotein")
@RequiredArgsConstructor
public class PriceController {
    private final PriceService priceService;

    @GetMapping("/best-time")
    public ResponseEntity<BestTimeDto> bestTime(@RequestParam("code") String code) {
        
      return priceService.bestTime(code)
          .map(ResponseEntity::ok)          // 데이터가 있으면 200 OK
          .orElseGet(() -> ResponseEntity   // 없으면 204 No Content
                          .noContent()
                          .build());
    }

  @GetMapping("/history")
  public List<PriceHistoryDto> getHistoryByPeriod(
      @RequestParam("code") String code,
      @RequestParam("period") String period) {
    return priceService.getHistoryByPeriod(code, period);
  }
}

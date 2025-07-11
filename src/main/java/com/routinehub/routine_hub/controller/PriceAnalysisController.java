package com.routinehub.routine_hub.controller;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.routinehub.routine_hub.dto.AverageWeekDto;
import com.routinehub.routine_hub.model.PriceEntry;
import com.routinehub.routine_hub.repository.PriceEntryRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/price/myprotein")
@RequiredArgsConstructor
public class PriceAnalysisController {
  private final PriceEntryRepository repo;

  /**
   * GET /api/price/myprotein/average-lowest-week
   * params:
   *   code : 제품 코드
   */
  @GetMapping("/average-lowest-week")
  public ResponseEntity<AverageWeekDto> getAverageLowestWeek(@RequestParam("code") String code) {
    List<PriceEntry> list = repo.findAllByProductCodeAndScrapedAtBetween(
        code,
        LocalDateTime.now().minusYears(1),
        LocalDateTime.now()
    );

    // 주차별 평균 가격 계산
    Map<Integer, Double> avgByWeek = list.stream()
      .collect(Collectors.groupingBy(
        e -> (int) WeekFields.of(DayOfWeek.MONDAY, 1)
                             .weekOfMonth()
                             .getFrom(e.getScrapedAt()),
        Collectors.averagingInt(PriceEntry::getPrice)
      ));

    // 평균 최저가인 weekOfMonth 찾기
    int bestWeek = avgByWeek.entrySet().stream()
      .min(Comparator.comparingDouble(Map.Entry::getValue))
      .map(Map.Entry::getKey)
      .orElse(1);

    return ResponseEntity.ok(
      new AverageWeekDto(bestWeek, avgByWeek.get(bestWeek))
    );
  }


}

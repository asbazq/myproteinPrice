package com.routinehub.routine_hub.controller;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.routinehub.routine_hub.dto.AverageWeekDto;
import com.routinehub.routine_hub.dto.BestTimeDto;
import com.routinehub.routine_hub.dto.PriceHistoryDto;
import com.routinehub.routine_hub.model.PriceEntry;
import com.routinehub.routine_hub.repository.PriceEntryRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/price/myprotein")
@RequiredArgsConstructor
public class PriceController {
    private final PriceEntryRepository repo;

    @GetMapping("/best-time")
    public ResponseEntity<BestTimeDto> bestTime(@RequestParam("code") String code) {
        // 1) 최저가 엔트리 조회
        Optional<PriceEntry> minOpt = repo.findTopByProductCodeOrderByPriceAsc(code);
        if (minOpt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        PriceEntry minEntry = minOpt.get();

        // 2) 지난 3개월 치 데이터 가져오기
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeMonthsAgo = now.minusMonths(3);

        List<PriceEntry> recent = repo.findAllByProductCodeAndScrapedAtBetween(
            code, threeMonthsAgo, now
        );

        // 3) 주차별 평균 계산
        WeekFields wf = WeekFields.of(DayOfWeek.MONDAY, 1);
        Map<Integer, Double> avgByWeek = recent.stream()
            .collect(Collectors.groupingBy(
                e -> (int) wf.weekOfMonth().getFrom(e.getScrapedAt()),
                Collectors.averagingInt(PriceEntry::getPrice)
            ));

        // 4) DTO 리스트로 변환 & 최저 평균 주차 찾기
        List<AverageWeekDto> weeklyAverages = avgByWeek.entrySet().stream()
            .map(e -> new AverageWeekDto(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingInt(AverageWeekDto::weekOfMonth))
            .toList();

        int bestWeek = avgByWeek.entrySet().stream()
            .min(Comparator.comparingDouble(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(1);

        // 5) 응답 생성
        BestTimeDto dto = new BestTimeDto(
            code,
            minEntry.getScrapedAt().toLocalDate(),
            minEntry.getPrice(),
            bestWeek,
            weeklyAverages
        );
        return ResponseEntity.ok(dto);
    }

  @GetMapping("/history")
  public List<PriceHistoryDto> getHistoryByPeriod(
      @RequestParam("code") String code,
      @RequestParam("period") String period) {

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime from;
    switch(period) {
      case "daily":
        from = now.minusDays(1);
        break;
      case "weekly":
        from = now.minusWeeks(1);
        break;
      case "monthly":
        from = now.minusMonths(1);
        break;
      case "yearly":
        from = now.minusYears(1);
        break;
      default:
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "period must be weekly/monthly/yearly");
    }

    return repo.findAllByProductCodeAndScrapedAtBetween(code, from, now)
               .stream()
               .map(e -> new PriceHistoryDto(e.getScrapedAt(), e.getPrice(), e.getOriginPrice(), e.getDiscountRate()))
               .toList();
  }
}

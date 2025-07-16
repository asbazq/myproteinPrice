package com.routinehub.routine_hub.service;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.routinehub.routine_hub.dto.AverageWeekDto;
import com.routinehub.routine_hub.dto.BestTimeDto;
import com.routinehub.routine_hub.dto.PriceHistoryDto;
import com.routinehub.routine_hub.model.PriceEntry;
import com.routinehub.routine_hub.repository.PriceEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PriceService {
    private final PriceEntryRepository priceEntryRepository;

    public Optional<BestTimeDto> bestTime(String code) {
        // 1) 최저가 엔트리 조회
        Optional<PriceEntry> minOpt = priceEntryRepository.findTopByProductCodeOrderByPriceAsc(code);
        if (minOpt.isEmpty()) {
            return Optional.empty();
        }
        PriceEntry minEntry = minOpt.get();

        // 2) 지난 3개월 치 데이터 가져오기
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime threeMonthsAgo = now.minusMonths(3);

        List<PriceEntry> recent = priceEntryRepository.findAllByProductCodeAndScrapedAtBetween(
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
        return Optional.of(dto);
    }

  public List<PriceHistoryDto> getHistoryByPeriod(String code, String period) {
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime from;
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

    return priceEntryRepository.findAllByProductCodeAndScrapedAtBetween(code, from, now)
               .stream()
               .map(e -> new PriceHistoryDto(e.getScrapedAt(), e.getPrice(), e.getOriginPrice(), e.getDiscountRate()))
               .toList();
    }

}

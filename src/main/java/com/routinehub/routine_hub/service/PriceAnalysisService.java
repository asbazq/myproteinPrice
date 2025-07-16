package com.routinehub.routine_hub.service;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.routinehub.routine_hub.dto.AverageWeekDto;
import com.routinehub.routine_hub.model.PriceEntry;
import com.routinehub.routine_hub.repository.PriceEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PriceAnalysisService {
    private final PriceEntryRepository priceEntryRepository;

    public AverageWeekDto getAverageLowestWeek(@RequestParam("code") String code) {
    List<PriceEntry> list = priceEntryRepository.findAllByProductCodeAndScrapedAtBetween(
        code,
        OffsetDateTime.now().minusYears(1),
        OffsetDateTime.now()
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

    return new AverageWeekDto(bestWeek, avgByWeek.get(bestWeek));
  }
}

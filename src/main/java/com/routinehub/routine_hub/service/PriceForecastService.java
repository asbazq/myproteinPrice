package com.routinehub.routine_hub.service;

import com.routinehub.routine_hub.dto.ArmaForecastDto;
import com.routinehub.routine_hub.dto.ForecastPoint;
import com.routinehub.routine_hub.dto.MultiForecastDto;
import com.routinehub.routine_hub.model.PriceEntry;
import com.routinehub.routine_hub.repository.PriceEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceForecastService {
    private final PriceEntryRepository repo;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 휴일 범위 정의
    private static final Map<String, LocalDate[]> HOLIDAYS = Map.ofEntries(
        Map.entry("설날",           new LocalDate[]{LocalDate.of(2025,1,20), LocalDate.of(2025,1,25)}),
        Map.entry("추석",           new LocalDate[]{LocalDate.of(2025,9,15), LocalDate.of(2025,9,21)}),
        Map.entry("블랙프라이데이", new LocalDate[]{LocalDate.of(2025,11,24), LocalDate.of(2025,11,30)}),
        Map.entry("1월1일",         new LocalDate[]{LocalDate.of(2025,1,1),  LocalDate.of(2025,1,1)}),
        Map.entry("2월2일",         new LocalDate[]{LocalDate.of(2025,2,2),  LocalDate.of(2025,2,2)}),
        Map.entry("3월3일",         new LocalDate[]{LocalDate.of(2025,3,3),  LocalDate.of(2025,3,3)}),
        Map.entry("4월4일",         new LocalDate[]{LocalDate.of(2025,4,4),  LocalDate.of(2025,4,4)}),
        Map.entry("5월5일",         new LocalDate[]{LocalDate.of(2025,5,5),  LocalDate.of(2025,5,5)}),
        Map.entry("6월6일",         new LocalDate[]{LocalDate.of(2025,6,6),  LocalDate.of(2025,6,6)}),
        Map.entry("7월7일",         new LocalDate[]{LocalDate.of(2025,7,7),  LocalDate.of(2025,7,7)}),
        Map.entry("8월8일",         new LocalDate[]{LocalDate.of(2025,8,8),  LocalDate.of(2025,8,8)}),
        Map.entry("9월9일",         new LocalDate[]{LocalDate.of(2025,9,9),  LocalDate.of(2025,9,9)}),
        Map.entry("10월10일",      new LocalDate[]{LocalDate.of(2025,10,10), LocalDate.of(2025,10,10)}),
        Map.entry("11월11일",      new LocalDate[]{LocalDate.of(2025,11,11), LocalDate.of(2025,11,11)}),
        Map.entry("12월12일",      new LocalDate[]{LocalDate.of(2025,12,12), LocalDate.of(2025,12,12)})
    );

    /** 단일 시점 예측 (ARX(1)) */
    public ArmaForecastDto forecastNext(String code) {
        ForecastPoint point = forecastOnOffsets(code, Collections.singletonList(1)).get(0);
        return new ArmaForecastDto(
            point.predictedPrice(),
            0.0,
            point.dropProb()
        );
    }

    /** 다중 시점 예측 (1일부터 horizonDays일까지 연속 예측) */
    public MultiForecastDto forecastMulti(String code, int horizonDays) {
        List<ForecastPoint> points = forecastOnOffsets(code,
            IntStream.rangeClosed(1, horizonDays).boxed().collect(Collectors.toList()));
        return new MultiForecastDto(code, points);
    }

    /** 단일 offset 예측 */
    public ForecastPoint forecastOffset(String code, int offset) {
        return forecastOnOffsets(code, Collections.singletonList(offset)).get(0);
    }

    /**
     * 특정 일수(offsets)에 해당하는 미래 시점 예측
     * offsets: 1이면 내일, 30이면 30일 뒤
     */
    public List<ForecastPoint> forecastOnOffsets(String code, List<Integer> offsets) {
        List<PriceEntry> hist = repo.findAllByProductCodeOrderByScrapedAtAsc(code);
        int n = hist.size();
        if (n < 2) {
            // 오늘 오전 7시 기준 OffsetDateTime
            OffsetDateTime now = ZonedDateTime.now(KST)
                .withHour(7).withMinute(0).withSecond(0).withNano(0)
                .toOffsetDateTime();
            return offsets.stream()
                .map(i -> new ForecastPoint(
                    now.plusDays(i),
                    n == 1 ? hist.get(0).getPrice() : 0.0,
                    0.5
                ))
                .collect(Collectors.toList());
        }

        // OLS 파라미터 계산 (ARX(1))
         double[] prices = hist.stream().mapToDouble(PriceEntry::getPrice).toArray();
        OffsetDateTime[] dateTimes = hist.stream()
            .map(PriceEntry::getScrapedAt)
            .toArray(OffsetDateTime[]::new);
        LocalDate[] dates = Arrays.stream(dateTimes).map(OffsetDateTime::toLocalDate)
                                  .toArray(LocalDate[]::new);
        int m = n - 1;
        double[] y = new double[m];
        double[][] X = new double[m][2];
        for (int i = 1; i < n; i++) {
            y[i-1]    = prices[i];
            X[i-1][0] = prices[i-1];
            LocalDate d = dates[i];
            boolean hol = HOLIDAYS.values().stream().anyMatch(r ->
                !d.isBefore(r[0].minusDays(1)) && !d.isAfter(r[1])
            );
            X[i-1][1] = hol ? 1.0 : 0.0;
        }
        OLSMultipleLinearRegression reg = new OLSMultipleLinearRegression();
        reg.newSampleData(y, X);
        double[] params   = reg.estimateRegressionParameters();
        double intercept  = params[0];
        double phi        = params[1];
        double gamma      = params[2];
        double sigma      = Math.sqrt(reg.calculateResidualSumOfSquares() / (m - params.length));

        // 재귀 예측: 최대 offset 까지 갱신
        int maxOffset = offsets.stream().max(Integer::compareTo).orElse(0);
        List<ForecastPoint> all = new ArrayList<>();
        // 마지막 데이터의 날짜를 KST 오전 7시로 변환
        OffsetDateTime lastDateTime = dateTimes[n-1]
            .atZoneSameInstant(KST)
            .withHour(7).withMinute(0).withSecond(0).withNano(0)
            .toOffsetDateTime();
        double lastPrice = prices[n-1];
        for (int i = 1; i <= maxOffset; i++) {
            OffsetDateTime nextDateTime = lastDateTime.plusDays(i);
            LocalDate nextDate = nextDateTime.toLocalDate();
            boolean nextHol = HOLIDAYS.values().stream().anyMatch(r ->
                !nextDate.isBefore(r[0].minusDays(1)) && !nextDate.isAfter(r[1])
            );
            double pred = intercept + phi * lastPrice + gamma * (nextHol ? 1 : 0);
            double z    = (lastPrice - pred) / (sigma > 0 ? sigma : 1);
            double pDrop = new NormalDistribution().cumulativeProbability(z);
            all.add(new ForecastPoint(nextDateTime, pred, pDrop));
            lastPrice = pred;
        }
        // requested offsets 필터링 및 원래 순서 유지
        return offsets.stream()
            .distinct()
            .sorted()
            .map(offset -> all.get(offset - 1))
            .collect(Collectors.toList());
    }

    /** 다음 달(30일) 중 최저 예측값과 해당 날짜 */
    public ForecastPoint forecastCheapestNextMonth(String code) {
        return forecastOnOffsets(code, IntStream.rangeClosed(1, 30)
            .boxed().collect(Collectors.toList()))
          .stream()
          .min(Comparator.comparing(ForecastPoint::predictedPrice))
          .orElseThrow();
    }
}

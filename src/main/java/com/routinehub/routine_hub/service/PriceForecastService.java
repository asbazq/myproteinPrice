package com.routinehub.routine_hub.service;

import com.routinehub.routine_hub.dto.ArmaForecastDto;
import com.routinehub.routine_hub.model.PriceEntry;
import com.routinehub.routine_hub.repository.PriceEntryRepository;

import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PriceForecastService {
  private static final Logger log = LoggerFactory.getLogger(PriceForecastService.class);
  private final PriceEntryRepository repo;

  /**  
   * 단순 ARX(1) 모델:  
   *   y[t] ≃ β0 + β1·y[t−1] + β2·(dummy_holiday[t])  
   */
  public ArmaForecastDto forecastNext(String code) {
    List<PriceEntry> hist = repo.findAllByProductCodeOrderByScrapedAtAsc(code);
    int n = hist.size();
    // 데이터 부족 시 폴백
    if (n < 2) {
      double cur = (n == 1 ? hist.get(0).getPrice() : 0.0);
      return new ArmaForecastDto(cur, 0.0, 0.5);
    }

    // 1) 시계열 & 날짜 준비
    double[] prices = hist.stream()
                          .mapToDouble(PriceEntry::getPrice)
                          .toArray();

    // 2) 휴일 범위 정의 (여러 날짜/기간을 넣을 수 있습니다)
    Map<String, LocalDate[]> HOLIDAYS = Map.ofEntries(
      Map.entry("설날",          new LocalDate[]{LocalDate.of(2025,1,20), LocalDate.of(2025,1,25)}),
      Map.entry("추석",          new LocalDate[]{LocalDate.of(2025,9,15), LocalDate.of(2025,9,21)}),
      Map.entry("블랙프라이데이", new LocalDate[]{LocalDate.of(2025,11,24),LocalDate.of(2025,11,30)}),
      // 월/일 일치일
      Map.entry("1월1일",   new LocalDate[]{LocalDate.of(2025,1,1),   LocalDate.of(2025,1,1)}),
      Map.entry("2월2일",   new LocalDate[]{LocalDate.of(2025,2,2),   LocalDate.of(2025,2,2)}),
      Map.entry("3월3일",   new LocalDate[]{LocalDate.of(2025,3,3),   LocalDate.of(2025,3,3)}),
      Map.entry("4월4일",   new LocalDate[]{LocalDate.of(2025,4,4),   LocalDate.of(2025,4,4)}),
      Map.entry("5월5일",   new LocalDate[]{LocalDate.of(2025,5,5),   LocalDate.of(2025,5,5)}),
      Map.entry("6월6일",   new LocalDate[]{LocalDate.of(2025,6,6),   LocalDate.of(2025,6,6)}),
      Map.entry("7월7일",   new LocalDate[]{LocalDate.of(2025,7,7),   LocalDate.of(2025,7,7)}),
      Map.entry("8월8일",   new LocalDate[]{LocalDate.of(2025,8,8),   LocalDate.of(2025,8,8)}),
      Map.entry("9월9일",   new LocalDate[]{LocalDate.of(2025,9,9),   LocalDate.of(2025,9,9)}),
      Map.entry("10월10일", new LocalDate[]{LocalDate.of(2025,10,10), LocalDate.of(2025,10,10)}),
      Map.entry("11월11일", new LocalDate[]{LocalDate.of(2025,11,11), LocalDate.of(2025,11,11)}),
      Map.entry("12월12일", new LocalDate[]{LocalDate.of(2025,12,12), LocalDate.of(2025,12,12)})
    );

    // 3) 회귀용 행렬 X, y 생성
    //    X 열 0 = lagged price, 열 1 = holiday dummy (0 or 1)
    int m = n - 1;
    double[]                 y = new double[m];
    double[][]               X = new double[m][2];
    LocalDateTime[] timestamps = hist.stream()
                                     .map(PriceEntry::getScrapedAt)
                                     .toArray(LocalDateTime[]::new);

    for (int i = 1; i < n; i++) {
        y[i - 1]        = prices[i];
        X[i - 1][0]     = prices[i - 1];
        LocalDate date = timestamps[i].toLocalDate();
        boolean isDiscountDay = HOLIDAYS.values().stream().anyMatch(range -> {
            // 할인 기간: (원래 start - 1일) 부터 원래 end일까지
            LocalDate windowStart = range[0].minusDays(1);
            LocalDate windowEnd   = range[1];
            return (!date.isBefore(windowStart) && !date.isAfter(windowEnd));
        });
        X[i - 1][1] = isDiscountDay ? 1.0 : 0.0;
    }

    // 4) OLS 회귀 적합
    OLSMultipleLinearRegression reg = new OLSMultipleLinearRegression();
    reg.newSampleData(y, X);
    double[] params = reg.estimateRegressionParameters();  
    double intercept = params[0];
    double phi       = params[1];
    double gamma     = params[2];

    // 잔차분산 σ² 계산 (df = m – #params)
    double rss    = reg.calculateResidualSumOfSquares();
    double sigma2 = rss / (m - params.length);
    double stdErr = Math.sqrt(sigma2);

    // 5) 다음 시점 dummy 변수 생성 (간격이 하루일 때)
    LocalDate lastDate = timestamps[n - 1].toLocalDate();
    LocalDate nextDate = lastDate.plusDays(1);
    boolean nextDiscountDay = HOLIDAYS.values().stream().anyMatch(range -> {
        LocalDate windowStart = range[0].minusDays(1);
        LocalDate windowEnd   = range[1];
        return (!nextDate.isBefore(windowStart) && !nextDate.isAfter(windowEnd));
    });

    // 6) 예측값 계산
    double nextPrice = intercept
                    + phi * prices[n - 1]
                    + gamma * (nextDiscountDay ? 1.0 : 0.0);

    // 7) 하락 확률 (현재 가격 대비)
    double current   = prices[n - 1];
    double z         = (current - nextPrice) / (stdErr > 0 ? stdErr : 1.0);
    double pDrop     = new NormalDistribution().cumulativeProbability(z);

    log.info("ARX(1) forecast: nextPrice={}, σ={}, dropProb={}", nextPrice, stdErr, pDrop);
    return new ArmaForecastDto(nextPrice, stdErr, pDrop);
  }
}

package com.routinehub.routine_hub.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import com.routinehub.routine_hub.model.PriceEntry;
import com.routinehub.routine_hub.repository.PriceEntryRepository;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyProteinCrawlerService {
    private final PriceEntryRepository priceEntryRepository;

    private static final String BASKET_URL    = "https://www.myprotein.co.kr/basket/";
    private static final String PRODUCT_URL   = "https://www.myprotein.co.kr/p/.../10530943/";
    private static final String PRODUCT_CODE  = "IMPACT_WHEY_10530943";
    private static final String DISCOUNT_CODE = "Tomato";
    private static final String FLAVOR_TEXT   = "내추럴 초콜렛";
    private static final String AMOUNT_KEY    = "2x2.5kg";

    @Scheduled(cron = "0 0 07 * * *", zone = "Asia/Seoul")
    // @Scheduled(initialDelay = 4_000, fixedDelay = Long.MAX_VALUE)  // 테스트용
    public void scheduledTask() {
        runWithRetry(3);   // ← 최대 3회 재시도
    }

    private void runWithRetry(int maxRetry) {
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            log.info("{} 번째 시도", attempt);
            try {
                scheduledTaskBody();         // 실제 크롤링
                return;                       // 성공 시 종료
            } catch (SessionNotCreatedException |
                     NoSuchSessionException
                    | TimeoutException e) {
                attempt++;
                log.warn("WebDriver 세션 오류, 재시도 {}/{} — {}", attempt, maxRetry, e);
                sleepSilently(3000);          // 3초 대기 후 재시도
            } catch (Exception e) {           // 기타 예외 → 바로 로그 후 종료
                log.error("scheduledTask 예외", e);
                return;
            }
        }
        log.error("WebDriver 세션 재시도 {}회 모두 실패", maxRetry);
    }

    private void scheduledTaskBody() throws Exception {
        log.info("[scheduledTaskBody] 시작 — URL: {}", BASKET_URL);

        Path tmpProfile = Files.createTempDirectory("cprof-");
        WebDriverManager.chromedriver()
                .driverVersion("138.0.7204.168")   // ← chrome --version 그대로
                .setup();
        System.setProperty("webdriver.chrome.binary", "/opt/google/chrome/chrome");
        // 2) Chrome 옵션
        ChromeOptions opts = new ChromeOptions();
        opts.setBinary("/opt/google/chrome/chrome");
        opts.addArguments(
            "--headless",                    // 레거시 headless
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-setuid-sandbox",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--remote-allow-origins=*",
            "--user-data-dir=" + tmpProfile
        );

        // 3) 드라이버 생성
        ChromeDriver driver = new ChromeDriver(opts);

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            driver.get(BASKET_URL);
            dismissOverlays(driver);

            boolean basketEmpty = driver.findElements(
                    By.cssSelector("p[data-e2e='basket-no_items']"))
                .stream()
                .anyMatch(el ->
                    el.getText().contains("장바구니에 추가된 제품이 없습니다"));

            if (basketEmpty) {
                addToBasket(driver, wait);
            }

            scrapPrice(driver, wait);

        } finally {
            // 4) 자원 정리
            driver.quit();
            FileSystemUtils.deleteRecursively(tmpProfile);
        }
    }

    /** 장바구니 담기 로직 **/
    private void addToBasket(WebDriver driver,WebDriverWait wait) {
        driver.get(PRODUCT_URL);
        Select flavor = new Select(
            wait.until(ExpectedConditions.elementToBeClickable(
                By.id("Flavour-variation")
            ))
        );
        flavor.selectByVisibleText(FLAVOR_TEXT);

        By amountSelector = By.cssSelector(
            "button[data-option='Amount'][data-key='" + AMOUNT_KEY + "']"
        );
        WebElement amountBtn = wait.until(
            ExpectedConditions.elementToBeClickable(amountSelector)
        );
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", amountBtn);

        WebElement addBtn = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("add-to-basket"))
        );
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addBtn);

        System.out.printf("[장바구니담기] %s 정상 추가됨%n", PRODUCT_CODE);
    }

    private void scrapPrice(WebDriver driver, WebDriverWait wait) {

        /* ── 1. 할인코드 입력 & 적용 ─────────────────────────── */
        By priceSel      = By.cssSelector("div.item-price span.font-bold.text-lg");
        By originSel     = By.cssSelector("div.item-price p.bg-accent-200.line-through");
        By discountSel   = By.cssSelector("div.item-price p.bg-accent");
        By promoInputSel = By.id("promo-code-input");
        By applyBtnSel   = By.id("promo-code-add");

        driver.get(BASKET_URL);

        WebElement promoInput = wait.until(ExpectedConditions.elementToBeClickable(promoInputSel));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block:'center'});", promoInput);
        promoInput.clear();
        promoInput.sendKeys(DISCOUNT_CODE);

        // 기존 가격(문자열)만 저장
        String beforeText = driver.findElement(priceSel).getText();

        WebElement applyBtn = wait.until(ExpectedConditions.elementToBeClickable(applyBtnSel));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block:'center'});", applyBtn);
        applyBtn.click();

        /* 2. 가격이 바뀔 때까지 대기 (Element 재‑찾으므로 Stale 방지) */
        wait.until(ExpectedConditions.not(
            ExpectedConditions.textToBePresentInElementLocated(priceSel, beforeText))
        );

        /* ── 3. 최종 가격·원가·할인 정보 읽기 ───────────────── */
        int discountedPrice = Integer.parseInt(
            driver.findElement(priceSel).getText().replaceAll("[^0-9]", "")
        );

        int originPrice = Integer.parseInt(
            driver.findElement(originSel).getText().replaceAll("[^0-9]", "")
        );

        String discountText = driver.findElement(discountSel).getText();
        Matcher m = Pattern.compile("\\d[\\d,]*").matcher(discountText);
        m.find(); int discountAmount = Integer.parseInt(m.group().replace(",", ""));
        m.find(); int discountRate   = Integer.parseInt(m.group());

        /* ── 4. DB 저장 ─────────────────────────────────────── */
        PriceEntry pe = new PriceEntry();
        pe.setProductCode(PRODUCT_CODE);
        pe.setProductName("IMPACT_WHEY " + FLAVOR_TEXT + AMOUNT_KEY);
        pe.setOriginPrice(originPrice);
        pe.setPrice(discountedPrice);
        pe.setDiscountAmount(discountAmount);
        pe.setDiscountRate(discountRate);
        pe.setScrapedAt(OffsetDateTime.now());
        priceEntryRepository.save(pe);

        log.info("[MyProtein] 원가={}₩, 할인가={}₩, 할인액={}₩, 할인율={}%",
                originPrice, discountedPrice, discountAmount, discountRate);
    }


    // @Scheduled(cron = "0 5 7 * * *", zone = "Asia/Seoul")
    // public void storeWeeklyForecast() {
    //     String code = "IMPACT_WHEY_10530943";
    //     OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
    //         .withHour(7).withMinute(0).withSecond(0).withNano(0);

    //     // 1~7일 예측
    //     forecastService.forecastOnOffsets(
    //         code,
    //         IntStream.rangeClosed(1, 7).boxed().toList()
    //     ).forEach(p -> {
    //         ForecastEntry entry = new ForecastEntry();
    //         entry.setProductCode(code);
    //         entry.setPredictedPrice(p.predictedPrice());
    //         entry.setDropProbability(p.dropProb());
    //         entry.setForecastDate(p.date());
    //         entry.setCreatedAt(now);
    //         forecastEntryRepository.save(entry);
    //     });
    // }

    private void dismissOverlays(WebDriver driver) {
        List<By> popups = List.of(
            By.cssSelector("button[data-testid='close-button']"),
            By.cssSelector("button[aria-label='Close']"),
            By.cssSelector("div.modal button.close")    // 필요에 따라 추가
        );
        for (By sel : popups) {
            driver.findElements(sel).forEach(el -> {
                try { el.click(); }
                catch (Exception ignored) {}
            });
        }
    }

    private static void sleepSilently(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
    }

}

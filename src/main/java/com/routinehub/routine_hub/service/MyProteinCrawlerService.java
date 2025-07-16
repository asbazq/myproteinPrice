package com.routinehub.routine_hub.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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

    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    // @Scheduled(initialDelay = 3_000, fixedDelay = Long.MAX_VALUE)  // 테스트용
    public void scheduledTask() {

        Path       tmpProfile = null;
        WebDriver  driver     = null;

        try {
            /* 1) 크롬/드라이버 위치 읽기 */
            String chromeBin    = System.getenv("WEB_DRIVER_CHROME_BIN");
            String chromeDriver = System.getenv("WEB_DRIVER_CHROME_DRIVER");
            System.setProperty("webdriver.chrome.driver", chromeDriver);

            /* 2) 세션 전용 user‑data‑dir */
            tmpProfile = Files.createTempDirectory("chrome-prof-");

            /* 3) 옵션 직접 구성 */
            ChromeOptions opts = new ChromeOptions();
            opts.setBinary(chromeBin);
            opts.addArguments(
                "--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                "--disable-setuid-sandbox", "--single-process",
                "--window-size=1920,1080",
                "--user-data-dir=" + tmpProfile.toAbsolutePath(),
                "--remote-allow-origins=*"
            );

            /* 4) 드라이버 생성 → 작업 수행 */
            driver = new ChromeDriver(opts);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            driver.get(BASKET_URL);
            dismissOverlays(driver);

            boolean empty = driver.findElements(
                By.cssSelector("p[data-e2e='basket-no_items']"))
                .stream()
                .anyMatch(el -> el.getText().contains("장바구니에 추가된 제품이 없습니다"));

            if (empty) addToBasket(driver, wait);
            scrapPrice(driver, wait);

        } catch (Exception e) {
            log.error("[RoutineHub] scheduledTask 실패", e);

        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
            }
            if (tmpProfile != null) {
                try { FileSystemUtils.deleteRecursively(tmpProfile); }
                catch (IOException ignored) {}
            }
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
        driver.get(BASKET_URL);
        WebElement promoInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("promo-code-input"))
        );
        ((JavascriptExecutor) driver)
            .executeScript("arguments[0].scrollIntoView({block:'center'});", promoInput);
        promoInput.clear();
        promoInput.sendKeys(DISCOUNT_CODE);

        WebElement beforeSpan = driver.findElement(
            By.cssSelector("div.item-price span.font-bold.text-lg")
        );
        String beforeText = beforeSpan.getText();

        WebElement applyBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("promo-code-add"))
        );
        ((JavascriptExecutor) driver)
            .executeScript("arguments[0].scrollIntoView({block:'center'});", applyBtn);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", applyBtn);

        wait.until(drv -> !drv.findElement(
            By.cssSelector("div.item-price span.font-bold.text-lg"))
            .getText().equals(beforeText)
        );

        int discountedPrice = Integer.parseInt(
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.item-price span.font-bold.text-lg")
            )).getText().replaceAll("[^0-9]", "")
        );
        int originPrice = Integer.parseInt(
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.item-price p.bg-accent-200.line-through")
            )).getText().replaceAll("[^0-9]", "")
        );

        String discountText = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.item-price p.bg-accent")
            )
        ).getText();

        Matcher m = Pattern.compile("\\d[\\d,]*").matcher(discountText);
        m.find(); int discountAmount = Integer.parseInt(m.group().replaceAll(",", ""));
        m.find(); int discountRate   = Integer.parseInt(m.group());

        PriceEntry pe = new PriceEntry();
        pe.setProductCode(PRODUCT_CODE);
        pe.setProductName("IMPACT_WHEY " + FLAVOR_TEXT + AMOUNT_KEY);
        pe.setOriginPrice(originPrice);
        pe.setPrice(discountedPrice);
        pe.setDiscountAmount(discountAmount);
        pe.setDiscountRate(discountRate);
        pe.setScrapedAt(OffsetDateTime.now());
        priceEntryRepository.save(pe);

        System.out.printf(
            "[MyProtein] %s 원가=%d원 할인가=%d원 할인액=%d원 할인율=%d%%%n",
            PRODUCT_CODE, originPrice, discountedPrice,
            discountAmount, discountRate
        );
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

}

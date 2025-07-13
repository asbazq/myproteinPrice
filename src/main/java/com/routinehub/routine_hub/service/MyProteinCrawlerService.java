package com.routinehub.routine_hub.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.routinehub.routine_hub.model.PriceEntry;
import com.routinehub.routine_hub.repository.PriceEntryRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class MyProteinCrawlerService {
    private final WebDriver driver;
    private final PriceEntryRepository repo;

    private static final String BASKET_URL    = "https://www.myprotein.co.kr/basket/";
    private static final String PRODUCT_URL   = "https://www.myprotein.co.kr/p/.../10530943/";
    private static final String PRODUCT_CODE  = "IMPACT_WHEY_10530943";
    private static final String DISCOUNT_CODE = "Tomato";
    private static final String FLAVOR_TEXT   = "내추럴 초콜렛";
    private static final String AMOUNT_KEY    = "2x2.5kg";

    // @PostConstruct
    @Scheduled(cron = "0 0 7 * * * ", zone="Asia/Seoul")
    public void scheduledTask() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // 1) 장바구니 페이지 열기
        driver.get(BASKET_URL);
        List<WebElement> emptyMsgs = driver.findElements(
            By.cssSelector("p[data-e2e='basket-no_items']"));
        boolean basketEmpty = !emptyMsgs.isEmpty()
            && emptyMsgs.get(0).getText().contains("장바구니에 추가된 제품이 없습니다");

        if (basketEmpty) {
            // B) 장바구니에 상품이 없으면 → 장바구니 담기 로직 실행
            addToBasket(wait);
        }

        // A) 할인코드 적용 & 가격 스크랩 (장바구니가 비어 있든, 담기 후든 무조건 실행)
        scrapPrice(wait);
    }

    /** 장바구니 담기 로직 **/
    private void addToBasket(WebDriverWait wait) {
        driver.get(PRODUCT_URL);

        // 맛 선택
        Select flavor = new Select(
            wait.until(ExpectedConditions.elementToBeClickable(
                By.id("Flavour-variation")
            ))
        );
        flavor.selectByVisibleText(FLAVOR_TEXT);

        // 용량 선택
        By amountSelector = By.cssSelector("button[data-option='Amount'][data-key='" + AMOUNT_KEY + "']");

        // 클릭 가능해질 때까지 대기
        WebElement amountBtn = wait.until(
            ExpectedConditions.elementToBeClickable(amountSelector)
        );

        // JS 클릭으로 인터셉트 우회
        ((JavascriptExecutor) driver)
            .executeScript("arguments[0].click();", amountBtn);
        // amountBtn.click();

        // JS 클릭으로 add-to-basket 버튼 누르기
        WebElement addBtn = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("add-to-basket"))
        );
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addBtn);

        System.out.printf("[장바구니담기] %s 정상 추가됨%n", PRODUCT_CODE);
    }

    /** 할인코드 적용 후 원가/할인가 스크랩 **/
    private void scrapPrice(WebDriverWait wait) {
        driver.get(BASKET_URL);

        WebElement promoInput = new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.visibilityOfElementLocated(
                By.id("promo-code-input")
            ));

        ((JavascriptExecutor)driver).executeScript(
            "arguments[0].scrollIntoView({block:'center'});", promoInput
        );
        promoInput.clear();
        promoInput.sendKeys(DISCOUNT_CODE);

        WebElement beforeSpan = driver.findElement(
            By.cssSelector("div.item-price span.font-bold.text-lg")
        );
        String beforeText = beforeSpan.getText();

        // (쿠폰 입력 후) “추가하기” 버튼 JS 클릭
        WebElement applyBtn = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("promo-code-add"))
        );
        // 뷰포트 중앙에 스크롤
        ((JavascriptExecutor) driver)
            .executeScript("arguments[0].scrollIntoView({block:'center'});", applyBtn);
        // JS click으로 오버레이 인터셉트 우회
        ((JavascriptExecutor) driver)
            .executeScript("arguments[0].click();", applyBtn);

        wait.until(driver -> {
            String nowText = driver.findElement(
                By.cssSelector("div.item-price span.font-bold.text-lg")
            ).getText();
            return !nowText.equals(beforeText);
        });

        // (1) 최종 결제금액 (할인가)
        WebElement totalSpan = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.item-price span.font-bold.text-lg")
            )
        );
        int discountedPrice = Integer.parseInt(
            totalSpan.getText().replaceAll("[^0-9]", "")
        );

        // (2) 원가(originPrice)
        WebElement originEl = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.item-price p.bg-accent-200.line-through")
            )
        );
        int originPrice = Integer.parseInt(
            originEl.getText().replaceAll("[^0-9]", "")
        );

        // (3) 할인 금액 & 할인율
        WebElement discountEl = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.item-price p.bg-accent")
            )
        );
        String discountText = discountEl.getText();
        // e.g. "할인 금액 ₩103,804 (40%)"
        Matcher m = Pattern.compile("\\d[\\d,]*").matcher(discountText);

        m.find();
        int discountAmount = Integer.parseInt(m.group().replaceAll(",", ""));
        m.find();
        int discountRate   = Integer.parseInt(m.group());

        // (4) DB 저장
        PriceEntry pe = new PriceEntry();
        pe.setProductCode(PRODUCT_CODE);
        pe.setProductName("IMPACT_WHEY " + FLAVOR_TEXT +  AMOUNT_KEY);
        pe.setOriginPrice(originPrice);
        pe.setPrice(discountedPrice);
        pe.setDiscountAmount(discountAmount);
        pe.setDiscountRate(discountRate);
        pe.setScrapedAt(LocalDateTime.now());
        repo.save(pe);

        System.out.printf(
        "[MyProtein] %s 원가=%d원 할인가=%d원 할인액=%d원 할인율=%d%%%n",
        PRODUCT_CODE, originPrice, discountedPrice,
        discountAmount, discountRate
        );
    }
}

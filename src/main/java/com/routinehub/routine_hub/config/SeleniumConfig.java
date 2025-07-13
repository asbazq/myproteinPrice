package com.routinehub.routine_hub.config;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.bonigarcia.wdm.WebDriverManager;

@Configuration
public class SeleniumConfig {

    @Bean(destroyMethod = "quit")
    public WebDriver webDriver() {
        // Selenium Manager 비활성화 (WebDriverManager 대신 직접 경로 세팅)
        System.setProperty("selenium.manager.disabled", "true");

        // 환경 변수에서 chromium 바이너리와 chromedriver 경로 읽기
        String chromeBin    = System.getenv("WEB_DRIVER_CHROME_BIN");
        String chromeDriver = System.getenv("WEB_DRIVER_CHROME_DRIVER");

        if (chromeBin == null || chromeDriver == null) {
            throw new IllegalStateException(
              "환경 변수 WEB_DRIVER_CHROME_BIN, WEB_DRIVER_CHROME_DRIVER 를 반드시 설정해야 합니다."
            );
        }

        // chromedriver 위치를 시스템 프로퍼티에 지정
        System.setProperty("webdriver.chrome.driver", chromeDriver);

        // Chrome headless 모드 옵션
        ChromeOptions options = new ChromeOptions();
        options.setBinary(chromeBin);
        options.addArguments(
           "--headless=new",             // 창을 띄우지 않습니다
          "--no-sandbox",           // root 권한 컨테이너 환경에서 필요
          "--disable-dev-shm-usage", // /dev/shm 부족 문제 회피
          "--disable-setuid-sandbox",
          "--single-process",
          "--window-size=1920,1080",
          "--remote-debugging-port=9222"
        );

        return new ChromeDriver(options);
    }
}

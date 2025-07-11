package com.routinehub.routine_hub.config;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeleniumConfig {

    @Bean(destroyMethod = "quit")
    public WebDriver webDriver() {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments(
          "--headless",
          "--disable-gpu",
          "--no-sandbox",
          "--window-size=1920,1080"
        );
        return new ChromeDriver(opts);
    }
}

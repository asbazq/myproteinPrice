package com.routinehub.routine_hub.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name="price_entry")
@Getter
public class PriceEntry {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productCode; 
    private String productName;
    private String productUrl;
    private int originPrice;
    private int price;
    private int discountAmount;
    private int discountRate;
    private OffsetDateTime scrapedAt;

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setProductUrl(String prdoductUrl) {
        this.productUrl = prdoductUrl;
    }

    public void setOriginPrice(int originPrice) {
        this.originPrice = originPrice;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setDiscountAmount(int discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void setDiscountRate(int discountRate) {
        this.discountRate = discountRate;
    }

    public void setScrapedAt(OffsetDateTime scrapedAt) {
        this.scrapedAt = scrapedAt;
    }
}
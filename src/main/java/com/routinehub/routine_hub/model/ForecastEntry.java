package com.routinehub.routine_hub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "forecast_entry")
@Getter
@Setter
public class ForecastEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productCode;

    @Column(nullable = false)
    private OffsetDateTime forecastDate;

    @Column(nullable = false)
    private double predictedPrice;

    @Column(nullable = false)
    private double dropProbability;

    @Column(nullable = false)
    private OffsetDateTime createdAt;
}

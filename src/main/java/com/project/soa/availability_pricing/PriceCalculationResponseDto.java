package com.project.soa.availability_pricing;

import java.math.BigDecimal;

public class PriceCalculationResponseDto {
    private BigDecimal basePrice;
    private BigDecimal finalPrice;
    private int numberOfNights;
    private String breakdown;

    public PriceCalculationResponseDto(BigDecimal basePrice, BigDecimal finalPrice, int numberOfNights, String breakdown) {
        this.basePrice = basePrice;
        this.finalPrice = finalPrice;
        this.numberOfNights = numberOfNights;
        this.breakdown = breakdown;
    }

    public BigDecimal getBasePrice() { return basePrice; }
    public BigDecimal getFinalPrice() { return finalPrice; }
    public int getNumberOfNights() { return numberOfNights; }
    public String getBreakdown() { return breakdown; }
}

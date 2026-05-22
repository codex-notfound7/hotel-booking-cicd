package com.project.soa.availability_pricing;


import java.math.BigDecimal;

public final class PricingMapper {

    private PricingMapper() {}

    public static PriceCalculationResponseDto toDto(
            BigDecimal basePrice,
            BigDecimal finalPrice,
            int numberOfNights,
            String breakdown) {
        return new PriceCalculationResponseDto(basePrice, finalPrice, numberOfNights, breakdown);
    }
}
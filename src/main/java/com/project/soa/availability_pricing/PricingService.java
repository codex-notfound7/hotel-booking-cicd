package com.project.soa.availability_pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface PricingService {

    AvailabilityCheckResponseDto checkAvailability(AvailabilityCheckRequestDto request);

    PriceCalculationResponseDto calculatePrice(PriceCalculationRequestDto request);

    BigDecimal calculateTotalPrice(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut);

    boolean isFullyBooked(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, UUID excludeBookingId);
}

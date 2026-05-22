package com.project.soa.availability_pricing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/availability")
@Tag(name = "Availability & Pricing", description = "Check availability and calculate prices")
public class AvailabilityPricingController {

    private final PricingService pricingService;

    public AvailabilityPricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('GUEST', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Check availability", description = "Check if room is available for date range and guest count")
    public AvailabilityCheckResponseDto checkAvailability(@Valid @RequestBody AvailabilityCheckRequestDto request) {
        return pricingService.checkAvailability(request);
    }

    @PostMapping("/price")
    @PreAuthorize("hasAnyRole('GUEST', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Calculate price", description = "Calculate final price with weekend and seasonal multipliers")
    public PriceCalculationResponseDto calculatePrice(@Valid @RequestBody PriceCalculationRequestDto request) {
        return pricingService.calculatePrice(request);
    }
}

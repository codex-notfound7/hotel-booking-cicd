package com.project.soa.availability_pricing;

import java.math.BigDecimal;
import java.util.UUID;

public class AvailabilityCheckResponseDto {
    private UUID roomTypeId;
    private boolean available;
    private String message;
    private BigDecimal calculatedPrice;

    public AvailabilityCheckResponseDto(UUID roomTypeId, boolean available, String message, BigDecimal calculatedPrice) {
        this.roomTypeId = roomTypeId;
        this.available = available;
        this.message = message;
        this.calculatedPrice = calculatedPrice;
    }

    public static AvailabilityCheckResponseDto available(UUID roomTypeId, BigDecimal price) {
        return new AvailabilityCheckResponseDto(roomTypeId, true, "Room is available", price);
    }

    public static AvailabilityCheckResponseDto unavailable(UUID roomTypeId, String reason) {
        return new AvailabilityCheckResponseDto(roomTypeId, false, reason, null);
    }

    public UUID getRoomTypeId() { return roomTypeId; }
    public boolean isAvailable() { return available; }
    public String getMessage() { return message; }
    public BigDecimal getCalculatedPrice() { return calculatedPrice; }
}

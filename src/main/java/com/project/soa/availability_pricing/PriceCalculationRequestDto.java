package com.project.soa.availability_pricing;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public class PriceCalculationRequestDto {
    @NotNull(message = "Room type ID is required")
    private UUID roomTypeId;

    @NotNull(message = "Check-in date is required")
    private LocalDate checkIn;

    @NotNull(message = "Check-out date is required")
    private LocalDate checkOut;

    public UUID getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(UUID roomTypeId) { this.roomTypeId = roomTypeId; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
}

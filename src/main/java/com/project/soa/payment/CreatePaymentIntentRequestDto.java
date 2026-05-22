package com.project.soa.payment;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreatePaymentIntentRequestDto {
    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
}

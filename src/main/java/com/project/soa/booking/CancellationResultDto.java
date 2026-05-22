package com.project.soa.booking;

import java.math.BigDecimal;
import java.util.UUID;


public record CancellationResultDto(
        UUID bookingId,
        String bookingStatus,
        BigDecimal refundAmount,
        BigDecimal penaltyAmount,
        String policyApplied,
        String paymentStatus   // null if no payment was ever created
) {}

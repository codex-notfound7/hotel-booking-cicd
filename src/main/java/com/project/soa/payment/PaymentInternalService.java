package com.project.soa.payment;

import java.math.BigDecimal;
import java.util.UUID;


public interface PaymentInternalService {


    String handleBookingCancellation(UUID bookingId, BigDecimal penaltyAmount);

    boolean isBookingPaid(UUID bookingId);
}
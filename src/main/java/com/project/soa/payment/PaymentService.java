package com.project.soa.payment;

import java.util.UUID;

public interface PaymentService {

    Payment createPaymentIntent(CreatePaymentIntentRequestDto dto);

    PaymentResponseDto simulatePayment(UUID paymentId, boolean success);


    PaymentResponseDto getPaymentByBookingId(UUID bookingId);

    PaymentResponseDto getPayment(UUID paymentId);
}

package com.project.soa.payment;

import com.project.soa.booking.*;
import com.project.soa.common.exception.BusinessRuleException;
import com.project.soa.common.exception.ResourceNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService, PaymentInternalService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;


    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;

    }

    @Override
    public Payment createPaymentIntent(CreatePaymentIntentRequestDto dto) {

        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", dto.getBookingId()));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    @Override
    public PaymentResponseDto simulatePayment(UUID paymentId, boolean success) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);



        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }

        return PaymentMapper.toDto(payment);
    }

    @Override
    public String handleBookingCancellation(UUID bookingId, BigDecimal penaltyAmount) {

        return paymentRepository.findByBookingId(bookingId).map(payment -> {

            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                return null;
            }

            if (penaltyAmount.compareTo(BigDecimal.ZERO) == 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIAL_REFUND);
            }

            paymentRepository.save(payment);

            return payment.getStatus().name();

        }).orElse(null);
    }

    @Override
    public boolean isBookingPaid(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .map(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .orElse(false);
    }

    @Override
    public PaymentResponseDto getPaymentByBookingId(UUID bookingId) {

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", bookingId));

        return PaymentMapper.toDto(payment);
    }

    @Override
    public PaymentResponseDto getPayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        return PaymentMapper.toDto(payment);
    }
}
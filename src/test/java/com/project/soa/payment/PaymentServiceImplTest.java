package com.project.soa.payment;

import com.project.soa.booking.Booking;
import com.project.soa.booking.BookingRepository;
import com.project.soa.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock PaymentRepository paymentRepository;
    @Mock BookingRepository bookingRepository;

    @InjectMocks PaymentServiceImpl service;

    UUID bookingId = UUID.randomUUID();
    UUID paymentId = UUID.randomUUID();

    // ── createPaymentIntent ───────────────────────────────────────────────────

    @Test
    void createPaymentIntent_bookingFound_createsPaymentWithCorrectAmount() {
        Booking booking = booking(new BigDecimal("350.00"));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(paymentId);
            return p;
        });

        Payment result = service.createPaymentIntent(intentDto(bookingId));

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getAmount()).isEqualByComparingTo("350.00");
        assertThat(result.getBooking()).isEqualTo(booking);
    }

    @Test
    void createPaymentIntent_bookingNotFound_throwsResourceNotFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPaymentIntent(intentDto(bookingId)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── simulatePayment ───────────────────────────────────────────────────────

    @Test
    void simulatePayment_success_setsStatusToSuccess() {
        Payment payment = payment(PaymentStatus.PENDING);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponseDto result = service.simulatePayment(paymentId, true);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS.name());
    }

    @Test
    void simulatePayment_failure_setsStatusToFailed() {
        Payment payment = payment(PaymentStatus.PENDING);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponseDto result = service.simulatePayment(paymentId, false);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED.name());
    }

    @Test
    void simulatePayment_notFound_throwsResourceNotFound() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.simulatePayment(paymentId, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── handleBookingCancellation ─────────────────────────────────────────────

    @Test
    void handleCancellation_noPenalty_setsRefunded() {
        Payment payment = payment(PaymentStatus.SUCCESS);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String status = service.handleBookingCancellation(bookingId, BigDecimal.ZERO);

        assertThat(status).isEqualTo(PaymentStatus.REFUNDED.name());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void handleCancellation_withPenalty_setsPartialRefund() {
        Payment payment = payment(PaymentStatus.SUCCESS);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String status = service.handleBookingCancellation(bookingId, new BigDecimal("50.00"));

        assertThat(status).isEqualTo(PaymentStatus.PARTIAL_REFUND.name());
    }

    @Test
    void handleCancellation_paymentNotSuccess_doesNotChangeStatus() {
        Payment payment = payment(PaymentStatus.PENDING);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));

        String status = service.handleBookingCancellation(bookingId, BigDecimal.ZERO);

        assertThat(status).isNull();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleCancellation_noPaymentRecord_returnsNull() {
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

        assertThat(service.handleBookingCancellation(bookingId, BigDecimal.ZERO)).isNull();
    }

    // ── isBookingPaid ─────────────────────────────────────────────────────────

    @Test
    void isBookingPaid_successStatus_returnsTrue() {
        when(paymentRepository.findByBookingId(bookingId))
                .thenReturn(Optional.of(payment(PaymentStatus.SUCCESS)));

        assertThat(service.isBookingPaid(bookingId)).isTrue();
    }

    @Test
    void isBookingPaid_pendingStatus_returnsFalse() {
        when(paymentRepository.findByBookingId(bookingId))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        assertThat(service.isBookingPaid(bookingId)).isFalse();
    }

    @Test
    void isBookingPaid_noPayment_returnsFalse() {
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

        assertThat(service.isBookingPaid(bookingId)).isFalse();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Booking booking(BigDecimal totalPrice) {
        Booking b = new Booking();
        b.setId(bookingId);
        b.setTotalPrice(totalPrice);
        return b;
    }

    private Payment payment(PaymentStatus status) {
        Payment p = new Payment();
        p.setId(paymentId);
        p.setStatus(status);
        p.setAmount(new BigDecimal("350.00"));
        return p;
    }

    private CreatePaymentIntentRequestDto intentDto(UUID bId) {
        CreatePaymentIntentRequestDto dto = new CreatePaymentIntentRequestDto();
        dto.setBookingId(bId);
        return dto;
    }
}

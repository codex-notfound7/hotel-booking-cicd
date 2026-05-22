package com.project.soa.notification;


import com.project.soa.booking.Booking;
import com.project.soa.booking.BookingRepository;
import com.project.soa.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;


@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final ApplicationEventPublisher eventPublisher;
    private final BookingRepository bookingRepository;

    public NotificationServiceImpl(ApplicationEventPublisher eventPublisher,
                                   BookingRepository bookingRepository) {
        this.eventPublisher = eventPublisher;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void sendBookingConfirmed(UUID bookingId) {
        eventPublisher.publishEvent(new BookingConfirmedEvent(bookingId));
    }

    @Override
    public void sendBookingCancelled(UUID bookingId) {
        eventPublisher.publishEvent(new BookingCancelledEvent(bookingId));
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        Booking booking = loadBooking(event.bookingId());
        log.info("Email sent: Booking confirmed. BookingId={}, Guest={}, Hotel={}, CheckIn={}, CheckOut={}",
                booking.getId(),
                booking.getUser() != null ? booking.getUser().getEmail() : "unknown",
                booking.getRoomType() != null && booking.getRoomType().getHotel() != null
                        ? booking.getRoomType().getHotel().getName() : "N/A",
                booking.getCheckIn(), booking.getCheckOut());
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void handleBookingCancelled(BookingCancelledEvent event) {
        Booking booking = loadBooking(event.bookingId());
        log.info("Email sent: Booking cancelled. BookingId={}, Guest={}, Hotel={}, CheckIn={}, CheckOut={}",
                booking.getId(),
                booking.getUser() != null ? booking.getUser().getEmail() : "unknown",
                booking.getRoomType() != null && booking.getRoomType().getHotel() != null
                        ? booking.getRoomType().getHotel().getName() : "N/A",
                booking.getCheckIn(), booking.getCheckOut());
    }

    private Booking loadBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId) // we come back here soon (never)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }
}

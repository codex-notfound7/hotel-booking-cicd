package com.project.soa.booking;

import com.project.soa.auth.user.User;
import com.project.soa.auth.user.UserInternalService;
import com.project.soa.availability_pricing.PricingService;
import com.project.soa.catalog.CatalogInternalService;
import com.project.soa.catalog.CatalogStatus;
import com.project.soa.catalog.RoomType;
import com.project.soa.common.exception.BusinessRuleException;
import com.project.soa.common.exception.ResourceNotFoundException;
import com.project.soa.notification.NotificationService;
import com.project.soa.payment.PaymentInternalService;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BookingServiceImpl implements BookingService, BookingInternalService {

    private static final int FREE_CANCELLATION_MIN_DAYS   = 7;
    private static final int PARTIAL_REFUND_MIN_DAYS      = 3;
    private static final BigDecimal PARTIAL_REFUND_RATE   = new BigDecimal("0.50");
    private static final Duration PENDING_RESERVATION_TTL = Duration.ofMinutes(15);

    private final UserInternalService userService;
    private final BookingRepository bookingRepository;
    private final CatalogInternalService catalogInternalService;
    private final PricingService pricingService;
    private final NotificationService notificationService;
    private final PaymentInternalService paymentService;
    private final Clock clock;

    public BookingServiceImpl(UserInternalService userService,
                              BookingRepository bookingRepository,
                              CatalogInternalService catalogInternalService,
                              PricingService pricingService,
                              NotificationService notificationService,
                              PaymentInternalService paymentService,
                              Clock clock) {
        this.userService              = userService;
        this.bookingRepository        = bookingRepository;
        this.catalogInternalService   = catalogInternalService;
        this.pricingService           = pricingService;
        this.notificationService      = notificationService;
        this.paymentService           = paymentService;
        this.clock                    = clock;
    }

    @Override
    public Booking createBooking(CreateBookingRequestDto dto) {

        if (dto.getCheckOut() != null && dto.getCheckIn() != null
                && !dto.getCheckOut().isAfter(dto.getCheckIn())) {
            throw new BusinessRuleException("Check-out must be after check-in");
        }

        User user = resolveAuthenticatedUser();

        // Acquire pessimistic write lock via catalog internal service — no direct repo access
        RoomType roomType = catalogInternalService.getRoomTypeForUpdate(dto.getRoomTypeId());

        if (roomType.getStatus() != CatalogStatus.ACTIVE) {
            throw new BusinessRuleException("Room type is not available for booking");
        }
        if (roomType.getHotel() == null || roomType.getHotel().getStatus() != CatalogStatus.ACTIVE) {
            throw new BusinessRuleException("Hotel is not currently accepting bookings");
        }
        if (roomType.getCapacity() < dto.getNumberOfGuests()) {
            throw new BusinessRuleException("Guest count exceeds room capacity");
        }
        if (pricingService.isFullyBooked(roomType.getId(), dto.getCheckIn(), dto.getCheckOut(), null)) {
            throw new BusinessRuleException("No rooms available for the selected dates");
        }

        BigDecimal totalPrice = pricingService.calculateTotalPrice(
                roomType.getId(), dto.getCheckIn(), dto.getCheckOut());

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoomType(roomType);
        booking.setCheckIn(dto.getCheckIn());
        booking.setCheckOut(dto.getCheckOut());
        booking.setNumberOfGuests(dto.getNumberOfGuests());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(totalPrice);
        booking.initTimestamps(clock);
        booking.setPendingExpiresAt(LocalDateTime.now(clock).plus(PENDING_RESERVATION_TTL));

        return bookingRepository.save(booking);
    }

    @Override
    public Booking confirmBooking(UUID bookingId) {
        UUID managerId = resolveAuthenticatedUser().getId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        UUID hotelManagerId = booking.getRoomType().getHotel().getManager() != null
                ? booking.getRoomType().getHotel().getManager().getId()
                : null;

        if (!managerId.equals(hotelManagerId)) {
            throw new BusinessRuleException("Access denied: you do not manage the hotel for this booking");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) return booking;
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING bookings can be confirmed");
        }
        if (booking.getPendingExpiresAt() != null
                && booking.getPendingExpiresAt().isBefore(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("Cannot confirm: booking reservation expired");
        }
        if (!paymentService.isBookingPaid(bookingId)) {
            throw new BusinessRuleException("Cannot confirm: booking is not paid");
        }
        if (pricingService.isFullyBooked(
                booking.getRoomType().getId(),
                booking.getCheckIn(), booking.getCheckOut(), bookingId)) {
            throw new BusinessRuleException("Cannot confirm: room type is fully booked for the selected dates");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPendingExpiresAt(null);
        booking.touch(clock);

        Booking saved = bookingRepository.save(booking);
        notificationService.sendBookingConfirmed(saved.getId());
        return saved;
    }

    @Override
    public CancellationResultDto cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Already cancelled");
        }

        LocalDate today = LocalDate.now(clock);
        if (!booking.getCheckIn().isAfter(today)) {
            throw new BusinessRuleException("Cannot cancel on or after check-in day");
        }

        BigDecimal totalPrice = booking.getTotalPrice() == null ? BigDecimal.ZERO : booking.getTotalPrice();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(today, booking.getCheckIn());

        BigDecimal refundAmount;
        BigDecimal penaltyAmount;
        String policy;

        if (daysUntilCheckIn >= FREE_CANCELLATION_MIN_DAYS) {
            refundAmount  = totalPrice;
            penaltyAmount = BigDecimal.ZERO;
            policy = "Full refund (7+ days before check-in)";
        } else if (daysUntilCheckIn >= PARTIAL_REFUND_MIN_DAYS) {
            refundAmount  = totalPrice.multiply(PARTIAL_REFUND_RATE).setScale(2, RoundingMode.HALF_UP);
            penaltyAmount = totalPrice.subtract(refundAmount).setScale(2, RoundingMode.HALF_UP);
            policy = "50% refund (3–6 days before check-in)";
        } else {
            refundAmount  = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            penaltyAmount = totalPrice.setScale(2, RoundingMode.HALF_UP);
            policy = "No refund (<3 days before check-in)";
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.touch(clock);
        bookingRepository.save(booking);

        String paymentStatus = paymentService.handleBookingCancellation(bookingId, penaltyAmount);
        notificationService.sendBookingCancelled(booking.getId());

        return new CancellationResultDto(
                bookingId, BookingStatus.CANCELLED.name(),
                refundAmount, penaltyAmount, policy, paymentStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookings() {
        User user = resolveAuthenticatedUser();
        return bookingRepository.findByUserIdOrderByCheckInDesc(user.getId())
                .stream().map(BookingMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getUpcomingBookingsForManager() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessRuleException("No authenticated user found");
        }
        UUID callerId = UUID.fromString(jwt.getSubject());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        var today = LocalDate.now(clock);
        var bookings = isAdmin
                ? bookingRepository.findUpcomingNotCancelled(today)
                : bookingRepository.findUpcomingNotCancelledForManager(callerId, today);
        return bookings.stream().map(BookingMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getAllBookings() {
        return bookingRepository.findAll().stream().map(BookingMapper::toDto).toList();
    }

    // ── BookingInternalService ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findAllWithHotelDetails() {
        return bookingRepository.findAllWithHotelDetails();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private User resolveAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessRuleException("No authenticated user found");
        }
        return userService.getById(UUID.fromString(jwt.getSubject()));
    }
}

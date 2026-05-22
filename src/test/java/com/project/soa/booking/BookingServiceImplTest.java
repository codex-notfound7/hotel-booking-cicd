package com.project.soa.booking;

import com.project.soa.auth.user.User;
import com.project.soa.auth.user.UserInternalService;
import com.project.soa.availability_pricing.PricingService;
import com.project.soa.catalog.CatalogInternalService;
import com.project.soa.catalog.CatalogStatus;
import com.project.soa.catalog.Hotel;
import com.project.soa.catalog.RoomType;
import com.project.soa.common.exception.BusinessRuleException;
import com.project.soa.common.exception.ResourceNotFoundException;
import com.project.soa.notification.NotificationService;
import com.project.soa.payment.PaymentInternalService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock UserInternalService userService;
    @Mock BookingRepository bookingRepository;
    @Mock CatalogInternalService catalogInternalService;
    @Mock PricingService pricingService;
    @Mock NotificationService notificationService;
    @Mock PaymentInternalService paymentService;

    Clock fixedClock = Clock.fixed(Instant.parse("2025-06-01T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks BookingServiceImpl service;

    UUID guestId    = UUID.randomUUID();
    UUID managerId  = UUID.randomUUID();
    UUID roomTypeId = UUID.randomUUID();
    UUID bookingId  = UUID.randomUUID();

    User guest;
    User manager;
    Hotel hotel;
    RoomType roomType;

    @BeforeEach
    void setUp() {
        // Inject fixed clock via reflection since @InjectMocks picks constructor
        service = new BookingServiceImpl(
                userService, bookingRepository, catalogInternalService,
                pricingService, notificationService, paymentService, fixedClock);

        guest = user(guestId);
        manager = user(managerId);

        hotel = new Hotel();
        hotel.setId(UUID.randomUUID());
        hotel.setStatus(CatalogStatus.ACTIVE);
        hotel.setManager(manager);

        roomType = new RoomType();
        roomType.setId(roomTypeId);
        roomType.setCapacity(2);
        roomType.setTotalRooms(5);
        roomType.setStatus(CatalogStatus.ACTIVE);
        roomType.setHotel(hotel);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ── createBooking ─────────────────────────────────────────────────────────

    @Test
    void createBooking_happyPath_returnsPendingBooking() {
        authenticateAs(guestId, "GUEST");
        LocalDate in  = LocalDate.of(2025, 7, 1);
        LocalDate out = LocalDate.of(2025, 7, 5);

        when(userService.getById(guestId)).thenReturn(guest);
        when(catalogInternalService.getRoomTypeForUpdate(roomTypeId)).thenReturn(roomType);
        when(pricingService.isFullyBooked(roomTypeId, in, out, null)).thenReturn(false);
        when(pricingService.calculateTotalPrice(roomTypeId, in, out)).thenReturn(new BigDecimal("400.00"));
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(bookingId);
            return b;
        });

        Booking result = service.createBooking(dto(roomTypeId, in, out, 2));

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.getTotalPrice()).isEqualByComparingTo("400.00");
        assertThat(result.getPendingExpiresAt()).isNotNull();
    }

    @Test
    void createBooking_checkoutBeforeCheckin_throws() {
        authenticateAs(guestId, "GUEST");
        LocalDate in  = LocalDate.of(2025, 7, 5);
        LocalDate out = LocalDate.of(2025, 7, 1);

        assertThatThrownBy(() -> service.createBooking(dto(roomTypeId, in, out, 2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Check-out must be after check-in");
    }

    @Test
    void createBooking_roomTypeInactive_throws() {
        authenticateAs(guestId, "GUEST");
        roomType.setStatus(CatalogStatus.INACTIVE);
        when(userService.getById(guestId)).thenReturn(guest);
        when(catalogInternalService.getRoomTypeForUpdate(roomTypeId)).thenReturn(roomType);

        assertThatThrownBy(() -> service.createBooking(dto(roomTypeId, future(1), future(3), 2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not available for booking");
    }

    @Test
    void createBooking_hotelInactive_throws() {
        authenticateAs(guestId, "GUEST");
        hotel.setStatus(CatalogStatus.INACTIVE);
        when(userService.getById(guestId)).thenReturn(guest);
        when(catalogInternalService.getRoomTypeForUpdate(roomTypeId)).thenReturn(roomType);

        assertThatThrownBy(() -> service.createBooking(dto(roomTypeId, future(1), future(3), 2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not currently accepting bookings");
    }

    @Test
    void createBooking_capacityExceeded_throws() {
        authenticateAs(guestId, "GUEST");
        roomType.setCapacity(1);
        when(userService.getById(guestId)).thenReturn(guest);
        when(catalogInternalService.getRoomTypeForUpdate(roomTypeId)).thenReturn(roomType);

        assertThatThrownBy(() -> service.createBooking(dto(roomTypeId, future(1), future(3), 2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds room capacity");
    }

    @Test
    void createBooking_fullyBooked_throws() {
        authenticateAs(guestId, "GUEST");
        LocalDate in  = future(1);
        LocalDate out = future(3);
        when(userService.getById(guestId)).thenReturn(guest);
        when(catalogInternalService.getRoomTypeForUpdate(roomTypeId)).thenReturn(roomType);
        when(pricingService.isFullyBooked(roomTypeId, in, out, null)).thenReturn(true);

        assertThatThrownBy(() -> service.createBooking(dto(roomTypeId, in, out, 2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No rooms available");
    }

    // ── confirmBooking ────────────────────────────────────────────────────────

    @Test
    void confirmBooking_notTheManager_throws() {
        authenticateAs(UUID.randomUUID(), "MANAGER");
        when(userService.getById(any())).thenReturn(user(UUID.randomUUID()));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(pendingBooking()));

        assertThatThrownBy(() -> service.confirmBooking(bookingId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("do not manage");
    }

    @Test
    void confirmBooking_notPendingStatus_throws() {
        authenticateAs(managerId, "MANAGER");
        when(userService.getById(managerId)).thenReturn(manager);
        Booking b = pendingBooking();
        b.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.confirmBooking(bookingId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only PENDING");
    }

    @Test
    void confirmBooking_pendingExpired_throws() {
        authenticateAs(managerId, "MANAGER");
        when(userService.getById(managerId)).thenReturn(manager);
        Booking b = pendingBooking();
        // Expired 1 hour ago
        b.setPendingExpiresAt(LocalDateTime.now(fixedClock).minusHours(1));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.confirmBooking(bookingId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reservation expired");
    }

    @Test
    void confirmBooking_notPaid_throws() {
        authenticateAs(managerId, "MANAGER");
        when(userService.getById(managerId)).thenReturn(manager);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(pendingBooking()));
        when(paymentService.isBookingPaid(bookingId)).thenReturn(false);

        assertThatThrownBy(() -> service.confirmBooking(bookingId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not paid");
    }

    @Test
    void confirmBooking_success_sendsNotification() {
        authenticateAs(managerId, "MANAGER");
        when(userService.getById(managerId)).thenReturn(manager);
        Booking b = pendingBooking();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));
        when(paymentService.isBookingPaid(bookingId)).thenReturn(true);
        when(pricingService.isFullyBooked(any(), any(), any(), eq(bookingId))).thenReturn(false);
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking result = service.confirmBooking(bookingId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.getPendingExpiresAt()).isNull();
        verify(notificationService).sendBookingConfirmed(bookingId);
    }

    // ── cancelBooking ─────────────────────────────────────────────────────────

    @Test
    void cancelBooking_alreadyCancelled_throws() {
        Booking b = pendingBooking();
        b.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cancelBooking(bookingId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Already cancelled");
    }

    @Test
    void cancelBooking_onCheckinDay_throws() {
        Booking b = pendingBooking();
        // Check-in is today per fixed clock (2025-06-01)
        b.setCheckIn(LocalDate.of(2025, 6, 1));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cancelBooking(bookingId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("on or after check-in");
    }

    @Test
    void cancelBooking_sevenPlusDaysBefore_fullRefund() {
        Booking b = pendingBooking();
        b.setCheckIn(LocalDate.of(2025, 6, 10)); // 9 days from fixedClock date
        b.setTotalPrice(new BigDecimal("500.00"));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any())).thenReturn(b);

        CancellationResultDto result = service.cancelBooking(bookingId);

        assertThat(result.refundAmount()).isEqualByComparingTo("500.00");
        assertThat(result.penaltyAmount()).isEqualByComparingTo("0.00");
        assertThat(result.policyApplied()).contains("Full refund");
        verify(notificationService).sendBookingCancelled(bookingId);
    }

    @Test
    void cancelBooking_threeToSixDaysBefore_fiftyPercentRefund() {
        Booking b = pendingBooking();
        b.setCheckIn(LocalDate.of(2025, 6, 5)); // 4 days from fixedClock date
        b.setTotalPrice(new BigDecimal("200.00"));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any())).thenReturn(b);

        CancellationResultDto result = service.cancelBooking(bookingId);

        assertThat(result.refundAmount()).isEqualByComparingTo("100.00");
        assertThat(result.penaltyAmount()).isEqualByComparingTo("100.00");
        assertThat(result.policyApplied()).contains("50%");
    }

    @Test
    void cancelBooking_lessThanThreeDaysBefore_noRefund() {
        Booking b = pendingBooking();
        b.setCheckIn(LocalDate.of(2025, 6, 3)); // 2 days from fixedClock date
        b.setTotalPrice(new BigDecimal("300.00"));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any())).thenReturn(b);

        CancellationResultDto result = service.cancelBooking(bookingId);

        assertThat(result.refundAmount()).isEqualByComparingTo("0.00");
        assertThat(result.penaltyAmount()).isEqualByComparingTo("300.00");
        assertThat(result.policyApplied()).contains("No refund");
    }

    // ── BookingInternalService ────────────────────────────────────────────────

    @Test
    void findAllWithHotelDetails_delegatesToRepository() {
        Booking b = pendingBooking();
        when(bookingRepository.findAllWithHotelDetails()).thenReturn(List.of(b));

        assertThat(service.findAllWithHotelDetails()).containsExactly(b);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Booking pendingBooking() {
        Booking b = new Booking();
        b.setId(bookingId);
        b.setStatus(BookingStatus.PENDING);
        b.setCheckIn(LocalDate.of(2025, 7, 1));
        b.setCheckOut(LocalDate.of(2025, 7, 5));
        b.setTotalPrice(new BigDecimal("400.00"));
        b.setPendingExpiresAt(LocalDateTime.now(fixedClock).plusMinutes(15));
        b.setRoomType(roomType);
        b.setUser(guest);
        return b;
    }

    private CreateBookingRequestDto dto(UUID rtId, LocalDate in, LocalDate out, int guests) {
        CreateBookingRequestDto dto = new CreateBookingRequestDto();
        dto.setRoomTypeId(rtId);
        dto.setCheckIn(in);
        dto.setCheckOut(out);
        dto.setNumberOfGuests(guests);
        return dto;
    }

    private LocalDate future(int days) {
        return LocalDate.now(fixedClock).plusDays(days);
    }

    private User user(UUID id) {
        User u = new User();
        u.setId(id);
        u.setEmail(id + "@test.com");
        return u;
    }

    private void authenticateAs(UUID userId, String role) {
        Jwt jwt = Jwt.withTokenValue("tok")
                .header("alg", "none")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}

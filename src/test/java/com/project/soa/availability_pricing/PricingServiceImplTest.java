package com.project.soa.availability_pricing;

import com.project.soa.booking.Booking;
import com.project.soa.booking.BookingRepository;
import com.project.soa.booking.BookingStatus;
import com.project.soa.catalog.CatalogStatus;
import com.project.soa.catalog.RoomType;
import com.project.soa.catalog.RoomTypeRepository;
import com.project.soa.common.exception.BusinessRuleException;
import com.project.soa.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingServiceImplTest {

    @Mock RoomTypeRepository roomTypeRepository;
    @Mock BookingRepository bookingRepository;

    @InjectMocks PricingServiceImpl service;

    UUID roomTypeId = UUID.randomUUID();

    // ── checkAvailability ─────────────────────────────────────────────────────

    @Test
    void checkAvailability_checkoutBeforeCheckin_returnsUnavailable() {
        AvailabilityCheckRequestDto req = req(LocalDate.of(2025, 7, 5), LocalDate.of(2025, 7, 1), 2);

        AvailabilityCheckResponseDto result = service.checkAvailability(req);

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getMessage()).contains("Check-out must be after check-in");
        verifyNoInteractions(roomTypeRepository);
    }

    @Test
    void checkAvailability_inactiveRoomType_returnsUnavailable() {
        RoomType rt = roomType(1, 5);
        rt.setStatus(CatalogStatus.INACTIVE);
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(rt));

        AvailabilityCheckResponseDto result = service.checkAvailability(
                req(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 3), 1));

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getMessage()).contains("not currently available");
    }

    @Test
    void checkAvailability_capacityInsufficient_returnsUnavailable() {
        RoomType rt = roomType(1, 5);
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(rt));

        AvailabilityCheckResponseDto result = service.checkAvailability(
                req(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 3), 3));

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getMessage()).contains("capacity");
    }

    @Test
    void checkAvailability_allRoomsBooked_returnsUnavailable() {
        RoomType rt = roomType(2, 1); // capacity 2, totalRooms 1
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(rt));
        when(bookingRepository.findConfirmedBookingsForRoomTypeInRange(any(), any(), any()))
                .thenReturn(List.of(new Booking()));

        AvailabilityCheckResponseDto result = service.checkAvailability(
                req(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 3), 2));

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getMessage()).contains("No rooms available");
    }

    @Test
    void checkAvailability_roomsAvailable_returnsAvailableWithPrice() {
        RoomType rt = roomType(2, 5);
        rt.setBasePrice(new BigDecimal("100.00"));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(rt));
        when(bookingRepository.findConfirmedBookingsForRoomTypeInRange(any(), any(), any()))
                .thenReturn(List.of());

        // Two weekdays: Mon 2025-06-02 + Tue 2025-06-03 → base price each
        AvailabilityCheckResponseDto result = service.checkAvailability(
                req(LocalDate.of(2025, 6, 2), LocalDate.of(2025, 6, 4), 1));

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getCalculatedPrice()).isGreaterThan(BigDecimal.ZERO);
    }

    // ── calculateTotalPrice ───────────────────────────────────────────────────

    @Test
    void calculateTotalPrice_weekdays_returnsBasePrice() {
        RoomType rt = roomType(2, 5);
        rt.setBasePrice(new BigDecimal("100.00"));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(rt));

        // Monday + Tuesday (2 weekdays, no multiplier)
        BigDecimal price = service.calculateTotalPrice(
                roomTypeId, LocalDate.of(2025, 6, 2), LocalDate.of(2025, 6, 4));

        assertThat(price).isEqualByComparingTo("200.00");
    }

    @Test
    void calculateTotalPrice_weekend_appliesMultiplier() {
        RoomType rt = roomType(2, 5);
        rt.setBasePrice(new BigDecimal("100.00"));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(rt));

        // Saturday 2025-06-07 only (1 night, weekend multiplier 1.2)
        BigDecimal price = service.calculateTotalPrice(
                roomTypeId, LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 8));

        assertThat(price).isEqualByComparingTo("120.00");
    }

    @Test
    void calculateTotalPrice_peakSeason_appliesMultiplier() {
        RoomType rt = roomType(2, 5);
        rt.setBasePrice(new BigDecimal("100.00"));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(rt));

        // Dec 24 (weekday peak): weekday × peak(1.3)
        BigDecimal price = service.calculateTotalPrice(
                roomTypeId, LocalDate.of(2025, 12, 24), LocalDate.of(2025, 12, 25));

        assertThat(price).isGreaterThan(new BigDecimal("100.00"));
    }

    @Test
    void calculateTotalPrice_summerMonths_appliesSummerMultiplier() {
        RoomType rt = roomType(2, 5);
        rt.setBasePrice(new BigDecimal("100.00"));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(rt));

        // Monday in July → summer(1.2) but not weekend
        BigDecimal price = service.calculateTotalPrice(
                roomTypeId, LocalDate.of(2025, 7, 7), LocalDate.of(2025, 7, 8));

        assertThat(price).isEqualByComparingTo("120.00");
    }

    // ── isFullyBooked ─────────────────────────────────────────────────────────

    @Test
    void isFullyBooked_belowCapacity_returnsFalse() {
        RoomType rt = roomType(2, 3);
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(rt));
        when(bookingRepository.findBlockingBookingsForRoomTypeInRange(
                any(), any(), any(), any())).thenReturn(List.of(confirmedBooking(), confirmedBooking()));

        assertThat(service.isFullyBooked(roomTypeId, LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 5), null))
                .isFalse();
    }

    @Test
    void isFullyBooked_atCapacity_returnsTrue() {
        RoomType rt = roomType(2, 2);
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(rt));
        when(bookingRepository.findBlockingBookingsForRoomTypeInRange(
                any(), any(), any(), any())).thenReturn(List.of(confirmedBooking(), confirmedBooking()));

        assertThat(service.isFullyBooked(roomTypeId, LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 5), null))
                .isTrue();
    }

    @Test
    void isFullyBooked_withExcludedBooking_excludesItFromCount() {
        UUID excludedId = UUID.randomUUID();
        Booking excluded = confirmedBooking();
        excluded.setId(excludedId);

        RoomType rt = roomType(2, 1);
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(rt));
        // Only the excluded booking overlaps — so 0 blocking after exclusion
        when(bookingRepository.findBlockingBookingsForRoomTypeInRange(
                any(), any(), any(), any())).thenReturn(List.of(excluded));

        assertThat(service.isFullyBooked(
                roomTypeId, LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 5), excludedId))
                .isFalse();
    }

    // ── calculatePrice (detailed breakdown DTO) ───────────────────────────────

    @Test
    void calculatePrice_checkoutBeforeCheckin_throwsBusinessRule() {
        PriceCalculationRequestDto dto = new PriceCalculationRequestDto();
        dto.setRoomTypeId(roomTypeId);
        dto.setCheckIn(LocalDate.of(2025, 7, 5));
        dto.setCheckOut(LocalDate.of(2025, 7, 1));

        assertThatThrownBy(() -> service.calculatePrice(dto))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void calculatePrice_validDates_returnsBreakdown() {
        RoomType rt = roomType(2, 5);
        rt.setBasePrice(new BigDecimal("100.00"));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(rt));

        PriceCalculationRequestDto dto = new PriceCalculationRequestDto();
        dto.setRoomTypeId(roomTypeId);
        dto.setCheckIn(LocalDate.of(2025, 6, 2));
        dto.setCheckOut(LocalDate.of(2025, 6, 4));

        PriceCalculationResponseDto result = service.calculatePrice(dto);

        assertThat(result.getFinalPrice()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.getNumberOfNights()).isEqualTo(2);
        assertThat(result.getBreakdown()).isNotBlank();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RoomType roomType(int capacity, int totalRooms) {
        RoomType rt = new RoomType();
        rt.setId(roomTypeId);
        rt.setCapacity(capacity);
        rt.setTotalRooms(totalRooms);
        rt.setBasePrice(new BigDecimal("100.00"));
        rt.setStatus(CatalogStatus.ACTIVE);
        return rt;
    }

    private AvailabilityCheckRequestDto req(LocalDate in, LocalDate out, int guests) {
        AvailabilityCheckRequestDto dto = new AvailabilityCheckRequestDto();
        dto.setRoomTypeId(roomTypeId);
        dto.setCheckIn(in);
        dto.setCheckOut(out);
        dto.setNumberOfGuests(guests);
        return dto;
    }

    private Booking confirmedBooking() {
        Booking b = new Booking();
        b.setId(UUID.randomUUID());
        b.setStatus(BookingStatus.CONFIRMED);
        b.setPendingExpiresAt(LocalDateTime.now().plusHours(1));
        return b;
    }
}

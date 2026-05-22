package com.project.soa.availability_pricing;

import com.project.soa.booking.BookingRepository;
import com.project.soa.catalog.CatalogStatus;
import com.project.soa.catalog.RoomType;
import com.project.soa.catalog.RoomTypeRepository;
import com.project.soa.common.exception.BusinessRuleException;
import com.project.soa.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PricingServiceImpl implements PricingService {

    private static final BigDecimal WEEKEND_MULTIPLIER     = new BigDecimal("1.2");
    private static final BigDecimal PEAK_SEASON_MULTIPLIER = new BigDecimal("1.3");
    private static final BigDecimal SUMMER_MULTIPLIER      = new BigDecimal("1.2");

    private final RoomTypeRepository roomTypeRepository;
    private final BookingRepository bookingRepository;

    public PricingServiceImpl(RoomTypeRepository roomTypeRepository,
                              BookingRepository bookingRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.bookingRepository  = bookingRepository;
    }

    @Override
    public AvailabilityCheckResponseDto checkAvailability(AvailabilityCheckRequestDto request) {
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            return AvailabilityCheckResponseDto.unavailable(
                    request.getRoomTypeId(), "Check-out must be after check-in");
        }

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", request.getRoomTypeId()));

        if (roomType.getStatus() != CatalogStatus.ACTIVE) {
            return AvailabilityCheckResponseDto.unavailable(
                    request.getRoomTypeId(), "This room type is not currently available");
        }

        if (roomType.getCapacity() < request.getNumberOfGuests()) {
            return AvailabilityCheckResponseDto.unavailable(request.getRoomTypeId(),
                    "Room capacity (" + roomType.getCapacity() + ") is less than number of guests ("
                            + request.getNumberOfGuests() + ")");
        }

        // Overlap rule: [checkIn, checkOut) overlaps if existing.checkIn < checkOut && existing.checkOut > checkIn
        long confirmedOverlapping = bookingRepository.findConfirmedBookingsForRoomTypeInRange(
                request.getRoomTypeId(), request.getCheckIn(), request.getCheckOut()
        ).size();

        if (confirmedOverlapping >= roomType.getTotalRooms()) {
            return AvailabilityCheckResponseDto.unavailable(
                    request.getRoomTypeId(), "No rooms available for the selected dates");
        }

        BigDecimal price = calculateTotalPrice(
                request.getRoomTypeId(), request.getCheckIn(), request.getCheckOut()
        );

        return AvailabilityCheckResponseDto.available(request.getRoomTypeId(), price);
    }

    @Override
    public PriceCalculationResponseDto calculatePrice(PriceCalculationRequestDto request) {
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new BusinessRuleException("Check-out must be after check-in");
        }

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", request.getRoomTypeId()));

        BigDecimal basePrice = roomType.getBasePrice();
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(
                request.getCheckIn(), request.getCheckOut());

        BigDecimal finalPrice = BigDecimal.ZERO;
        StringBuilder breakdown = new StringBuilder();

        for (LocalDate d = request.getCheckIn(); d.isBefore(request.getCheckOut()); d = d.plusDays(1)) {
            BigDecimal dayPrice = getDayPrice(d, basePrice);
            finalPrice = finalPrice.add(dayPrice);

            String reason = getDayPriceReason(d);
            if (breakdown.length() > 0) breakdown.append("; ");
            breakdown.append(d).append(": ").append(dayPrice);
            if (!reason.isEmpty()) breakdown.append(" (").append(reason).append(")");
        }

        return PricingMapper.toDto(basePrice, finalPrice, nights, breakdown.toString());
    }

    @Override
    public BigDecimal calculateTotalPrice(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", roomTypeId));

        BigDecimal total = BigDecimal.ZERO;

        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            total = total.add(getDayPrice(d, roomType.getBasePrice()));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean isFullyBooked(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut,
                                 UUID excludeBookingId) {

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", roomTypeId));

        // Count bookings that block inventory: CONFIRMED, or unexpired PENDING reservations
        long blockingOverlapping = bookingRepository.findBlockingBookingsForRoomTypeInRange(
                        roomTypeId, checkIn, checkOut, LocalDateTime.now()
                ).stream()
                .filter(b -> excludeBookingId == null || !b.getId().equals(excludeBookingId))
                .count();

        return blockingOverlapping >= roomType.getTotalRooms();
    }


    private BigDecimal getDayPrice(LocalDate date, BigDecimal basePrice) {
        BigDecimal multiplier = BigDecimal.ONE;

        if (isWeekend(date))     multiplier = multiplier.multiply(WEEKEND_MULTIPLIER);
        if (isPeakSeason(date))  multiplier = multiplier.multiply(PEAK_SEASON_MULTIPLIER);
        else if (isSummer(date)) multiplier = multiplier.multiply(SUMMER_MULTIPLIER);

        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private String getDayPriceReason(LocalDate date) {
        StringBuilder reason = new StringBuilder();

        if (isWeekend(date))     reason.append("weekend");
        if (isPeakSeason(date))  reason.append(reason.length() > 0 ? "+peak" : "peak season");
        else if (isSummer(date)) reason.append(reason.length() > 0 ? "+summer" : "summer");

        return reason.toString();
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6;
    }

    private boolean isPeakSeason(LocalDate date) {
        return (date.getMonth() == Month.DECEMBER && date.getDayOfMonth() >= 20)
                || (date.getMonth() == Month.JANUARY && date.getDayOfMonth() <= 5);
    }

    private boolean isSummer(LocalDate date) {
        return date.getMonth() == Month.JULY || date.getMonth() == Month.AUGUST;
    }
}
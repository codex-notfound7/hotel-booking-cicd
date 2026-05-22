package com.project.soa.booking;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    Booking createBooking(CreateBookingRequestDto dto);

    Booking confirmBooking(UUID bookingId);

    CancellationResultDto cancelBooking(UUID bookingId);

    List<BookingResponseDto> getMyBookings();

    Booking getBookingById(UUID id);

    List<BookingResponseDto> getUpcomingBookingsForManager();

    List<BookingResponseDto> getAllBookings();
}
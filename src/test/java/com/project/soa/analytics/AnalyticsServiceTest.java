package com.project.soa.analytics;

import com.project.soa.auth.user.UserInternalService;
import com.project.soa.booking.Booking;
import com.project.soa.booking.BookingInternalService;
import com.project.soa.booking.BookingStatus;
import com.project.soa.catalog.CatalogInternalService;
import com.project.soa.catalog.Hotel;
import com.project.soa.catalog.RoomType;
import com.project.soa.review.ReviewInternalService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock BookingInternalService bookingInternalService;
    @Mock UserInternalService userInternalService;
    @Mock CatalogInternalService catalogInternalService;
    @Mock ReviewInternalService reviewInternalService;

    @InjectMocks AnalyticsService analyticsService;

    @Test
    void generateReport_aggregatesDataFromAllModules() {
        Hotel hotel = new Hotel();
        hotel.setId(UUID.randomUUID());
        hotel.setName("Sea View Hotel");

        RoomType rt = new RoomType();
        rt.setId(UUID.randomUUID());
        rt.setHotel(hotel);

        Booking confirmed = booking(rt, BookingStatus.CONFIRMED, new BigDecimal("500.00"));
        Booking cancelled = booking(rt, BookingStatus.CANCELLED, new BigDecimal("200.00"));
        Booking pending   = booking(rt, BookingStatus.PENDING,   new BigDecimal("300.00"));

        when(bookingInternalService.findAllWithHotelDetails())
                .thenReturn(List.of(confirmed, cancelled, pending));
        when(userInternalService.count()).thenReturn(50L);
        when(catalogInternalService.countHotels()).thenReturn(10L);
        when(reviewInternalService.countReviews()).thenReturn(25L);
        when(reviewInternalService.getOverallAverageRating()).thenReturn(4.2);

        AnalyticsReportDto report = analyticsService.generateReport();

        assertThat(report.getTotalBookings()).isEqualTo(3);
        assertThat(report.getConfirmedBookings()).isEqualTo(1);
        assertThat(report.getCancelledBookings()).isEqualTo(1);
        assertThat(report.getPendingBookings()).isEqualTo(1);
        assertThat(report.getTotalRevenue()).isEqualByComparingTo("500.00");
        assertThat(report.getAverageBookingValue()).isEqualByComparingTo("500.00");
        assertThat(report.getTotalUsers()).isEqualTo(50);
        assertThat(report.getTotalHotels()).isEqualTo(10);
        assertThat(report.getTotalReviews()).isEqualTo(25);
        assertThat(report.getAverageReviewRating()).isEqualTo(4.2);
        assertThat(report.getTopHotelsByBookings()).containsKey("Sea View Hotel");
    }

    @Test
    void generateReport_noBookings_returnsZeroedReport() {
        when(bookingInternalService.findAllWithHotelDetails()).thenReturn(List.of());
        when(userInternalService.count()).thenReturn(0L);
        when(catalogInternalService.countHotels()).thenReturn(0L);
        when(reviewInternalService.countReviews()).thenReturn(0L);
        when(reviewInternalService.getOverallAverageRating()).thenReturn(0.0);

        AnalyticsReportDto report = analyticsService.generateReport();

        assertThat(report.getTotalBookings()).isEqualTo(0);
        assertThat(report.getTotalRevenue()).isEqualByComparingTo("0.00");
        assertThat(report.getAverageBookingValue()).isEqualByComparingTo("0.00");
    }

    @Test
    void generateReport_usesInternalServicesOnly_neverAccessesForeignRepos() {
        when(bookingInternalService.findAllWithHotelDetails()).thenReturn(List.of());
        when(userInternalService.count()).thenReturn(1L);
        when(catalogInternalService.countHotels()).thenReturn(1L);
        when(reviewInternalService.countReviews()).thenReturn(1L);
        when(reviewInternalService.getOverallAverageRating()).thenReturn(0.0);

        analyticsService.generateReport();

        // All data comes through the typed internal services — no extra interactions
        verify(bookingInternalService).findAllWithHotelDetails();
        verify(userInternalService).count();
        verify(catalogInternalService).countHotels();
        verify(reviewInternalService).countReviews();
        verify(reviewInternalService).getOverallAverageRating();
        verifyNoMoreInteractions(
                bookingInternalService, userInternalService,
                catalogInternalService, reviewInternalService);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Booking booking(RoomType rt, BookingStatus status, BigDecimal price) {
        Booking b = new Booking();
        b.setId(UUID.randomUUID());
        b.setRoomType(rt);
        b.setStatus(status);
        b.setTotalPrice(price);
        b.setCheckIn(LocalDate.now().plusDays(5));
        b.setCheckOut(LocalDate.now().plusDays(7));
        b.setCreatedAt(LocalDateTime.now());
        return b;
    }
}

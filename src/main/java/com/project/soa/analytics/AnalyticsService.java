package com.project.soa.analytics;

import com.project.soa.auth.user.UserInternalService;
import com.project.soa.booking.Booking;
import com.project.soa.booking.BookingInternalService;
import com.project.soa.catalog.CatalogInternalService;
import com.project.soa.review.ReviewInternalService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final BookingInternalService bookingInternalService;
    private final UserInternalService    userInternalService;
    private final CatalogInternalService catalogInternalService;
    private final ReviewInternalService  reviewInternalService;

    public AnalyticsService(BookingInternalService bookingInternalService,
                            UserInternalService    userInternalService,
                            CatalogInternalService catalogInternalService,
                            ReviewInternalService  reviewInternalService) {
        this.bookingInternalService = bookingInternalService;
        this.userInternalService    = userInternalService;
        this.catalogInternalService = catalogInternalService;
        this.reviewInternalService  = reviewInternalService;
    }

    public AnalyticsReportDto generateReport() {
        List<Booking> allBookings = bookingInternalService.findAllWithHotelDetails();

        return AnalyticsMapper.toReportDto(
                allBookings,
                userInternalService.count(),
                catalogInternalService.countHotels(),
                reviewInternalService.countReviews(),
                reviewInternalService.getOverallAverageRating()
        );
    }
}

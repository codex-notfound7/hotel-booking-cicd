package com.project.soa.analytics;


import com.project.soa.booking.Booking;
import com.project.soa.booking.BookingStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AnalyticsMapper {

    private AnalyticsMapper() {}

    public static AnalyticsReportDto toReportDto(
            List<Booking> allBookings,
            long totalUsers,
            long totalHotels,
            long totalReviews,
            double avgRating) {

        long confirmed = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long cancelled = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        long pending   = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();

        BigDecimal totalRevenue = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgValue = confirmed > 0
                ? totalRevenue.divide(BigDecimal.valueOf(confirmed), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Long> topHotels = allBookings.stream()
                .filter(b -> b.getRoomType() != null && b.getRoomType().getHotel() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getRoomType().getHotel().getName(),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

        Map<String, BigDecimal> revenueByHotel = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        && b.getRoomType() != null && b.getRoomType().getHotel() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getRoomType().getHotel().getName(),
                        Collectors.reducing(BigDecimal.ZERO,
                                b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO,
                                BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> bookingsPerMonth = allBookings.stream()
                .filter(b -> b.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getCreatedAt().format(fmt),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

        AnalyticsReportDto report = new AnalyticsReportDto();
        report.setTotalUsers(totalUsers);
        report.setTotalHotels(totalHotels);
        report.setTotalBookings(allBookings.size());
        report.setConfirmedBookings(confirmed);
        report.setCancelledBookings(cancelled);
        report.setPendingBookings(pending);
        report.setTotalRevenue(totalRevenue);
        report.setAverageBookingValue(avgValue);
        report.setTotalReviews(totalReviews);
        report.setAverageReviewRating(Math.round(avgRating * 10.0) / 10.0);
        report.setTopHotelsByBookings(topHotels);
        report.setRevenueByHotel(revenueByHotel);
        report.setBookingsPerMonth(bookingsPerMonth);
        return report;
    }
}
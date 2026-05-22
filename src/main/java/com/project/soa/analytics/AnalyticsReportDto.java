package com.project.soa.analytics;

import java.math.BigDecimal;
import java.util.Map;

public class AnalyticsReportDto {

    private long totalUsers;
    private long totalHotels;
    private long totalBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private long pendingBookings;
    private BigDecimal totalRevenue;
    private BigDecimal averageBookingValue;
    private long totalReviews;
    private double averageReviewRating;
    private Map<String, Long> topHotelsByBookings;
    private Map<String, BigDecimal> revenueByHotel;
    private Map<String, Long> bookingsPerMonth;

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    public long getTotalHotels() { return totalHotels; }
    public void setTotalHotels(long totalHotels) { this.totalHotels = totalHotels; }
    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }
    public long getConfirmedBookings() { return confirmedBookings; }
    public void setConfirmedBookings(long confirmedBookings) { this.confirmedBookings = confirmedBookings; }
    public long getCancelledBookings() { return cancelledBookings; }
    public void setCancelledBookings(long cancelledBookings) { this.cancelledBookings = cancelledBookings; }
    public long getPendingBookings() { return pendingBookings; }
    public void setPendingBookings(long pendingBookings) { this.pendingBookings = pendingBookings; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public BigDecimal getAverageBookingValue() { return averageBookingValue; }
    public void setAverageBookingValue(BigDecimal averageBookingValue) { this.averageBookingValue = averageBookingValue; }
    public long getTotalReviews() { return totalReviews; }
    public void setTotalReviews(long totalReviews) { this.totalReviews = totalReviews; }
    public double getAverageReviewRating() { return averageReviewRating; }
    public void setAverageReviewRating(double averageReviewRating) { this.averageReviewRating = averageReviewRating; }
    public Map<String, Long> getTopHotelsByBookings() { return topHotelsByBookings; }
    public void setTopHotelsByBookings(Map<String, Long> topHotelsByBookings) { this.topHotelsByBookings = topHotelsByBookings; }
    public Map<String, Long> getBookingsPerMonth() { return bookingsPerMonth; }
    public void setBookingsPerMonth(Map<String, Long> bookingsPerMonth) { this.bookingsPerMonth = bookingsPerMonth; }
    public Map<String, BigDecimal> getRevenueByHotel() { return revenueByHotel; }
    public void setRevenueByHotel(Map<String, BigDecimal> revenueByHotel) { this.revenueByHotel = revenueByHotel; }
}
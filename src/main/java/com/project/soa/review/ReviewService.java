package com.project.soa.review;



import java.util.List;
import java.util.UUID;

public interface ReviewService {
    ReviewResponseDto createReview(UUID hotelId, ReviewRequestDto dto);
    List<ReviewResponseDto> getVisibleReviewsForHotel(UUID hotelId);
    // Admin operations
    List<ReviewResponseDto> getAllReviews();
    List<ReviewResponseDto> getAllReviewsForHotel(UUID hotelId);
    ReviewResponseDto hideReview(UUID reviewId);
    ReviewResponseDto showReview(UUID reviewId);
    void deleteReview(UUID reviewId);
}
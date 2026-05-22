package com.project.soa.review;

import com.project.soa.auth.user.User;
import com.project.soa.auth.user.UserInternalService;
import com.project.soa.catalog.CatalogInternalService;
import com.project.soa.catalog.Hotel;
import com.project.soa.common.exception.BusinessRuleException;
import com.project.soa.common.exception.ResourceNotFoundException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService, ReviewInternalService {

    private final ReviewRepository reviewRepository;
    private final UserInternalService userService;
    private final CatalogInternalService catalogInternalService;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             UserInternalService userService,
                             CatalogInternalService catalogInternalService) {
        this.reviewRepository       = reviewRepository;
        this.userService            = userService;
        this.catalogInternalService = catalogInternalService;
    }

    @Override
    public ReviewResponseDto createReview(UUID hotelId, ReviewRequestDto dto) {
        User user = resolveAuthenticatedUser();
        Hotel hotel = catalogInternalService.findHotelById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", hotelId));
        if (reviewRepository.findByUserIdAndHotelId(user.getId(), hotelId).isPresent()) {
            throw new BusinessRuleException("You have already submitted a review for this hotel");
        }
        Review review = new Review();
        review.setUser(user);
        review.setHotel(hotel);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        return ReviewMapper.toDto(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getVisibleReviewsForHotel(UUID hotelId) {
        catalogInternalService.findHotelById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", hotelId));
        return reviewRepository.findByHotelIdAndStatus(hotelId, ReviewStatus.VISIBLE)
                .stream().map(ReviewMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getAllReviews() {
        return reviewRepository.findAllWithDetails().stream().map(ReviewMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getAllReviewsForHotel(UUID hotelId) {
        catalogInternalService.findHotelById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", hotelId));
        return reviewRepository.findAllByHotelId(hotelId).stream().map(ReviewMapper::toDto).toList();
    }

    @Override
    public ReviewResponseDto hideReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        review.setStatus(ReviewStatus.HIDDEN);
        return ReviewMapper.toDto(reviewRepository.save(review));
    }

    @Override
    public ReviewResponseDto showReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        review.setStatus(ReviewStatus.VISIBLE);
        return ReviewMapper.toDto(reviewRepository.save(review));
    }

    @Override
    public void deleteReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        reviewRepository.delete(review);
    }

    // ── ReviewInternalService ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public long countReviews() {
        return reviewRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public double getOverallAverageRating() {
        Double raw = reviewRepository.getOverallAverageRating();
        return raw != null ? raw : 0.0;
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

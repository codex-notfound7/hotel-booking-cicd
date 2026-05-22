package com.project.soa.review;

import com.project.soa.auth.user.User;
import com.project.soa.auth.user.UserInternalService;
import com.project.soa.catalog.CatalogInternalService;
import com.project.soa.catalog.Hotel;
import com.project.soa.common.exception.BusinessRuleException;
import com.project.soa.common.exception.ResourceNotFoundException;

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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock ReviewRepository reviewRepository;
    @Mock UserInternalService userService;
    @Mock CatalogInternalService catalogInternalService;

    @InjectMocks ReviewServiceImpl service;

    UUID userId  = UUID.randomUUID();
    UUID hotelId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();

    User user;
    Hotel hotel;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(userId);
        user.setEmail("guest@test.com");

        hotel = new Hotel();
        hotel.setId(hotelId);
        hotel.setName("Test Hotel");

        authenticateAs(userId);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ── createReview ──────────────────────────────────────────────────────────

    @Test
    void createReview_hotelNotFound_throwsResourceNotFound() {
        when(userService.getById(userId)).thenReturn(user);
        when(catalogInternalService.findHotelById(hotelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createReview(hotelId, reviewDto(5, "Great!")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createReview_duplicateReview_throwsBusinessRule() {
        when(userService.getById(userId)).thenReturn(user);
        when(catalogInternalService.findHotelById(hotelId)).thenReturn(Optional.of(hotel));
        when(reviewRepository.findByUserIdAndHotelId(userId, hotelId))
                .thenReturn(Optional.of(new Review()));

        assertThatThrownBy(() -> service.createReview(hotelId, reviewDto(4, "Nice")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already submitted");
    }

    @Test
    void createReview_success_savesAndReturnsDto() {
        when(userService.getById(userId)).thenReturn(user);
        when(catalogInternalService.findHotelById(hotelId)).thenReturn(Optional.of(hotel));
        when(reviewRepository.findByUserIdAndHotelId(userId, hotelId)).thenReturn(Optional.empty());
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(reviewId);
            return r;
        });

        ReviewResponseDto result = service.createReview(hotelId, reviewDto(5, "Excellent stay!"));

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("Excellent stay!");
        verify(reviewRepository).save(any(Review.class));
    }

    // ── getVisibleReviewsForHotel ─────────────────────────────────────────────

    @Test
    void getVisibleReviewsForHotel_hotelNotFound_throwsResourceNotFound() {
        when(catalogInternalService.findHotelById(hotelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVisibleReviewsForHotel(hotelId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getVisibleReviewsForHotel_returnsOnlyVisibleReviews() {
        Review visible = review(ReviewStatus.VISIBLE);
        when(catalogInternalService.findHotelById(hotelId)).thenReturn(Optional.of(hotel));
        when(reviewRepository.findByHotelIdAndStatus(hotelId, ReviewStatus.VISIBLE))
                .thenReturn(List.of(visible));

        List<ReviewResponseDto> result = service.getVisibleReviewsForHotel(hotelId);

        assertThat(result).hasSize(1);
    }

    // ── hideReview / showReview ───────────────────────────────────────────────

    @Test
    void hideReview_found_setsStatusHidden() {
        Review r = review(ReviewStatus.VISIBLE);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(r));
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponseDto result = service.hideReview(reviewId);

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.HIDDEN.name());
    }

    @Test
    void hideReview_notFound_throwsResourceNotFound() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.hideReview(reviewId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void showReview_found_setsStatusVisible() {
        Review r = review(ReviewStatus.HIDDEN);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(r));
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponseDto result = service.showReview(reviewId);

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.VISIBLE.name());
    }

    // ── deleteReview ──────────────────────────────────────────────────────────

    @Test
    void deleteReview_found_callsDelete() {
        Review r = review(ReviewStatus.VISIBLE);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(r));

        service.deleteReview(reviewId);

        verify(reviewRepository).delete(r);
    }

    @Test
    void deleteReview_notFound_throwsResourceNotFound() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteReview(reviewId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── ReviewInternalService ─────────────────────────────────────────────────

    @Test
    void countReviews_delegatesToRepository() {
        when(reviewRepository.count()).thenReturn(17L);

        assertThat(service.countReviews()).isEqualTo(17L);
    }

    @Test
    void getOverallAverageRating_withReviews_returnsValue() {
        when(reviewRepository.getOverallAverageRating()).thenReturn(4.3);

        assertThat(service.getOverallAverageRating()).isEqualTo(4.3);
    }

    @Test
    void getOverallAverageRating_noReviews_returnsZero() {
        when(reviewRepository.getOverallAverageRating()).thenReturn(null);

        assertThat(service.getOverallAverageRating()).isEqualTo(0.0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ReviewRequestDto reviewDto(int rating, String comment) {
        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setRating(rating);
        dto.setComment(comment);
        return dto;
    }

    private Review review(ReviewStatus status) {
        Review r = new Review();
        r.setId(reviewId);
        r.setRating(4);
        r.setComment("Good");
        r.setStatus(status);
        r.setUser(user);
        r.setHotel(hotel);
        return r;
    }

    private void authenticateAs(UUID id) {
        Jwt jwt = Jwt.withTokenValue("tok")
                .header("alg", "none")
                .subject(id.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_GUEST"))));
    }
}

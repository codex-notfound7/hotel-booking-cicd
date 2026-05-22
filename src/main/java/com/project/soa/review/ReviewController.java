package com.project.soa.review;

import com.project.soa.common.exception.ErrorResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Reviews", description = "Hotel reviews — write, read, and manager moderation")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // ── Public / Guest endpoints ─────────────────────────────────────────────

    @PostMapping("/api/hotels/{hotelId}/reviews")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit a review",
            description = "Authenticated users can submit one review per hotel. " +
                    "Reviewer is determined from the JWT — userId is not accepted in the request body.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review submitted successfully",
                    content = @Content(schema = @Schema(implementation = ReviewResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g. missing rating or comment)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "User has already submitted a review for this hotel",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<ReviewResponseDto> createReview(
            @PathVariable UUID hotelId,
            @Valid @RequestBody ReviewRequestDto dto,
            UriComponentsBuilder uriBuilder) {
        ReviewResponseDto response = reviewService.createReview(hotelId, dto);
        return ResponseEntity.created(
                        uriBuilder.path("/api/hotels/{hotelId}/reviews").buildAndExpand(hotelId).toUri())
                .body(response);
    }

    @GetMapping("/api/hotels/{hotelId}/reviews")
    @Operation(summary = "Get reviews for a hotel",
            description = "Public. Returns all VISIBLE reviews for the hotel.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of visible reviews returned successfully"),
            @ApiResponse(responseCode = "404", description = "Hotel not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public List<ReviewResponseDto> getHotelReviews(@PathVariable UUID hotelId) {
        return reviewService.getVisibleReviewsForHotel(hotelId);
    }

    // ── Manager moderation endpoints ─────────────────────────────────────────

    @GetMapping("/api/manager/reviews")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "List all reviews (manager)",
            description = "MANAGER/ADMIN only. Returns all reviews including hidden ones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All reviews returned successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — MANAGER or ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public List<ReviewResponseDto> getAllReviews() {
        return reviewService.getAllReviews();
    }

    @GetMapping("/api/manager/hotels/{hotelId}/reviews")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "List all reviews for a hotel (manager)",
            description = "MANAGER/ADMIN only. Includes hidden reviews.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reviews returned successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — MANAGER or ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public List<ReviewResponseDto> getAllReviewsForHotel(@PathVariable UUID hotelId) {
        return reviewService.getAllReviewsForHotel(hotelId);
    }

    @PostMapping("/api/manager/reviews/{reviewId}/hide")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Hide a review", description = "MANAGER/ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review hidden successfully",
                    content = @Content(schema = @Schema(implementation = ReviewResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — MANAGER or ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ReviewResponseDto hideReview(@PathVariable UUID reviewId) {
        return reviewService.hideReview(reviewId);
    }

    @PostMapping("/api/manager/reviews/{reviewId}/show")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Restore a hidden review", description = "MANAGER/ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review made visible again",
                    content = @Content(schema = @Schema(implementation = ReviewResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — MANAGER or ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ReviewResponseDto showReview(@PathVariable UUID reviewId) {
        return reviewService.showReview(reviewId);
    }

    @DeleteMapping("/api/manager/reviews/{reviewId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Delete a review", description = "MANAGER/ADMIN only. Permanently deletes the review.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Review deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — MANAGER or ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<Void> deleteReview(@PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
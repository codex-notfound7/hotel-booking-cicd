package com.project.soa.booking;


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

import com.project.soa.common.exception.ErrorResponseDto;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Booking", description = "Booking operations")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }


    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create booking",
            description = "Create a booking. Status starts as PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created successfully",
                    content = @Content(schema = @Schema(implementation = BookingResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or business rule violated " +
                    "(e.g. check-out before check-in, guest count exceeds capacity, room not available)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Room type not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody CreateBookingRequestDto dto,
            UriComponentsBuilder uriBuilder) {

        Booking booking = bookingService.createBooking(dto);

        return ResponseEntity.created(
                uriBuilder
                        .path("/{id}")
                        .buildAndExpand(booking.getId())
                        .toUri()
        ).body(BookingMapper.toDto(booking));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my bookings",
            description = "Returns bookings of the logged-in user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of bookings returned successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public List<BookingResponseDto> getMyBookings() {
        return bookingService.getMyBookings();
    }


    @GetMapping("/{id}")
    @PreAuthorize("@bookingSecurity.canViewBooking(#id)")
    @Operation(summary = "Get booking",
            description = "Booking owner or MANAGER only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking returned successfully",
                    content = @Content(schema = @Schema(implementation = BookingResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — not the booking owner or a manager",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public BookingResponseDto getBooking(@PathVariable UUID id) {
        return BookingMapper.toDto(
                bookingService.getBookingById(id)
        );
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@bookingSecurity.canCancel(#id)")
    @Operation(summary = "Cancel booking",
            description = "Owner or MANAGER only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled; returns refund and penalty details",
                    content = @Content(schema = @Schema(implementation = CancellationResultDto.class))),
            @ApiResponse(responseCode = "400", description = "Cannot cancel — already cancelled or check-in already passed",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public CancellationResultDto cancelBooking(@PathVariable UUID id) {
        return bookingService.cancelBooking(id);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Confirm booking",
            description = "MANAGER only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking confirmed successfully",
                    content = @Content(schema = @Schema(implementation = BookingResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Cannot confirm — not PENDING, expired, unpaid, or room fully booked",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — not the manager of this hotel",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public BookingResponseDto confirmBooking(@PathVariable UUID id) {

        Booking booking = bookingService.confirmBooking(id);
        return BookingMapper.toDto(booking);
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Get upcoming bookings",
            description = "MANAGER: upcoming bookings for hotels you manage. ADMIN: upcoming bookings across all hotels.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of upcoming bookings returned successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — MANAGER or ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public List<BookingResponseDto> getUpcomingBookings() {
        return bookingService.getUpcomingBookingsForManager();
    }
}
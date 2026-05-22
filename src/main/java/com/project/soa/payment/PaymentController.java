package com.project.soa.payment;

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

import java.util.UUID;


@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "Mock payment operations")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }


    @PostMapping("/intent")
    @PreAuthorize("@bookingSecurity.canViewBooking(#dto.bookingId)")
    @Operation(summary = "Create payment intent",
            description = "Create a payment intent linked to a PENDING booking. " +
                    "Caller must own the booking or be a MANAGER/ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment intent created successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Booking is not in PENDING status or already has a payment intent",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — not the booking owner or a manager",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PaymentResponseDto> createPaymentIntent(
            @Valid @RequestBody CreatePaymentIntentRequestDto dto,
            UriComponentsBuilder uriBuilder) {
        Payment payment = paymentService.createPaymentIntent(dto);
        PaymentResponseDto response = paymentService.getPayment(payment.getId());
        return ResponseEntity.created(
                        uriBuilder.path("/api/payments/{id}").buildAndExpand(payment.getId()).toUri())
                .body(response);
    }


    @PostMapping("/{paymentId}/simulate")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Simulate payment (MANAGER/ADMIN only)",
            description = "Simulate success or failure. " +
                    "Restricted to MANAGER/ADMIN — guests cannot self-confirm payments.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment simulation applied successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Payment is not in a simulatable state",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — MANAGER or ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public PaymentResponseDto simulatePayment(
            @PathVariable UUID paymentId,
            @RequestBody(required = false) SimulatePaymentRequestDto dto) {
        boolean success = dto == null || dto.isSuccess();
        return paymentService.simulatePayment(paymentId, success);
    }


    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment by payment ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment returned successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public PaymentResponseDto getPayment(@PathVariable UUID paymentId) {
        return paymentService.getPayment(paymentId);
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("@bookingSecurity.canViewBooking(#bookingId)")
    @Operation(summary = "Get payment by booking ID",
            description = "Caller must own the booking or be a MANAGER/ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment returned successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — not the booking owner or a manager",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Booking or payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public PaymentResponseDto getPaymentByBooking(@PathVariable UUID bookingId) {
        return paymentService.getPaymentByBookingId(bookingId);
    }
}
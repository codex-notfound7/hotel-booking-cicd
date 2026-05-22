package com.project.soa.auth.admin;

import com.project.soa.analytics.AnalyticsReportDto;
import com.project.soa.analytics.AnalyticsService;
import com.project.soa.auth.user.User;
import com.project.soa.auth.user.UserInternalService;
import com.project.soa.auth.user.UserMapper;
import com.project.soa.auth.user.UserResponseDto;
import com.project.soa.booking.BookingResponseDto;
import com.project.soa.booking.BookingService;
import com.project.soa.common.exception.ErrorResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Platform administration — ADMIN role required")
public class AdminController {

    private final UserInternalService userService;
    private final BookingService bookingService;
    private final AnalyticsService analyticsService;

    public AdminController(UserInternalService userService,
                           BookingService bookingService,
                           AnalyticsService analyticsService) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.analyticsService = analyticsService;
    }


    @GetMapping("/users")
    @Operation(summary = "List all users", description = "ADMIN only. Returns all registered users.")
    public List<UserResponseDto> getAllUsers() {
        return userService.findAll().stream().map(UserMapper::toDto).toList();
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user by ID", description = "ADMIN only. Returns a single user by UUID.")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID userId) {
        User user = userService.getById(userId);
        return ResponseEntity.ok(UserMapper.toDto(user));
    }

    @PostMapping("/users/{userId}/deactivate")
    @Operation(summary = "Deactivate user", description = "Temporarily disable a user account.")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/activate")
    @Operation(summary = "Activate user", description = "Activate a previously deactivated user account.")
    public ResponseEntity<Void> activateUser(@PathVariable UUID userId) {
        userService.activateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user", description = "Permanently remove a user account and refresh tokens.")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/users/{userId}/approve-manager")
    @Operation(summary = "Approve manager", description = "Approve a pending manager account.")
    public ResponseEntity<Void> approveManager(@PathVariable UUID userId) {
        userService.approveManager(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/reject-manager")
    @Operation(summary = "Reject manager", description = "Reject a pending manager account.")
    public ResponseEntity<Void> rejectManager(@PathVariable UUID userId) {
        userService.rejectManager(userId);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/bookings")
    @Operation(summary = "List all bookings", description = "ADMIN only. Returns all bookings across the platform.")
    public List<BookingResponseDto> getAllBookings() {
        return bookingService.getAllBookings();
    }


    @GetMapping("/analytics")
    @Operation(summary = "Get platform analytics", description = "ADMIN only. Returns dashboard stats.")
    public ResponseEntity<AnalyticsReportDto> getAnalytics() {
        AnalyticsReportDto report = analyticsService.generateReport();
        return ResponseEntity.ok(report);
    }
}
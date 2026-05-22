package com.project.soa.auth.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Profile", description = "Self-service profile management")
public class UserProfileController {

    private final UserInternalService userService;

    public UserProfileController(UserInternalService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my profile", description = "Returns the authenticated user's profile.")
    public ResponseEntity<UserResponseDto> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userService.getById(userId);
        return ResponseEntity.ok(UserMapper.toDto(user));
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update my profile",
            description = "Partial update — omit any field to leave it unchanged. " +
                    "Changing email to one already in use returns 409.")
    public ResponseEntity<UserResponseDto> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User updated = userService.updateProfile(userId, request.name(), request.email());
        return ResponseEntity.ok(UserMapper.toDto(updated));
    }

    public record UpdateProfileRequest(
            @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
            String name,

            @Email(message = "Must be a valid email address")
            String email
    ) {}
}
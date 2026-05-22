package com.project.soa.booking;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component("bookingSecurity")
@Transactional(readOnly = true)
public class BookingSecurity {

    private final BookingRepository bookingRepository;

    public BookingSecurity(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public boolean canCancel(UUID bookingId) {
        return isManagerOrAdmin() || isBookingOwner(bookingId);
    }

    public boolean canViewBooking(UUID bookingId) {
        return isManagerOrAdmin() || isBookingOwner(bookingId);
    }

    public boolean isOwnerOrManager(UUID userId) {
        return isManagerOrAdmin() || getCurrentUserId()
                .map(id -> id.equals(userId.toString()))
                .orElse(false);
    }

    public boolean isManager() {
        return isManagerOrAdmin();
    }

    private boolean isManagerOrAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_MANAGER") || a.equals("ROLE_ADMIN"));
    }

    private boolean isBookingOwner(UUID bookingId) {
        return getCurrentUserId()
                .map(currentUserId ->
                        bookingRepository.findById(bookingId)
                                .map(booking -> booking.getUser().getId().toString().equals(currentUserId))
                                .orElse(false))
                .orElse(false);
    }

    private Optional<String> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.ofNullable(jwt.getSubject());
        }
        return Optional.empty();
    }
}
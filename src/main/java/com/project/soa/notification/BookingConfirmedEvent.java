package com.project.soa.notification;

import java.util.UUID;

public record BookingConfirmedEvent(UUID bookingId) {}

package com.project.soa.notification;

import java.util.UUID;


public interface NotificationService {
    void sendBookingConfirmed(UUID bookingId);
    void sendBookingCancelled(UUID bookingId);
}

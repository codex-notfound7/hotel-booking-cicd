package com.project.soa.payment;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED,
    PARTIAL_REFUND  // used when late-cancellation penalty applies
}

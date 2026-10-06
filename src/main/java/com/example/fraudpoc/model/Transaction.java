package com.example.fraudpoc.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(
        String transactionId,
        String customerId,
        LocalDateTime timestamp,
        BigDecimal amount,
        String currency,
        String transactionType,
        String merchantCategory,
        String merchantId,
        String location,
        String deviceId,
        boolean newDevice,
        boolean newBeneficiary,
        int failedAttempts24h,
        int transactionCount24h
) {}

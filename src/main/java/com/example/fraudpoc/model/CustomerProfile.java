package com.example.fraudpoc.model;

import java.math.BigDecimal;
import java.util.List;

public record CustomerProfile(
        String customerId,
        BigDecimal averageTransactionAmount,
        int transactionCount24h,
        int transactionCount7d,
        List<String> usualLocations,
        List<String> usualDeviceIds,
        List<String> usualMerchantCategories
) {}

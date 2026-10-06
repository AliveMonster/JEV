package com.example.fraudpoc.model;

import java.util.List;

public record FraudDecision(
        String transactionId,
        FraudRisk fraudRisk,
        double confidence,
        Priority priority,
        boolean escalate,
        RecommendedAction recommendedAction,
        List<String> reasonCodes,
        String caseSummary
) {}

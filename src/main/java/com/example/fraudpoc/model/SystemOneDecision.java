package com.example.fraudpoc.model;

public record SystemOneDecision(
        FraudRisk fraudRisk,
        double confidence,
        double escalationProbability,

        double lowProbability,
        double mediumProbability,
        double highProbability,

        String model,

        long inputTokens,
        long outputTokens
) {
}
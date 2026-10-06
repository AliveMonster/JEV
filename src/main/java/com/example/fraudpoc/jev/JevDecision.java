package com.example.fraudpoc.jev;

import com.example.fraudpoc.model.FraudRisk;

public record JevDecision(
        FraudRisk fraudRisk,
        double confidence,
        double lowProbability,
        double mediumProbability,
        double highProbability,
        long inputTokens,
        long outputTokens
) {

    public long totalTokens() {
        return inputTokens + outputTokens;
    }
}
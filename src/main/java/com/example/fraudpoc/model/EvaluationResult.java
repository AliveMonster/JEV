package com.example.fraudpoc.model;

import com.example.fraudpoc.model.FraudDecision;
import com.example.fraudpoc.model.FraudRisk;

public record EvaluationResult(
        String architecture,
        FraudDecision decision,

        boolean sentToLlm,
        boolean stoppedByGate,

        long latencyMs,

        long inputTokens,
        long outputTokens,
        long totalTokens,

        long systemOneLatencyMs,
        long systemOneInputTokens,
        long systemOneOutputTokens,

        FraudRisk systemOneFraudRisk,
        double systemOneConfidence,
        double systemOneEscalationProbability,
        double systemOneLowProbability,
        double systemOneMediumProbability,
        double systemOneHighProbability
) {
}
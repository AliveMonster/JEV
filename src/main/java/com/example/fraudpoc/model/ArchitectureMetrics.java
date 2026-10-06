package com.example.fraudpoc.model;

public record ArchitectureMetrics(
        long transactions,
        long truePositives,
        long trueNegatives,
        long falsePositives,
        long falseNegatives,

        double accuracy,
        double precision,
        double recall,
        double f1,

        double avgLatencyMs,
        double p50LatencyMs,
        double p95LatencyMs,
        double p99LatencyMs,

        long llmCalls,

        // Architecture 2 / System One gate metrics
        long systemOneCalls,
        long gateStops,
        long gateFalseNegatives,
        long inputTokens,
        long outputTokens,
        long totalTokens,
        long systemOneInputTokens,
        long systemOneOutputTokens,
        long systemOneTotalTokens,

        // Architecture 3 / Jev gate metrics
        long jevCalls,
        long jevGateStops,
        long jevGateFalseNegatives,
        long jevInputTokens,
        long jevOutputTokens,
        long jevTotalTokens
) {}
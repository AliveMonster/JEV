package com.example.fraudpoc.model;

public record BenchmarkComparison(
        int datasetSize,
        ArchitectureMetrics architecture1,
        ArchitectureMetrics architecture2,
        double qwenCallReduction,
        double qwenTokenReduction,
        double latencyReduction
) {}

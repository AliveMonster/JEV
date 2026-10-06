package com.example.fraudpoc.model;

public record SingleEvaluationRequest(
        Transaction transaction,
        CustomerProfile customerContext
) {}

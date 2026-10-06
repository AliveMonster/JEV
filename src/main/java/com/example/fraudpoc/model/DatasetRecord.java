package com.example.fraudpoc.model;

public record DatasetRecord(
        Transaction transaction,
        CustomerProfile customerContext,
        boolean actualFraud,
        FraudPattern fraudPattern
) {}

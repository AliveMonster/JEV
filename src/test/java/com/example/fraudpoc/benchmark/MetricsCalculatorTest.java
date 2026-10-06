package com.example.fraudpoc.benchmark;

import com.example.fraudpoc.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCalculatorTest {

    @Test
    void calculatesBinaryMetrics() {

        CustomerProfile c = new CustomerProfile(
                "C1",
                BigDecimal.TEN,
                1,
                2,
                List.of("Bangalore"),
                List.of("D1"),
                List.of("GROCERY")
        );

        DatasetRecord fraud = new DatasetRecord(
                new Transaction(
                        "T1",
                        "C1",
                        LocalDateTime.now(),
                        BigDecimal.TEN,
                        "INR",
                        "UPI",
                        "GROCERY",
                        "M1",
                        "Bangalore",
                        "D1",
                        false,
                        false,
                        0,
                        1
                ),
                c,
                true,
                FraudPattern.HIGH_VALUE_ANOMALY
        );

        DatasetRecord safe = new DatasetRecord(
                new Transaction(
                        "T2",
                        "C1",
                        LocalDateTime.now(),
                        BigDecimal.TEN,
                        "INR",
                        "UPI",
                        "GROCERY",
                        "M1",
                        "Bangalore",
                        "D1",
                        false,
                        false,
                        0,
                        1
                ),
                c,
                false,
                FraudPattern.NONE
        );

        FraudDecision high = new FraudDecision(
                "T1",
                FraudRisk.HIGH,
                0.9,
                Priority.P1,
                true,
                RecommendedAction.BLOCK,
                List.of(),
                "High risk transaction"
        );

        FraudDecision low = new FraudDecision(
                "T2",
                FraudRisk.LOW,
                0.9,
                Priority.P3,
                false,
                RecommendedAction.ALLOW,
                List.of(),
                "Low risk transaction"
        );

        EvaluationResult fraudResult = new EvaluationResult(
                "ARCHITECTURE_1",
                high,

                true,
                false,

                100,

                50,
                20,
                70,

                0,
                0,
                0,

                null,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );

        EvaluationResult safeResult = new EvaluationResult(
                "ARCHITECTURE_1",
                low,

                true,
                false,

                100,

                50,
                20,
                70,

                0,
                0,
                0,

                null,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );

        var m = new MetricsCalculator().calculate(
                List.of(fraud, safe),
                List.of(fraudResult, safeResult),
                "ARCHITECTURE_1"
        );

        assertEquals(1, m.truePositives());
        assertEquals(1, m.trueNegatives());
        assertEquals(0, m.falseNegatives());
        assertEquals(1.0, m.accuracy());
    }
}
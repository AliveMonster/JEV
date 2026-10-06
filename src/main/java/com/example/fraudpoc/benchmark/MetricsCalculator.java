package com.example.fraudpoc.benchmark;

import com.example.fraudpoc.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MetricsCalculator {

    public ArchitectureMetrics calculate(
            List<DatasetRecord> records,
            List<EvaluationResult> results,
            String architecture) {

        long tp = 0;
        long tn = 0;
        long fp = 0;
        long fn = 0;

        long gateStops = 0;
        long gateFn = 0;

        long llm = 0;

        long systemOneCalls = 0;

        long inputTokens = 0;
        long outputTokens = 0;
        long totalTokens = 0;

        long systemOneInputTokens = 0;
        long systemOneOutputTokens = 0;

        long jevCalls = 0;
        long jevGateStops = 0;
        long jevGateFn = 0;

        long jevInputTokens = 0;
        long jevOutputTokens = 0;

        List<Long> latencies = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {

            DatasetRecord r = records.get(i);
            EvaluationResult e = results.get(i);

            boolean predicted =
                    e.decision().fraudRisk() != FraudRisk.LOW;

            // -----------------------------
            // Classification metrics
            // -----------------------------

            if (r.actualFraud() && predicted) {
                tp++;
            } else if (!r.actualFraud() && !predicted) {
                tn++;
            } else if (!r.actualFraud()) {
                fp++;
            } else {
                fn++;
            }

            // -----------------------------
            // Architecture 2
            // -----------------------------

            if ("ARCHITECTURE_2".equals(architecture)) {

                systemOneCalls++;

                if (e.stoppedByGate()) {
                    gateStops++;
                }

                if (e.stoppedByGate() && r.actualFraud()) {
                    gateFn++;
                }

                systemOneInputTokens +=
                        e.systemOneInputTokens();

                systemOneOutputTokens +=
                        e.systemOneOutputTokens();
            }

            // -----------------------------
            // Architecture 3
            // -----------------------------

            if ("ARCHITECTURE_3".equals(architecture)) {

                /*
                 * Jev metrics are currently carried
                 * through the existing gate fields in
                 * EvaluationResult.
                 */

                jevCalls++;

                jevInputTokens +=
                        e.systemOneInputTokens();

                jevOutputTokens +=
                        e.systemOneOutputTokens();

                if (e.stoppedByGate()) {
                    jevGateStops++;
                }

                if (e.stoppedByGate() && r.actualFraud()) {
                    jevGateFn++;
                }
            }

            // -----------------------------
            // Qwen metrics
            // -----------------------------

            if (e.sentToLlm()) {
                llm++;
            }

            inputTokens += e.inputTokens();
            outputTokens += e.outputTokens();
            totalTokens += e.totalTokens();

            latencies.add(e.latencyMs());
        }

        double n = records.size();

        double accuracy =
                safe((tp + tn) / n);

        double precision =
                safe(tp / (double) (tp + fp));

        double recall =
                safe(tp / (double) (tp + fn));

        double f1 =
                safe(
                        2 * precision * recall
                                / (precision + recall)
                );

        Collections.sort(latencies);

        return new ArchitectureMetrics(

                records.size(),

                tp,
                tn,
                fp,
                fn,

                accuracy,
                precision,
                recall,
                f1,

                latencies.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0),

                percentile(latencies, 0.50),
                percentile(latencies, 0.95),
                percentile(latencies, 0.99),

                llm,

                systemOneCalls,
                gateStops,
                gateFn,

                inputTokens,
                outputTokens,
                totalTokens,

                systemOneInputTokens,
                systemOneOutputTokens,
                systemOneInputTokens
                        + systemOneOutputTokens,

                // Jev
                jevCalls,
                jevGateStops,
                jevGateFn,
                jevInputTokens,
                jevOutputTokens,
                jevInputTokens + jevOutputTokens
        );
    }

    private static double safe(double value) {
        return Double.isFinite(value)
                ? value
                : 0.0;
    }

    private static double percentile(
            List<Long> values,
            double percentile) {

        if (values.isEmpty()) {
            return 0;
        }

        int index =
                (int) Math.ceil(
                        percentile * values.size()
                ) - 1;

        index =
                Math.max(
                        0,
                        Math.min(
                                index,
                                values.size() - 1
                        )
                );

        return values.get(index);
    }
}
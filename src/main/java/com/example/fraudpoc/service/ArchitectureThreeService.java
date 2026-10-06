package com.example.fraudpoc.service;

import com.example.fraudpoc.config.AppProperties;
import com.example.fraudpoc.jev.JevClient;
import com.example.fraudpoc.jev.JevDecision;
import com.example.fraudpoc.model.DatasetRecord;
import com.example.fraudpoc.model.EvaluationResult;
import com.example.fraudpoc.model.FraudDecision;
import com.example.fraudpoc.model.FraudRisk;
import com.example.fraudpoc.model.Priority;
import com.example.fraudpoc.model.RecommendedAction;
import com.example.fraudpoc.ollama.OllamaFraudClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArchitectureThreeService {

    private final JevClient jevClient;
    private final OllamaFraudClient ollama;
    private final AppProperties properties;

    public ArchitectureThreeService(
            JevClient jevClient,
            OllamaFraudClient ollama,
            AppProperties properties) {

        this.jevClient = jevClient;
        this.ollama = ollama;
        this.properties = properties;
    }

    public EvaluationResult evaluate(DatasetRecord record)
            throws JsonProcessingException {

        long start = System.nanoTime();

        JevDecision gate =
                jevClient.evaluate(
                        record.transaction(),
                        record.customerContext()
                );

        long gateLatency =
                (System.nanoTime() - start) / 1_000_000;

        boolean stop =
                gate.fraudRisk() == FraudRisk.LOW
                        && gate.confidence()
                        >= properties.getJev().getConfidenceThreshold();

        if (stop) {

            FraudDecision decision =
                    new FraudDecision(
                            record.transaction().transactionId(),
                            FraudRisk.LOW,
                            gate.confidence(),
                            Priority.P3,
                            false,
                            RecommendedAction.ALLOW,
                            List.of("JEV_LOW_CONFIDENT"),
                            "Transaction classified as LOW risk by Jev and stopped before reaching Qwen."
                    );

            return new EvaluationResult(
                    "ARCHITECTURE_3",
                    decision,
                    false,
                    true,
                    gateLatency,
                    0,
                    0,
                    0,
                    gateLatency,
                    gate.inputTokens(),
                    gate.outputTokens(),
                    gate.fraudRisk(),
                    gate.confidence(),
                    0.0,
                    gate.lowProbability(),
                    gate.mediumProbability(),
                    gate.highProbability()
            );
        }

        EvaluationResult qwen =
                ollama.evaluate(
                        record.transaction(),
                        record.customerContext(),
                        "ARCHITECTURE_3"
                );

        return new EvaluationResult(
                "ARCHITECTURE_3",
                qwen.decision(),
                true,
                false,
                (System.nanoTime() - start) / 1_000_000,
                qwen.inputTokens(),
                qwen.outputTokens(),
                qwen.totalTokens(),
                gateLatency,
                gate.inputTokens(),
                gate.outputTokens(),
                gate.fraudRisk(),
                gate.confidence(),
                0.0,
                gate.lowProbability(),
                gate.mediumProbability(),
                gate.highProbability()
        );
    }
}
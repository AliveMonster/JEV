package com.example.fraudpoc.controller;

import com.example.fraudpoc.model.DatasetRecord;
import com.example.fraudpoc.model.FraudPattern;
import com.example.fraudpoc.model.EvaluationResult;
import com.example.fraudpoc.model.SingleEvaluationRequest;
import com.example.fraudpoc.ollama.OllamaFraudClient;
import com.example.fraudpoc.service.ArchitectureThreeService;
import com.example.fraudpoc.service.ArchitectureTwoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transaction-risk")
public class TransactionRiskController {

    private final OllamaFraudClient ollama;
    private final ArchitectureTwoService architectureTwo;
    private final ArchitectureThreeService architectureThree;

    public TransactionRiskController(
            OllamaFraudClient ollama,
            ArchitectureTwoService architectureTwo,
            ArchitectureThreeService architectureThree) {

        this.ollama = ollama;
        this.architectureTwo = architectureTwo;
        this.architectureThree = architectureThree;
    }
    @PostMapping("/evaluate-architecture-3")
    public ResponseEntity<EvaluationResult> evaluateArchitectureThree(
            @RequestBody SingleEvaluationRequest request)
            throws JsonProcessingException {

        DatasetRecord record =
                new DatasetRecord(
                        request.transaction(),
                        request.customerContext(),
                        false,
                        FraudPattern.NONE
                );

        return ResponseEntity.ok(
                architectureThree.evaluate(record)
        );
    }

    @PostMapping("/evaluate")
    public ResponseEntity<EvaluationResult> evaluate(
            @RequestBody SingleEvaluationRequest request)
            throws JsonProcessingException {

        return ResponseEntity.ok(
                ollama.evaluate(
                        request.transaction(),
                        request.customerContext(),
                        "ARCHITECTURE_1"
                )
        );
    }

    @PostMapping("/evaluate-architecture-2")
    public ResponseEntity<EvaluationResult> evaluateArchitectureTwo(
            @RequestBody SingleEvaluationRequest request)
            throws JsonProcessingException {

        DatasetRecord record = new DatasetRecord(
                request.transaction(),
                request.customerContext(),
                false,
                FraudPattern.NONE
        );

        return ResponseEntity.ok(
                architectureTwo.evaluate(record)
        );
    }
}
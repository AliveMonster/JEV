package com.example.fraudpoc.service;

import com.example.fraudpoc.model.DatasetRecord;
import com.example.fraudpoc.model.EvaluationResult;
import com.example.fraudpoc.ollama.OllamaFraudClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;

@Service
public class ArchitectureOneService {
    private final OllamaFraudClient ollama;
    public ArchitectureOneService(OllamaFraudClient ollama){this.ollama=ollama;}
    public EvaluationResult evaluate(DatasetRecord record) throws JsonProcessingException {return ollama.evaluate(record.transaction(),record.customerContext(),"ARCHITECTURE_1");}
}

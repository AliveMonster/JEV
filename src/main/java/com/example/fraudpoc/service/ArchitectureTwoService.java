package com.example.fraudpoc.service;

import com.example.fraudpoc.config.AppProperties;
import com.example.fraudpoc.model.*;
import com.example.fraudpoc.ollama.OllamaFraudClient;
import com.example.fraudpoc.systemone.SystemOneClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;

@Service
public class ArchitectureTwoService {
    private final SystemOneClient systemOne; private final OllamaFraudClient ollama; private final AppProperties props;
    public ArchitectureTwoService(SystemOneClient systemOne,OllamaFraudClient ollama,AppProperties props){this.systemOne=systemOne;this.ollama=ollama;this.props=props;}
    public EvaluationResult evaluate(DatasetRecord record) throws JsonProcessingException {
        long start=System.nanoTime();
        long systemOneStart=System.nanoTime();
        SystemOneDecision gate=systemOne.evaluate(record.transaction(),record.customerContext());
        long systemOneLatency=(System.nanoTime()-systemOneStart)/1_000_000;
        boolean stop=gate.fraudRisk()==FraudRisk.LOW && gate.confidence()>=props.getSystemOne().getConfidenceThreshold();
        if(stop){
            long latency=(System.nanoTime()-start)/1_000_000;
            FraudDecision d=new FraudDecision(record.transaction().transactionId(),gate.fraudRisk(),gate.confidence(),Priority.P3,false,RecommendedAction.ALLOW,java.util.List.of("SYSTEM_ONE_LOW_CONFIDENT_STOP"),"Stopped by System One gate; no generative analysis was requested.");
            return new EvaluationResult(
                    "ARCHITECTURE_2",
                    d,
                    false,
                    true,
                    latency,

                    0,
                    0,
                    0,

                    systemOneLatency,
                    gate.inputTokens(),
                    gate.outputTokens(),

                    gate.fraudRisk(),
                    gate.confidence(),
                    gate.escalationProbability(),
                    gate.lowProbability(),
                    gate.mediumProbability(),
                    gate.highProbability()
            );
        }
        EvaluationResult q=ollama.evaluate(record.transaction(),record.customerContext(),"ARCHITECTURE_2");
        return new EvaluationResult(
                "ARCHITECTURE_2",
                q.decision(),
                true,
                false,
                (System.nanoTime() - start) / 1_000_000,

                q.inputTokens(),
                q.outputTokens(),
                q.totalTokens(),

                systemOneLatency,
                gate.inputTokens(),
                gate.outputTokens(),

                gate.fraudRisk(),
                gate.confidence(),
                gate.escalationProbability(),
                gate.lowProbability(),
                gate.mediumProbability(),
                gate.highProbability()
        );
    }
}

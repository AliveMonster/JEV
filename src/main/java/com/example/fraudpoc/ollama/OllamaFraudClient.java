package com.example.fraudpoc.ollama;

import com.example.fraudpoc.config.AppProperties;
import com.example.fraudpoc.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class OllamaFraudClient {
    private final RestClient http;
    private final AppProperties props;
    private final ObjectMapper mapper;
    private final String systemPrompt;

    public OllamaFraudClient(RestClient http, AppProperties props, ObjectMapper mapper) {
        this.http=http; this.props=props; this.mapper=mapper;
        try { this.systemPrompt=new ClassPathResource("prompts/fraud-analysis-system.txt").getContentAsString(StandardCharsets.UTF_8); }
        catch(Exception e){ throw new IllegalStateException("Cannot load fraud system prompt",e); }
    }

    public EvaluationResult evaluate(Transaction t, CustomerProfile c, String architecture) throws JsonProcessingException {
        long start=System.nanoTime();
        String userPrompt=buildUserPrompt(t,c);
        OllamaChatResponse response=http.post().uri(props.getOllama().getBaseUrl()+"/api/chat")
                .body(new OllamaRequest(props.getOllama().getModel(), List.of(new Message("system",systemPrompt),new Message("user",userPrompt)), mapper.readTree(schema()), false, new Options(0.0)))
                .retrieve().body(OllamaChatResponse.class);
        long latency=(System.nanoTime()-start)/1_000_000;
        if(response==null || response.message()==null) throw new IllegalStateException("Empty Ollama response");
        try {
            FraudDecision decision=mapper.readValue(stripJson(response.message().content()), FraudDecision.class);
            long in=response.promptEvalCount()==null?0:response.promptEvalCount();
            long out=response.evalCount()==null?0:response.evalCount();
            return new EvaluationResult(
                    architecture,
                    decision,

                    true,
                    false,

                    latency,

                    // Qwen tokens
                    in,
                    out,
                    in + out,

                    // System One
                    0,
                    0,
                    0,

                    // System One decision fields
                    null,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        } catch(Exception e){throw new IllegalStateException("Invalid Qwen fraud JSON: "+response.message().content(),e);}
    }

    public FraudDecision evaluateDecision(Transaction t, CustomerProfile c) throws JsonProcessingException { return evaluate(t,c,"ARCHITECTURE_1").decision(); }

    private String buildUserPrompt(Transaction t, CustomerProfile c){
        return """
                Evaluate this transaction for fraud risk. Do not invent facts.

                CUSTOMER CONTEXT:
                customerId=%s
                averageTransactionAmount=%s
                transactionCount24h=%d
                transactionCount7d=%d
                usualLocations=%s
                usualDeviceIds=%s
                usualMerchantCategories=%s

                TRANSACTION:
                transactionId=%s
                timestamp=%s
                amount=%s
                currency=%s
                transactionType=%s
                merchantCategory=%s
                merchantId=%s
                location=%s
                deviceId=%s
                newDevice=%s
                newBeneficiary=%s
                failedAttempts24h=%d
                transactionCount24h=%d
                """.formatted(c.customerId(),c.averageTransactionAmount(),c.transactionCount24h(),c.transactionCount7d(),c.usualLocations(),c.usualDeviceIds(),c.usualMerchantCategories(),t.transactionId(),t.timestamp(),t.amount(),t.currency(),t.transactionType(),t.merchantCategory(),t.merchantId(),t.location(),t.deviceId(),t.newDevice(),t.newBeneficiary(),t.failedAttempts24h(),t.transactionCount24h());
    }

    private String stripJson(String s){
        String x=s.trim(); if(x.startsWith("```")){x=x.replaceFirst("^```(?:json)?\\s*","").replaceFirst("\\s*```$","");} return x.trim();
    }
    private String schema(){return """
            {"type":"object","properties":{"transactionId":{"type":"string"},"fraudRisk":{"type":"string","enum":["LOW","MEDIUM","HIGH"]},"confidence":{"type":"number","minimum":0,"maximum":1},"priority":{"type":"string","enum":["P1","P2","P3"]},"escalate":{"type":"boolean"},"recommendedAction":{"type":"string","enum":["ALLOW","MONITOR","REVIEW","BLOCK"]},"reasonCodes":{"type":"array","items":{"type":"string"}},"caseSummary":{"type":"string"}},"required":["transactionId","fraudRisk","confidence","priority","escalate","recommendedAction","reasonCodes","caseSummary"]}
            """;}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role,String content){}
    public record OllamaRequest(String model,List<Message> messages,Object format,boolean stream, Options options){ }
    public record Options(
            double temperature
    ) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OllamaChatResponse(String model,Message message,Boolean done,
                                     @JsonProperty("total_duration") Long totalDuration,
                                     @JsonProperty("prompt_eval_count") Long promptEvalCount,
                                     @JsonProperty("eval_count") Long evalCount){ }
}

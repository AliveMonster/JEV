package com.example.fraudpoc.jev;

import com.example.fraudpoc.model.CustomerProfile;
import com.example.fraudpoc.model.FraudRisk;
import com.example.fraudpoc.model.Transaction;
import com.example.fraudpoc.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JevClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public JevClient(
            ObjectMapper objectMapper,
            AppProperties properties) {

        this.objectMapper = objectMapper;
        this.properties = properties;

        this.restClient = RestClient.builder()
                .baseUrl(properties.getJev().getBaseUrl())
                .requestInterceptor((request, body, execution) -> {

                    System.out.println("===== JEV OUTGOING REQUEST =====");
                    System.out.println("URI: " + request.getURI());
                    System.out.println("Method: " + request.getMethod());
                    System.out.println("Authorization present: "
                            + request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION));
                    System.out.println("Authorization value: "
                            + request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
                    System.out.println("Content-Type: "
                            + request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
                    System.out.println("================================");

                    return execution.execute(request, body);
                })
                .build();
    }

    public JevDecision evaluate(
            Transaction transaction,
            CustomerProfile customerContext)
            throws JsonProcessingException {

        Map<String, Object> state = buildState(
                transaction,
                customerContext
        );

        Map<String, Object> questions = buildQuestions();

        JevRequest request = new JevRequest(
                properties.getJev().getModel(),
                state,
                questions
        );

        long start = System.nanoTime();

        String apiKey = properties.getJev().getApiKey();

        String response = restClient
                .post()
                .uri("/api/alpha/decisions")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + apiKey
                )
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .body(request)
                .retrieve()
                .body(String.class);

        long latencyMs =
                (System.nanoTime() - start) / 1_000_000;
        System.out.println("JEV API key present: "
                + (apiKey != null && !apiKey.isBlank()));

        System.out.println("JEV API key length: "
                + (apiKey == null ? 0 : apiKey.length()));
        return parseResponse(response);
    }

    private Map<String, Object> buildState(
            Transaction t,
            CustomerProfile c) {

        double amountVsAverage = 0.0;

        if (c.averageTransactionAmount() != null
                && c.averageTransactionAmount().compareTo(BigDecimal.ZERO) > 0) {

            amountVsAverage =
                    t.amount().doubleValue()
                            / c.averageTransactionAmount().doubleValue();
        }

        boolean locationChanged =
                c.usualLocations() == null
                        || !c.usualLocations().contains(t.location());

        boolean deviceChanged =
                t.newDevice()
                        || c.usualDeviceIds() == null
                        || !c.usualDeviceIds().contains(t.deviceId());

        boolean beneficiaryChanged =
                t.newBeneficiary();

        int customerTransactions24h =
                Math.max(c.transactionCount24h(), 1);

        double velocityRatio =
                (double) t.transactionCount24h()
                        / customerTransactions24h;

        Map<String, Object> derivedSignals =
                new LinkedHashMap<>();

        derivedSignals.put(
                "amountVsAverage",
                round(amountVsAverage)
        );

        derivedSignals.put(
                "locationChanged",
                locationChanged
        );

        derivedSignals.put(
                "deviceChanged",
                deviceChanged
        );

        derivedSignals.put(
                "newBeneficiary",
                beneficiaryChanged
        );

        derivedSignals.put(
                "failedAttempts24h",
                t.failedAttempts24h()
        );

        derivedSignals.put(
                "transactionCount24h",
                t.transactionCount24h()
        );

        derivedSignals.put(
                "customerTypicalTransactionCount24h",
                c.transactionCount24h()
        );

        derivedSignals.put(
                "velocityRatio",
                round(velocityRatio)
        );

        Map<String, Object> state =
                new LinkedHashMap<>();

        state.put(
                "customerContext",
                c
        );

        state.put(
                "transaction",
                t
        );

        state.put(
                "derivedFraudSignals",
                derivedSignals
        );

        return state;
    }

    private Map<String, Object> buildQuestions() {

        Map<String, Object> fraudRisk =
                new LinkedHashMap<>();

        fraudRisk.put(
                "type",
                "choice"
        );

        fraudRisk.put(
                "instructions",
                """
                Classify the transaction's fraud risk using BOTH
                the supplied customer context and the derived
                fraud signals.

                Consider all relevant indicators together:

                - transaction amount relative to the customer's
                  average transaction amount
                - new or changed device
                - new beneficiary
                - location change
                - failed authentication attempts
                - unusually high transaction velocity

                LOW:
                The transaction is broadly consistent with the
                customer's normal behavior and there are no
                significant fraud indicators.

                MEDIUM:
                There are one or more suspicious or unusual
                indicators, but the evidence is not strong enough
                to indicate likely fraud.

                HIGH:
                Multiple strong fraud indicators or a significant
                anomaly suggest possible fraudulent activity or
                account compromise.

                Do not classify a transaction as HIGH merely because
                it is unusual. Consider the combination and strength
                of the available evidence.
                """
        );

        Map<String, String> criteria =
                new LinkedHashMap<>();

        criteria.put(
                "LOW",
                "Broadly consistent customer behavior with no significant fraud indicators."
        );

        criteria.put(
                "MEDIUM",
                "Some suspicious or unusual characteristics requiring caution, but evidence is not strong enough to indicate likely fraud."
        );

        criteria.put(
                "HIGH",
                "Multiple strong indicators or a significant anomaly suggesting possible fraudulent activity or account compromise."
        );

        fraudRisk.put(
                "criteria",
                criteria
        );

        Map<String, Object> questions =
                new LinkedHashMap<>();

        questions.put(
                "fraudRisk",
                fraudRisk
        );

        return questions;
    }

    private JevDecision parseResponse(String response)
            throws JsonProcessingException {

        JsonNode root =
                objectMapper.readTree(response);

        JsonNode answer =
                root.path("answers")
                        .path("fraudRisk");

        if (answer.isMissingNode()
                || answer.isNull()) {

            throw new IllegalStateException(
                    "Jev response does not contain answers.fraudRisk: "
                            + response
            );
        }

        String choice =
                answer.path("choice").asText();

        FraudRisk fraudRisk;

        try {
            fraudRisk =
                    FraudRisk.valueOf(
                            choice.toUpperCase()
                    );
        } catch (IllegalArgumentException e) {

            throw new IllegalStateException(
                    "Unexpected Jev fraudRisk: " + choice,
                    e
            );
        }

        double confidence =
                answer.path("confidence").asDouble();

        JsonNode probabilities =
                answer.path("probabilities");

        double low =
                probabilities.path("LOW").asDouble(0.0);

        double medium =
                probabilities.path("MEDIUM").asDouble(0.0);

        double high =
                probabilities.path("HIGH").asDouble(0.0);

        JsonNode usage =
                root.path("usage");

        long inputTokens =
                usage.path("input_tokens").asLong(0);

        long outputTokens =
                usage.path("output_tokens").asLong(0);

        return new JevDecision(
                fraudRisk,
                confidence,
                low,
                medium,
                high,
                inputTokens,
                outputTokens
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
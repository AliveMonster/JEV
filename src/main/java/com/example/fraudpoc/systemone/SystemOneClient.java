package com.example.fraudpoc.systemone;

import com.example.fraudpoc.config.AppProperties;
import com.example.fraudpoc.model.CustomerProfile;
import com.example.fraudpoc.model.FraudRisk;
import com.example.fraudpoc.model.SystemOneDecision;
import com.example.fraudpoc.model.Transaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemOneClient {

    private final RestClient http;
    private final AppProperties props;
    private final ObjectMapper mapper;

    public SystemOneClient(
            RestClient http,
            AppProperties props,
            ObjectMapper mapper) {

        this.http = http;
        this.props = props;
        this.mapper = mapper;
    }

    public SystemOneDecision evaluate(
            Transaction t,
            CustomerProfile c) {

        /*
         * ---------------------------------------------------------
         * 1. DERIVED FRAUD SIGNALS
         * ---------------------------------------------------------
         *
         * These are deterministic features calculated from the
         * transaction and customer context.
         *
         * IMPORTANT:
         * actualFraud is NOT used here.
         */

        double amountVsAverage = 0.0;

        if (c.averageTransactionAmount() != null
                && c.averageTransactionAmount().doubleValue() > 0) {

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
                round(amountVsAverage));

        derivedSignals.put(
                "locationChanged",
                locationChanged);

        derivedSignals.put(
                "deviceChanged",
                deviceChanged);

        derivedSignals.put(
                "newBeneficiary",
                beneficiaryChanged);

        derivedSignals.put(
                "failedAttempts24h",
                t.failedAttempts24h());

        derivedSignals.put(
                "transactionCount24h",
                t.transactionCount24h());

        derivedSignals.put(
                "customerTypicalTransactionCount24h",
                c.transactionCount24h());

        derivedSignals.put(
                "velocityRatio",
                round(velocityRatio));


        /*
         * ---------------------------------------------------------
         * 2. STATE SENT TO LAYA
         * ---------------------------------------------------------
         */

        Map<String, Object> state =
                new LinkedHashMap<>();

        state.put(
                "customerContext",
                c);

        state.put(
                "transaction",
                t);

        state.put(
                "derivedFraudSignals",
                derivedSignals);


        /*
         * ---------------------------------------------------------
         * 3. QUESTIONS
         * ---------------------------------------------------------
         */

        Map<String, Object> questions =
                new LinkedHashMap<>();

        questions.put(
                "fraudRisk",
                Map.of(
                        "type",
                        "choice",

                        "instructions",
                        """
                        Classify the transaction's fraud risk using
                        BOTH the supplied customer/transaction context
                        AND the derived fraud signals.

                        Pay particular attention to:
                        - transaction amount relative to the customer's
                          normal transaction amount
                        - new or changed device
                        - new beneficiary
                        - location change
                        - failed authentication attempts
                        - unusually high transaction velocity

                        LOW:
                        Behavior is broadly consistent with the customer
                        and there are no significant fraud indicators.

                        MEDIUM:
                        There are one or more suspicious or unusual
                        indicators, but the evidence is not strong enough
                        to indicate likely fraud.

                        HIGH:
                        Multiple strong fraud indicators or a significant
                        anomaly suggest possible account compromise or
                        fraudulent activity.

                        Do not assume that an unusual transaction is
                        automatically fraudulent.
                        """,

                        "criteria",
                        Map.of(
                                "LOW",
                                "Broadly consistent customer behavior with no significant fraud indicators",

                                "MEDIUM",
                                "Some suspicious or unusual characteristics requiring caution",

                                "HIGH",
                                "Multiple strong indicators or significant anomaly suggesting possible fraud"
                        )
                )
        );


        questions.put(
                "escalation",
                Map.of(
                        "type",
                        "noul",

                        "instructions",
                        """
                        Determine whether this transaction should be
                        escalated for additional fraud investigation.

                        Escalation should increase when there are multiple
                        independent fraud indicators such as:
                        unusually large amount, new device, new beneficiary,
                        changed location, authentication failures, or
                        unusually high transaction velocity.
                        """,

                        "threshold",
                        0.5
                )
        );


        /*
         * ---------------------------------------------------------
         * 4. REQUEST
         * ---------------------------------------------------------
         */

        Map<String, Object> req =
                Map.of(
                        "model",
                        props.getSystemOne().getModel(),

                        "state",
                        state,

                        "questions",
                        questions
                );


        /*
         * ---------------------------------------------------------
         * 5. CALL LAYA
         * ---------------------------------------------------------
         */

        JsonNode root =
                http.post()
                        .uri(
                                props.getSystemOne().getBaseUrl()
                                        + "/v1/systemone")
                        .body(req)
                        .retrieve()
                        .body(JsonNode.class);

        if (root == null) {
            throw new IllegalStateException(
                    "Empty System One response");
        }


        /*
         * ---------------------------------------------------------
         * 6. PARSE FRAUD RISK
         * ---------------------------------------------------------
         */

        JsonNode risk =
                root.path("answers")
                        .path("fraudRisk");

        if (!risk.hasNonNull("choice")) {
            throw new IllegalStateException(
                    "System One response missing fraudRisk.choice: "
                            + root);
        }

        String choice =
                risk.path("choice")
                        .asText()
                        .toUpperCase();

        double confidence =
                risk.path("confidence")
                        .asDouble(0.0);

        double lowProbability =
                risk.path("probabilities")
                        .path("LOW")
                        .asDouble(0.0);

        double mediumProbability =
                risk.path("probabilities")
                        .path("MEDIUM")
                        .asDouble(0.0);

        double highProbability =
                risk.path("probabilities")
                        .path("HIGH")
                        .asDouble(0.0);


        /*
         * ---------------------------------------------------------
         * 7. PARSE ESCALATION
         * ---------------------------------------------------------
         */

        JsonNode escalation =
                root.path("answers")
                        .path("escalation");

        double escalationProbability =
                escalation.path("noul")
                        .asDouble(
                                escalation.path("confidence")
                                        .asDouble(0.0)
                        );


        /*
         * ---------------------------------------------------------
         * 8. USAGE
         * ---------------------------------------------------------
         */

        long inputTokens =
                root.path("usage")
                        .path("input_tokens")
                        .asLong(0);

        long outputTokens =
                root.path("usage")
                        .path("output_tokens")
                        .asLong(0);


        /*
         * ---------------------------------------------------------
         * 9. RESULT
         * ---------------------------------------------------------
         */

        return new SystemOneDecision(
                FraudRisk.valueOf(choice),

                confidence,

                lowProbability,
                mediumProbability,
                highProbability,

                escalationProbability,

                root.path("model")
                        .asText(
                                props.getSystemOne().getModel()
                        ),

                inputTokens,
                outputTokens
        );
    }


    private double round(double value) {

        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
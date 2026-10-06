# Fraud Risk POC

End-to-end local benchmark comparing:

1. **Architecture 1:** every transaction -> Qwen via Ollama
2. **Architecture 2:** every transaction -> System One/Laya -> LOW + confidence >= 0.70 stops; otherwise -> Qwen

The same deterministic 10,000-transaction dataset is used for both architectures.

## Dataset

- 1,000 customers
- 10,000 transactions
- 9,000 safe
- 1,000 fraud
- deterministic random seed: 42
- multiple fraud patterns
- some safe transactions intentionally contain anomalies

`actualFraud` and `fraudPattern` are benchmark ground truth. They must never be sent to either model.

## Prerequisites

- Java 17+
- Maven 3.9+
- Ollama
- Qwen 3 4B (or another Ollama chat model)
- Ollaya/Laya for Architecture 2

## Start Ollama

```bash
ollama pull qwen3:1.7b
ollama serve
```

Ollama should be available at `http://localhost:11434`.

## Generate the dataset

The repository already contains a generated dataset. To regenerate it deterministically:

```bash
mvn -q -DskipTests package
java -cp target/classes:$(find ~/.m2/repository/com/fasterxml/jackson/core -name '*.jar' | tr '\n' ':') com.example.fraudpoc.dataset.FraudDatasetGenerator
```

For normal use, keep the checked-in `fraud-dataset.json` so benchmark runs are reproducible.

## Run application

```bash
mvn spring-boot:run
```

## Single transaction smoke test

```bash
curl -X POST http://localhost:8080/api/v1/transaction-risk/evaluate \
  -H 'Content-Type: application/json' \
  -d @scripts/sample-request.json
```

## Architecture 1 benchmark

```bash
curl -X POST http://localhost:8080/api/v1/benchmark/architecture-1
```

Every transaction goes to Qwen.

## Architecture 2 benchmark

Start Ollaya/Laya on port 11435, then:

```bash
curl -X POST http://localhost:8080/api/v1/benchmark/architecture-2
```

## Full comparison

```bash
curl -X POST http://localhost:8080/api/v1/benchmark/compare
```

This runs both architectures on the same dataset and reports:

- accuracy
- precision
- recall
- F1
- false negatives
- P50/P95/P99 latency
- Qwen calls
- System One calls
- gate stops
- gate false negatives
- input/output/total Qwen tokens
- Qwen call reduction
- token reduction
- average-latency reduction

## Important benchmark interpretation

For binary metrics, LOW is treated as predicted non-fraud and MEDIUM/HIGH as predicted fraud.

For Architecture 2, `gateFalseNegatives` means an actually fraudulent transaction was stopped by the System One gate (`LOW` with confidence >= threshold).

The threshold is configurable:

```yaml
fraud:
  system-one:
    confidence-threshold: 0.70
```

Do not assume 0.70 is universally optimal. Later, run the benchmark at 0.50, 0.60, 0.70, 0.80 and 0.90 and compare the resulting trade-offs.

## Ollaya / System One wire protocol

Architecture 2 uses `POST /v1/systemone` and sends typed questions for fraud risk and escalation. The client expects the TypeSafe/Jev-compatible response shape:

```json
{
  "answers": {
    "fraudRisk": {
      "choice": "LOW",
      "confidence": 0.91
    },
    "escalation": {
      "noul": 0.12,
      "confidence": 0.88
    }
  }
}
```

## Notes

The current project intentionally uses the native Ollama HTTP API rather than coupling the benchmark to a particular Spring AI minor-version API. This keeps the benchmark instrumentation stable while the existing Ollama/Spring AI showcase can remain untouched. The application can later replace `OllamaFraudClient` with Spring AI without changing the dataset or benchmark contracts.

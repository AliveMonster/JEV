#!/usr/bin/env bash
set -euo pipefail
mvn -q -DskipTests package
java -cp "target/classes:$(find "$HOME/.m2/repository/com/fasterxml/jackson" -name '*.jar' | tr '\n' ':')" \
  com.example.fraudpoc.dataset.FraudDatasetGenerator \
  src/main/resources/datasets/fraud-dataset.json

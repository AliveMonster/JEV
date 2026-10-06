#!/usr/bin/env bash
set -euo pipefail
curl -sS -X POST http://localhost:8080/api/v1/transaction-risk/evaluate \
  -H 'Content-Type: application/json' \
  -d @scripts/sample-request.json | jq .

#!/usr/bin/env bash
set -euo pipefail
case "${1:-compare}" in
  a1) curl -sS -X POST http://localhost:8080/api/v1/benchmark/architecture-1 | jq . ;;
  a2) curl -sS -X POST http://localhost:8080/api/v1/benchmark/architecture-2 | jq . ;;
  compare) curl -sS -X POST http://localhost:8080/api/v1/benchmark/compare | jq . ;;
  *) echo "Usage: $0 {a1|a2|compare}"; exit 1;;
esac

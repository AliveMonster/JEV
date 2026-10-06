package com.example.fraudpoc.jev;

import java.util.Map;

public record JevRequest(
        String model,
        Object state,
        Map<String, Object> questions
) {
}
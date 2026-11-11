package com.interactionchains.app.analysis;

public record TraceHopView(
        int sequence,
        String applicationName,
        String operationName,
        String time,
        String status) {
}

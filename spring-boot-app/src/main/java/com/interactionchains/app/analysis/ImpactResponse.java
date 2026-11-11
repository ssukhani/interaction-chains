package com.interactionchains.app.analysis;

import java.util.List;

public record ImpactResponse(
        String applicationId,
        String applicationName,
        String operationId,
        String operationName,
        List<ChainView> journeys) {
}

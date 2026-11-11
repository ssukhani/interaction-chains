package com.interactionchains.app.analysis;

import java.util.List;

public record EstateDocument(
        List<ChainApplication> applications,
        List<ChainOperation> operations,
        List<JourneyDefinition> journeys,
        List<TraceRecord> traces,
        List<DeclaredEdge> declaredEdges) {
}

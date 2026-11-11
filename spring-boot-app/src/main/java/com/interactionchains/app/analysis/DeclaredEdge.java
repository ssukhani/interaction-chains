package com.interactionchains.app.analysis;

public record DeclaredEdge(
        String id,
        String source,
        String journeyId,
        String fromApplicationId,
        String toApplicationId) {
}

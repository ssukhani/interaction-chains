package com.interactionchains.app.analysis;

public record SideFact(
        String source,
        String fromApplicationName,
        String toApplicationName,
        String label,
        boolean seenInTrace) {
}

package com.interactionchains.app.analysis;

import java.util.List;

public record ResolvedHop(
        int sequence,
        String applicationId,
        String applicationName,
        String team,
        String operationId,
        String operationName,
        List<Evidence> evidence,
        boolean seenInTrace,
        boolean highlighted,
        boolean proposed) {

    public ResolvedHop highlight() {
        return new ResolvedHop(
                sequence,
                applicationId,
                applicationName,
                team,
                operationId,
                operationName,
                evidence,
                seenInTrace,
                true,
                proposed);
    }

    public ResolvedHop atSequence(int nextSequence) {
        return new ResolvedHop(
                nextSequence,
                applicationId,
                applicationName,
                team,
                operationId,
                operationName,
                evidence,
                seenInTrace,
                highlighted,
                proposed);
    }
}

package com.interactionchains.app.analysis;

import java.util.List;

public record ChainView(
        String journeyId,
        String name,
        String channel,
        List<String> traceIds,
        List<ResolvedHop> hops,
        List<SideFact> sideFacts) {
}

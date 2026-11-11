package com.interactionchains.app.analysis;

import java.util.List;

public record TraceView(String id, String journeyId, String journeyName, List<TraceHopView> hops) {
}

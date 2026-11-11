package com.interactionchains.app.analysis;

import java.util.List;

public record JourneyDefinition(String id, String name, String channel, List<JourneyHopRef> hops) {
}

package com.interactionchains.app.analysis;

import java.util.List;

public record TraceRecord(String id, String journeyId, List<TraceSpan> spans) {
}

package com.interactionchains.app.analysis;

import java.util.List;

public record InterpretResponse(
        String type,
        String field,
        String question,
        List<Choice> options,
        Intent intent,
        List<JourneySummary> journeys,
        String message) {

    public static InterpretResponse clarify(String field, String question, List<Choice> options) {
        return new InterpretResponse("clarify", field, question, List.copyOf(options), null, null, null);
    }

    public static InterpretResponse shortlist(Intent intent, List<JourneySummary> journeys) {
        return new InterpretResponse("shortlist", null, null, null, intent, List.copyOf(journeys), null);
    }

    public static InterpretResponse none(String message) {
        return new InterpretResponse("none", null, null, null, null, null, message);
    }
}

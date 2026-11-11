package com.interactionchains.app.analysis;

import java.util.List;

public record InterpretRequest(
        String text,
        String channel,
        List<String> channels,
        String capability,
        Boolean clarifyUsed) {

    public boolean clarifyAlreadyUsed() {
        return Boolean.TRUE.equals(clarifyUsed);
    }
}

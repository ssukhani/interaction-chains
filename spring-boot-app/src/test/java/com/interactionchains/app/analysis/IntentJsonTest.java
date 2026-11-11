package com.interactionchains.app.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class IntentJsonTest {

    @Test
    void readsThePythonIntentPayload() {
        Intent intent = JsonMapper.builder().build().readValue(
                "{\"changeType\":\"add\",\"subject\":\"sanctions-screening\",\"channel\":null,\"capability\":null}",
                Intent.class);
        assertEquals("add", intent.changeType());
        assertEquals("sanctions-screening", intent.subject());
    }
}

package com.interactionchains.app.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PythonIntentClientTest {

    @Test
    void readsTheInterpreterPayload() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://python.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://python.test/interpret"))
                .andExpect(content().json("{\"text\":\"Add sanctions screening to web account opening\"}"))
                .andRespond(withSuccess(
                        "{\"changeType\":\"add\",\"subject\":\"sanctions-screening\",\"channel\":\"web\",\"capability\":null}",
                        MediaType.APPLICATION_JSON));

        Intent intent = new PythonIntentClient(builder.build())
                .interpret("Add sanctions screening to web account opening");

        assertEquals("add", intent.changeType());
        assertEquals("sanctions-screening", intent.subject());
        assertEquals("web", intent.channel());
        server.verify();
    }
}

package com.interactionchains.app.analysis;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PythonIntentClient implements IntentClient {

    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public PythonIntentClient(RestClient pythonRestClient) {
        this.restClient = pythonRestClient;
    }

    @Override
    public Intent interpret(String text) {
        try {
            byte[] request = jsonMapper.writeValueAsBytes(Map.of("text", text));
            String payload = restClient.post()
                    .uri("/interpret")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            if (payload == null || payload.isBlank()) {
                throw new IntentServiceException("Intent service returned an empty response.");
            }
            return jsonMapper.readValue(payload, Intent.class);
        } catch (RestClientException exception) {
            throw new IntentServiceException(
                    "Intent service is unavailable. Start the Python service on port 8081.");
        } catch (JacksonException exception) {
            throw new IntentServiceException("Intent service returned a response that could not be read.");
        }
    }
}

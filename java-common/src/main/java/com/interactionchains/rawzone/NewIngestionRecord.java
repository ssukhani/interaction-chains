package com.interactionchains.rawzone;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.Objects;

public record NewIngestionRecord(
        OffsetDateTime executionDatetime,
        String dataType,
        String dataFeed,
        JsonNode icMetaData,
        JsonNode icMetric,
        JsonNode mauiMetric,
        JsonNode entityMetric) {

    public NewIngestionRecord {
        Objects.requireNonNull(executionDatetime, "executionDatetime is required");
        Objects.requireNonNull(dataType, "dataType is required");
    }
}

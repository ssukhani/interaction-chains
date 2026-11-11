package com.interactionchains.rawzone;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

public record IngestionRecord(
        long id,
        OffsetDateTime executionDatetime,
        String dataType,
        String dataFeed,
        IngestionStatus status,
        String statusReason,
        JsonNode icMetaData,
        JsonNode icMetric,
        JsonNode mauiMetric,
        JsonNode entityMetric,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}

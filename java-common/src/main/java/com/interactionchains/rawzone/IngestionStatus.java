package com.interactionchains.rawzone;

public enum IngestionStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    SUCCESS("success"),
    FAILED("failed");

    private final String databaseValue;

    IngestionStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static IngestionStatus fromDatabaseValue(String databaseValue) {
        for (IngestionStatus status : values()) {
            if (status.databaseValue.equals(databaseValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ingestion status: " + databaseValue);
    }
}

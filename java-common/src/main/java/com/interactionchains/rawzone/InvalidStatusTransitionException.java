package com.interactionchains.rawzone;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(long id, IngestionStatus from, IngestionStatus to) {
        super("Cannot transition ingestion record " + id + " from " + from.databaseValue()
                + " to " + to.databaseValue());
    }
}

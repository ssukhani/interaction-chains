package com.interactionchains.rawzone;

import java.sql.SQLException;

public interface RawZoneStatusUpdater {
    void markProcessing(long id) throws SQLException;

    void markSuccess(long id) throws SQLException;

    void markFailed(long id, String reason) throws SQLException;
}

package com.interactionchains.rawzone;

import java.sql.SQLException;

public interface RawZoneRepository {
    IngestionRecord insert(NewIngestionRecord record) throws SQLException;
}

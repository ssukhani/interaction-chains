package com.interactionchains.rawzone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import javax.sql.DataSource;
import org.postgresql.util.PGobject;

public class JdbcRawZoneRepository implements RawZoneRepository {
    private static final String INSERT_SQL = """
            INSERT INTO fbsi.daily_data_feed (
                execution_datetime,
                data_type,
                data_feed,
                status,
                ic_meta_data,
                ic_metric,
                maui_metric,
                entity_metric
            )
            VALUES (?, ?, ?, 'pending', ?, ?, ?, ?)
            RETURNING
                id,
                execution_datetime,
                data_type,
                data_feed,
                status,
                status_reason,
                ic_meta_data,
                ic_metric,
                maui_metric,
                entity_metric,
                created_at,
                updated_at
            """;

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public JdbcRawZoneRepository(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public IngestionRecord insert(NewIngestionRecord record) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
                statement.setObject(1, record.executionDatetime());
                statement.setString(2, record.dataType());
                statement.setString(3, record.dataFeed());
                setJsonb(statement, 4, record.icMetaData());
                setJsonb(statement, 5, record.icMetric());
                setJsonb(statement, 6, record.mauiMetric());
                setJsonb(statement, 7, record.entityMetric());

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Insert into fbsi.daily_data_feed returned no row");
                    }
                    IngestionRecord inserted = mapRecord(resultSet);
                    connection.commit();
                    return inserted;
                }
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private void setJsonb(PreparedStatement statement, int parameterIndex, JsonNode jsonNode) throws SQLException {
        PGobject jsonb = toJsonb(jsonNode);
        if (jsonb == null) {
            statement.setNull(parameterIndex, Types.OTHER);
            return;
        }
        statement.setObject(parameterIndex, jsonb);
    }

    private PGobject toJsonb(JsonNode jsonNode) throws SQLException {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }

        PGobject jsonb = new PGobject();
        jsonb.setType("jsonb");
        try {
            jsonb.setValue(objectMapper.writeValueAsString(jsonNode));
        } catch (JsonProcessingException exception) {
            throw new SQLException("Unable to serialize raw-zone payload JSON", exception);
        }
        return jsonb;
    }

    private IngestionRecord mapRecord(ResultSet resultSet) throws SQLException {
        return new IngestionRecord(
                resultSet.getLong("id"),
                resultSet.getObject("execution_datetime", java.time.OffsetDateTime.class),
                resultSet.getString("data_type"),
                resultSet.getString("data_feed"),
                IngestionStatus.fromDatabaseValue(resultSet.getString("status")),
                resultSet.getString("status_reason"),
                readJson(resultSet, "ic_meta_data"),
                readJson(resultSet, "ic_metric"),
                readJson(resultSet, "maui_metric"),
                readJson(resultSet, "entity_metric"),
                toOffsetDateTime(resultSet, "created_at"),
                toOffsetDateTime(resultSet, "updated_at"));
    }

    private JsonNode readJson(ResultSet resultSet, String columnName) throws SQLException {
        String value = resultSet.getString(columnName);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new SQLException("Unable to parse " + columnName + " JSON", exception);
        }
    }

    private java.time.OffsetDateTime toOffsetDateTime(ResultSet resultSet, String columnName) throws SQLException {
        Object value = resultSet.getObject(columnName);
        if (value instanceof java.time.OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC);
        }
        if (value == null || resultSet.wasNull()) {
            return null;
        }
        throw new SQLException("Unsupported timestamp type for " + columnName + ": " + value.getClass());
    }
}

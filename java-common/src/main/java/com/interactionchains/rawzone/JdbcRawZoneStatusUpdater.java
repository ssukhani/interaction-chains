package com.interactionchains.rawzone;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

public class JdbcRawZoneStatusUpdater implements RawZoneStatusUpdater {
    private static final String UPDATE_STATUS_SQL = """
            UPDATE fbsi.daily_data_feed
            SET status = ?, updated_at = now()
            WHERE id = ? AND status = ?
            """;

    private static final String UPDATE_FAILED_SQL = """
            UPDATE fbsi.daily_data_feed
            SET status = ?, status_reason = ?, updated_at = now()
            WHERE id = ? AND status = ?
            """;

    private final DataSource dataSource;

    public JdbcRawZoneStatusUpdater(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void markProcessing(long id) throws SQLException {
        transition(id, IngestionStatus.PENDING, IngestionStatus.PROCESSING);
    }

    @Override
    public void markSuccess(long id) throws SQLException {
        transition(id, IngestionStatus.PROCESSING, IngestionStatus.SUCCESS);
    }

    @Override
    public void markFailed(long id, String reason) throws SQLException {
        transitionToFailed(id, reason);
    }

    private void transition(long id, IngestionStatus from, IngestionStatus to) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS_SQL)) {
                statement.setString(1, to.databaseValue());
                statement.setLong(2, id);
                statement.setString(3, from.databaseValue());

                int updatedRows = statement.executeUpdate();
                if (updatedRows != 1) {
                    throw new InvalidStatusTransitionException(id, from, to);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private void transitionToFailed(long id, String reason) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_FAILED_SQL)) {
                statement.setString(1, IngestionStatus.FAILED.databaseValue());
                statement.setString(2, reason);
                statement.setLong(3, id);
                statement.setString(4, IngestionStatus.PROCESSING.databaseValue());

                int updatedRows = statement.executeUpdate();
                if (updatedRows != 1) {
                    throw new InvalidStatusTransitionException(
                            id, IngestionStatus.PROCESSING, IngestionStatus.FAILED);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }
}

CREATE SCHEMA IF NOT EXISTS fbsi;

CREATE TABLE fbsi.daily_data_feed (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    execution_datetime timestamptz NOT NULL,
    data_type text NOT NULL,
    data_feed text,
    status text NOT NULL DEFAULT 'pending',
    status_reason text,
    ic_meta_data jsonb,
    ic_metric jsonb,
    maui_metric jsonb,
    entity_metric jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_daily_data_feed_status
        CHECK (status IN ('pending', 'processing', 'success', 'failed'))
);

CREATE INDEX ix_daily_data_feed_execution_datetime
    ON fbsi.daily_data_feed (execution_datetime);

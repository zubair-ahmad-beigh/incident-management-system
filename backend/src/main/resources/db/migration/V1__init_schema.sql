-- V1__init_schema.sql  (MySQL 8.0+)
-- IDs are CHAR(36) – populated by the application (UUID.randomUUID())
-- DATETIME(6) gives microsecond precision; all values stored in UTC

-- ─────────────────────────────────────────────
--  incidents
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS incidents (
    id              CHAR(36)        NOT NULL,
    component_id    VARCHAR(255)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    severity        VARCHAR(10)     NOT NULL DEFAULT 'P2',
    start_time      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    end_time        DATETIME(6),
    mttr_minutes    BIGINT,
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                    ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    -- CHECK constraints (supported MySQL 8.0.16+)
    CONSTRAINT chk_incident_status
        CHECK (status IN ('OPEN','INVESTIGATING','RESOLVED','CLOSED')),
    CONSTRAINT chk_incident_severity
        CHECK (severity IN ('P0','P1','P2'))

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────
--  rca
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS rca (
    id                  CHAR(36)    NOT NULL,
    incident_id         CHAR(36)    NOT NULL,
    root_cause          TEXT        NOT NULL,
    fix_applied         TEXT        NOT NULL,
    prevention_steps    TEXT        NOT NULL,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                    ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT uq_rca_incident   UNIQUE      (incident_id),
    CONSTRAINT fk_rca_incident   FOREIGN KEY (incident_id)
                                 REFERENCES  incidents(id)
                                 ON DELETE   CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────
--  Indexes
-- ─────────────────────────────────────────────
CREATE INDEX idx_incidents_component_id ON incidents (component_id);
CREATE INDEX idx_incidents_status       ON incidents (status);
CREATE INDEX idx_incidents_start_time   ON incidents (start_time DESC);
-- idx_rca_incident_id is already covered by the UNIQUE constraint above

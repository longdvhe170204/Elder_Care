-- Bảng cảnh báo theo dõi và khẩn cấp (nhóm 2 - có trạng thái, FR-026 đến FR-034, DBR-18, S-08).
CREATE TABLE alert
(
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id              BIGINT NULL,
    floor_area_id            BIGINT NULL,
    type                     VARCHAR(64) NOT NULL,
    type_lock                VARCHAR(128) NOT NULL,
    level                    ENUM ('LOW', 'MEDIUM', 'CRITICAL') NOT NULL,
    level_at_received        ENUM ('LOW', 'MEDIUM', 'CRITICAL') NULL,
    occurrence_count         INT NOT NULL DEFAULT 1,
    first_at                 DATETIME(6) NOT NULL,
    latest_at                DATETIME(6) NOT NULL,
    status                   ENUM ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS', 'CLOSED', 'CONVERTED_TO_INCIDENT') NOT NULL DEFAULT 'NEW',
    escalation_level         TINYINT NOT NULL DEFAULT 0,
    acknowledgement_deadline DATETIME(6) NULL,
    max_escalation_at        DATETIME(6) NULL,
    max_reminder_count       INT NOT NULL DEFAULT 0,
    assignee_id              BIGINT NULL,
    handover_acknowledged    BOOLEAN NOT NULL DEFAULT FALSE,
    receiver_id              BIGINT NULL,
    received_at              DATETIME(6) NULL,
    resolution               VARCHAR(200) NULL,
    close_reason             VARCHAR(500) NULL,
    closed_by_id             BIGINT NULL,
    closed_by_system         BOOLEAN NOT NULL DEFAULT FALSE,
    closed_at                DATETIME(6) NULL,
    incident_id              BIGINT NULL,
    critical_flag_id         BIGINT NULL,
    no_on_duty_doctor        BOOLEAN NOT NULL DEFAULT FALSE,
    escalation_stopped       BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_id            BIGINT NULL,
    description              VARCHAR(1000) NULL,
    requester_id             BIGINT NULL,
    arrived_at               DATETIME(6) NULL,
    arrived_by_id            BIGINT NULL,
    skip_arrival_time        BOOLEAN NOT NULL DEFAULT FALSE,
    open_lock                VARCHAR(191) GENERATED ALWAYS AS
        (IF(status IN ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS'), type_lock, NULL)) STORED,
    open_assist_lock         BIGINT GENERATED ALWAYS AS
        (IF(type = 'NV_YEU_CAU_HO_TRO' AND status IN ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS'), requester_id, NULL)) STORED,
    version                  INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_alert_resident CHECK (resident_id IS NOT NULL OR type = 'NV_YEU_CAU_HO_TRO'),
    CONSTRAINT uq_alert_open_lock UNIQUE (resident_id, open_lock),
    CONSTRAINT uq_alert_open_assist_lock UNIQUE (open_assist_lock),
    KEY idx_alert_status_deadline (status, acknowledgement_deadline),
    KEY idx_alert_floor_status (floor_area_id, status),
    KEY idx_alert_resident_status (resident_id, status),
    KEY idx_alert_assignee_status (assignee_id, status)
) COMMENT = 'DataGroup=STATEFUL';

DELIMITER $$
CREATE TRIGGER trg_alert_update_guard BEFORE UPDATE ON alert FOR EACH ROW
BEGIN
    -- Chặn đổi khi đã đóng hoặc chuyển sự cố, trừ khi chỉ cập nhật version
    IF OLD.status IN ('CLOSED', 'CONVERTED_TO_INCIDENT') THEN
        IF NOT (NEW.resident_id <=> OLD.resident_id
            AND NEW.floor_area_id <=> OLD.floor_area_id
            AND NEW.type <=> OLD.type
            AND NEW.type_lock <=> OLD.type_lock
            AND NEW.level <=> OLD.level
            AND NEW.level_at_received <=> OLD.level_at_received
            AND NEW.occurrence_count <=> OLD.occurrence_count
            AND NEW.first_at <=> OLD.first_at
            AND NEW.latest_at <=> OLD.latest_at
            AND NEW.status <=> OLD.status
            AND NEW.escalation_level <=> OLD.escalation_level
            AND NEW.acknowledgement_deadline <=> OLD.acknowledgement_deadline
            AND NEW.max_escalation_at <=> OLD.max_escalation_at
            AND NEW.max_reminder_count <=> OLD.max_reminder_count
            AND NEW.assignee_id <=> OLD.assignee_id
            AND NEW.handover_acknowledged <=> OLD.handover_acknowledged
            AND NEW.receiver_id <=> OLD.receiver_id
            AND NEW.received_at <=> OLD.received_at
            AND NEW.resolution <=> OLD.resolution
            AND NEW.close_reason <=> OLD.close_reason
            AND NEW.closed_by_id <=> OLD.closed_by_id
            AND NEW.closed_by_system <=> OLD.closed_by_system
            AND NEW.closed_at <=> OLD.closed_at
            AND NEW.incident_id <=> OLD.incident_id
            AND NEW.critical_flag_id <=> OLD.critical_flag_id
            AND NEW.no_on_duty_doctor <=> OLD.no_on_duty_doctor
            AND NEW.escalation_stopped <=> OLD.escalation_stopped
            AND NEW.created_by_id <=> OLD.created_by_id
            AND NEW.description <=> OLD.description
            AND NEW.requester_id <=> OLD.requester_id
            AND NEW.arrived_at <=> OLD.arrived_at
            AND NEW.arrived_by_id <=> OLD.arrived_by_id
            AND NEW.skip_arrival_time <=> OLD.skip_arrival_time) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'closed or converted alert is immutable';
        END IF;
    END IF;

    -- Chặn đổi type_lock và first_at
    IF NOT (NEW.type_lock <=> OLD.type_lock) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alert type_lock is immutable';
    END IF;
    IF NOT (NEW.first_at <=> OLD.first_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alert first_at is immutable';
    END IF;

    -- Chặn đổi resident_id (trừ khi gán nct_id lần đầu cho NV_YEU_CAU_HO_TRO)
    IF NOT (NEW.resident_id <=> OLD.resident_id) THEN
        IF NOT (OLD.resident_id IS NULL AND NEW.resident_id IS NOT NULL AND OLD.type = 'NV_YEU_CAU_HO_TRO') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alert resident_id is immutable once set';
        END IF;
    END IF;

    -- Chặn đổi arrived_at khi đã có
    IF OLD.arrived_at IS NOT NULL AND NOT (NEW.arrived_at <=> OLD.arrived_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alert arrived_at is immutable once set';
    END IF;
END$$

CREATE TRIGGER trg_alert_no_delete BEFORE DELETE ON alert FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alert rows are removed only by retention job';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON alert TO 'eldercare_app'@'%';
GRANT DELETE ON alert TO 'eldercare_retention'@'%';


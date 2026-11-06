-- 014 T008 (data-model mục 0, 3, 4; R-15): hai bảng riêng của feature. Cột chuyến đi (1–1) thêm vào activity_session ở
-- T016, người tham gia chuyến vào activity_registration. Tên dịch theo glossary 20.6.
CREATE TABLE activity_session (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 activity_id BIGINT NOT NULL,
 starts_at DATETIME(6) NOT NULL,
 ends_at DATETIME(6) NOT NULL,
 source ENUM('TEMPLATE','SINGLE') NOT NULL,
 location_room_id BIGINT NULL,
 location_area_id BIGINT NULL,
 floor_area_id BIGINT NULL,
 responsible_staff_id BIGINT NULL,
 status ENUM('SCHEDULED','ATTENDANCE_TAKEN','NO_ATTENDANCE','CANCELLED') NOT NULL,
 incomplete_attendance BOOLEAN NOT NULL DEFAULT FALSE,
 cancel_reason VARCHAR(2000) NULL,
 cancelled_by ENUM('STAFF','TEMPLATE_CHANGED','DEACTIVATED','ISOLATION') NULL,
 cancelled_by_staff_id BIGINT NULL,
 attendance_reminded_at DATETIME(6) NULL,
 created_by BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 version INT NOT NULL DEFAULT 0,
 CONSTRAINT uq_activity_session_start UNIQUE (activity_id, starts_at),
 CONSTRAINT ck_activity_session_time CHECK (ends_at > starts_at),
 CONSTRAINT ck_activity_session_cancel CHECK ((status = 'CANCELLED') = (cancelled_by IS NOT NULL)),
 INDEX ix_activity_session_floor (floor_area_id, starts_at),
 INDEX ix_activity_session_status (status, starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DataGroup=BUSINESS';

CREATE TABLE activity_registration (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 session_id BIGINT NOT NULL,
 resident_id BIGINT NOT NULL,
 source ENUM('REGULAR','MANUAL','ADDED_AT_ATTENDANCE') NOT NULL,
 registered_by_staff_id BIGINT NULL,
 registered_at DATETIME(6) NOT NULL,
 status ENUM('REGISTERED','CANCELLED','ATTENDED') NOT NULL,
 cancel_reason VARCHAR(2000) NULL,
 auto_cancel ENUM('TEMPLATE_CHANGED','SESSION_CHANGED','ISOLATION','INFECTION_SUSPECTED','FINAL_STATUS','CONSENT_WITHDRAWN','SESSION_CANCELLED') NULL,
 acknowledged_warnings JSON NULL,
 late_return_consent_id BIGINT NULL,
 version INT NOT NULL DEFAULT 0,
 active_key VARCHAR(60) GENERATED ALWAYS AS (IF(status <> 'CANCELLED', CONCAT(session_id, ':', resident_id), NULL)) STORED,
 CONSTRAINT uq_activity_registration_active UNIQUE (active_key),
 CONSTRAINT fk_activity_registration_session FOREIGN KEY (session_id) REFERENCES activity_session (id),
 INDEX ix_activity_registration_resident (resident_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DataGroup=BUSINESS';

GRANT INSERT, UPDATE ON activity_session TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON activity_registration TO 'eldercare_app'@'%';
GRANT DELETE ON activity_session TO 'eldercare_retention'@'%';
GRANT DELETE ON activity_registration TO 'eldercare_retention'@'%';

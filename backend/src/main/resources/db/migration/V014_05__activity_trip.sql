-- 014 T016, FR-034, data-model mục 0 và 6: chuyến và người tham gia dùng hai bảng hiện có.
-- Nhiệm vụ dùng status_history; đánh giá, điểm danh và gia hạn dùng record_event (R-15).
ALTER TABLE activity_session
 ADD COLUMN trip_destination VARCHAR(2000) NULL,
 ADD COLUMN trip_planned_departure_at DATETIME(6) NULL,
 ADD COLUMN trip_planned_return_at DATETIME(6) NULL,
 ADD COLUMN trip_transport VARCHAR(2000) NULL,
 ADD COLUMN trip_facility_vehicle BOOLEAN NULL,
 ADD COLUMN trip_status ENUM('SCHEDULED','IN_PROGRESS','RETURNED','CANCELLED') NULL,
 ADD COLUMN trip_overdue_return BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN trip_overdue_notified_for DATETIME(6) NULL,
 ADD COLUMN trip_manager_notified_for DATETIME(6) NULL,
 ADD COLUMN trip_remote_leader BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN trip_return_finished_at DATETIME(6) NULL,
 ADD COLUMN trip_return_finished_by_staff_id BIGINT NULL,
 ADD CONSTRAINT ck_activity_session_trip CHECK (
  (trip_status IS NULL AND trip_destination IS NULL AND trip_planned_departure_at IS NULL
   AND trip_planned_return_at IS NULL AND trip_transport IS NULL AND trip_facility_vehicle IS NULL)
  OR
  (trip_status IS NOT NULL AND trip_destination IS NOT NULL AND CHAR_LENGTH(TRIM(trip_destination)) > 0
   AND trip_transport IS NOT NULL AND CHAR_LENGTH(TRIM(trip_transport)) > 0
   AND trip_planned_departure_at IS NOT NULL AND trip_planned_return_at IS NOT NULL
   AND trip_planned_return_at > trip_planned_departure_at AND trip_facility_vehicle IS NOT NULL)
 ),
 ADD INDEX ix_activity_session_trip (trip_status, trip_planned_departure_at);

ALTER TABLE activity_registration
 ADD COLUMN participant_status ENUM('REGISTERED','ELIGIBLE','NOT_GOING','DEPARTED','RETURNED','MISSING_ON_RETURN','LEFT_GROUP') NULL,
 ADD COLUMN participant_reason VARCHAR(2000) NULL,
 ADD COLUMN missing_incident_id BIGINT NULL,
 ADD COLUMN missing_alert_requested BOOLEAN NOT NULL DEFAULT FALSE;

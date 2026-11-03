-- T028/R-15: chỉ mở rộng ràng buộc của loại M04 trên hồ sơ chung; không tạo bảng riêng.
ALTER TABLE simple_record
 ADD COLUMN m04_attendance_date DATE GENERATED ALWAYS AS
  (IF(kind='M04.DAY_CARE_ATTENDANCE',effective_from,NULL)) STORED,
 ADD COLUMN m04_expected_arrival TIME GENERATED ALWAYS AS
  (CASE WHEN kind='M04.DAY_CARE_ATTENDANCE' AND JSON_TYPE(JSON_EXTRACT(content,'$.expectedArrival'))='STRING'
   THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(content,'$.expectedArrival')) AS TIME) ELSE NULL END) STORED,
 ADD CONSTRAINT uq_m04_attendance_day UNIQUE(resident_id,m04_attendance_date),
 ADD CONSTRAINT ck_m04_attendance_state CHECK
  (kind<>'M04.DAY_CARE_ATTENDANCE' OR
   (resident_id IS NOT NULL AND effective_from IS NOT NULL AND
    status IN('NOT_ARRIVED','PRESENT','LEFT','REPORTED_ABSENT','UNREPORTED_ABSENT','HOLIDAY_CANCELLED'))),
 ADD INDEX ix_m04_attendance_due(kind,status,m04_attendance_date,m04_expected_arrival),
 ADD INDEX ix_m04_attendance_day(m04_attendance_date,status);

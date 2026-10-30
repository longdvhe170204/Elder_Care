-- Audit cơ sở dữ liệu F-04 (2026-10-29): bảng không có @Entity khai nhóm dữ liệu bằng COMMENT để RecordGroupTriggerIT
-- kiểm được trigger (mục 1.5; research R-05, R-15). Giá trị: CATALOG, BUSINESS, RECORD, MULTIPLE, TECHNICAL. Bảng mới
-- của mọi module ghi COMMENT = 'DataGroup=…' khi tạo (backend/README.md mục 4).
ALTER TABLE approval_request COMMENT = 'DataGroup=MULTIPLE';
ALTER TABLE audit_log COMMENT = 'DataGroup=RECORD';
ALTER TABLE catalog_item COMMENT = 'DataGroup=CATALOG';
ALTER TABLE correction_chain COMMENT = 'DataGroup=TECHNICAL';
ALTER TABLE EVENT_PUBLICATION COMMENT = 'DataGroup=TECHNICAL';
ALTER TABLE idempotency_key COMMENT = 'DataGroup=TECHNICAL';
ALTER TABLE parameter COMMENT = 'DataGroup=CATALOG';
ALTER TABLE parameter_history COMMENT = 'DataGroup=RECORD';
ALTER TABLE record_event COMMENT = 'DataGroup=RECORD';
ALTER TABLE reminder_marker COMMENT = 'DataGroup=TECHNICAL';
ALTER TABLE resident_module_state COMMENT = 'DataGroup=BUSINESS';
ALTER TABLE shedlock COMMENT = 'DataGroup=TECHNICAL';
ALTER TABLE simple_record COMMENT = 'DataGroup=MULTIPLE';
ALTER TABLE status_history COMMENT = 'DataGroup=RECORD';
ALTER TABLE technical_lock COMMENT = 'DataGroup=TECHNICAL';

-- T038: đọc việc tồn từ ca trước; producer đổi dấu thuộc lệnh bàn giao/Phase 7.
ALTER TABLE care_task
 ADD COLUMN carryover ENUM('FROM_PREVIOUS_SHIFT','AWAITING_HANDOVER_CONFIRMATION') NULL,
 ADD INDEX ix_care_task_carryover(floor_area_id,carryover,performer_id);

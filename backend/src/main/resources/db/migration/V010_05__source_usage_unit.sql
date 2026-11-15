-- 010 T011 (2026-11-10): đơn vị dùng do nguồn thuốc gửi (MedicationBilledEvent.usageUnit) để hiển thị và đối chiếu với
-- đơn vị tính giá của danh mục; buổi bán trú phát sinh ngoài lịch (AttendanceDetermined.adHocSession, FR-005b (a))
-- ghi vào chỉ mục như các nguồn sự kiện khác. Không quy đổi đơn vị.
ALTER TABLE cost_source_index
    ADD COLUMN usage_unit VARCHAR(32) NULL AFTER medication_id,
    MODIFY COLUMN source_type ENUM('CARE_TASK_RESULT','DOSE','PRN_ADMINISTRATION','DISPENSE_RECORD','FAMILY_MEAL',
        'OVERNIGHT_STAY','ACTIVITY_ATTENDANCE','DAY_CARE_SESSION') NOT NULL;

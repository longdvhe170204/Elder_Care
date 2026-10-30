-- 000 T108 phần 2 (research R-15 mục 4): mã lần chạy job là UUID; m13_job_run giữ tới khi 009 T043 gộp vào reminder_marker.
ALTER TABLE m13_job_run MODIFY scheduler_run_id VARCHAR(36) NOT NULL;

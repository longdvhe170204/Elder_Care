-- Q-453 (2026-11-26, mở lại Q-439): công việc lập theo tháng ở trạng thái Nháp cho tới khi Trưởng tầng Công bố công việc tháng.
-- Không tạo bảng mới. Cú pháp dùng được ở MySQL 8.0 và 8.4.
ALTER TABLE care_task MODIFY status
    ENUM('DRAFT','NOT_YET_DUE','DUE','OVERDUE','COMPLETED','COMPLETED_LATE','NOT_PERFORMED','CANCELLED') NOT NULL;

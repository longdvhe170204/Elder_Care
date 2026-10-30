-- Mốc đặt dấu chờ bác sĩ cho nguồn bàn giao 008; dữ liệu cũ chưa biết mốc giữ NULL.
-- bed_allocation giữ nhóm BUSINESS qua entity; không tạo bảng mới hoặc đoán mốc từ start_at.
ALTER TABLE bed_allocation
    ADD COLUMN doctor_confirmation_requested_at DATETIME(6) NULL;

-- T033/FR-004: giao Trưởng tầng tương lai bị kết thúc trước khi bắt đầu có khoảng rỗng,
-- không có thời điểm nào cấp phạm vi. Lệnh giao thông thường vẫn kiểm ends_at > starts_at.
ALTER TABLE floor_manager_assignment
 DROP CHECK floor_manager_assignment_chk_1,
 ADD CONSTRAINT chk_floor_manager_assignment_range CHECK (
  ends_at IS NULL OR ends_at>starts_at OR (status='ENDED' AND ends_at=starts_at)
 );

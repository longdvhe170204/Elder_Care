-- 014 T014 (FR-031, FR-032): nguồn tính phí đang hiệu lực của đăng ký (mã bản ghi điểm danh hoặc đính chính), để đính
-- chính nhiều lần vẫn biết đã có lượt tính phí hay chưa; bản ghi điểm danh là nhóm 3 nên không ghi trạng thái lên đó.
ALTER TABLE activity_registration ADD COLUMN billed_source_id VARCHAR(40) NULL;

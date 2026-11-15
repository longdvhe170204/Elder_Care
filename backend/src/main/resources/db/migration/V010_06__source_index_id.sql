-- 010 T033 (2026-11-10): khóa kỹ thuật cho chỉ mục nguồn để đi theo chuỗi lưu giữ của hồ sơ M01 (RetentionProvider
-- M11.COST_SOURCE_INDEX dùng DataRef long). Không băm mã nguồn, không đổi khóa chính (loại, mã, đơn vị).
ALTER TABLE cost_source_index
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT UNIQUE FIRST;

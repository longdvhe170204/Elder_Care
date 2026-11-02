-- Audit F-04, R-15 (backend/README.md mục 4): khai nhóm dữ liệu cho ba bảng của V010_01. Dòng kỳ, bảng, khoản đang mở
-- là nhóm 2, đã chốt hoặc hủy thì khóa (locked_at, trg_<bảng>_row_lock, trg_<bảng>_no_delete) nên là MULTIPLE;
-- cost_source_index là chỉ mục kỹ thuật (data-model 010 mục 0.2). Tách file vì V010_01 đã chạy trên schema test.
ALTER TABLE cost_statement COMMENT = 'DataGroup=MULTIPLE';
ALTER TABLE cost_item COMMENT = 'DataGroup=MULTIPLE';
ALTER TABLE cost_source_index COMMENT = 'DataGroup=TECHNICAL';

-- Khai nhóm cho nền 005 khi ghép luật kiểm mới; chưa thực hiện gộp bảng R-15/T064.
-- Theo data-model 005 mục 1: khóa theo người nhóm 2; mốc chạy kỹ thuật; ba danh mục nhóm 1.
ALTER TABLE care_resident_lock COMMENT = 'DataGroup=BUSINESS';
ALTER TABLE m04_job_milestone COMMENT = 'DataGroup=TECHNICAL';
ALTER TABLE care_task_type COMMENT = 'DataGroup=CATALOG';
ALTER TABLE care_task_type_certificate COMMENT = 'DataGroup=CATALOG';
ALTER TABLE care_result_value COMMENT = 'DataGroup=CATALOG';

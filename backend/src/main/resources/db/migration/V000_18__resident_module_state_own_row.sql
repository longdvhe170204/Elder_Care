-- Dòng riêng theo mã loại ở resident_module_state (2026-11-10, module-interfaces 000 mục 1.8, ResidentModuleStateApi
-- getOwnRow/saveOwnRow): cột module nhận cả mã module (M04) lẫn mã loại đầy đủ (M04.HOAT_DONG_NCT, tối đa 40 ký tự như
-- SharedKind.MAX_CODE_LENGTH). Dòng cũ giữ nguyên. Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).
ALTER TABLE resident_module_state MODIFY module VARCHAR(40) NOT NULL;

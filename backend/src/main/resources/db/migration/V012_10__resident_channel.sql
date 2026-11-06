-- 012 T065: phản hồi của người cao tuổi (data-model mục 9; FR-058, FR-058a, FR-058b, FR-066, FR-086; research F-13).
-- Người gửi là người thân hoặc chính người cao tuổi; nhân viên ghi thay khi người cao tuổi nói trực tiếp; loại Mong
-- muốn có chủ đề; nguồn kênh người cao tuổi, ghi trực tiếp; cách đóng ghi thay. Theo R-15 mục đổi loại (DOI_LOAI) là
-- record_event loại M10.FEEDBACK_ITEM, phan_hoi_y_kien là record_event loại M10.FEEDBACK_OPINION (khóa duy nhất ở
-- unique_key), nên không có bảng mới. Cú pháp dùng được ở MySQL 8.0 và 8.4 (R-01).

ALTER TABLE feedback
    ADD COLUMN sender_type ENUM ('FAMILY_MEMBER', 'RESIDENT') NOT NULL DEFAULT 'FAMILY_MEMBER'
        COMMENT 'Người gửi: người thân hoặc chính người cao tuổi (FR-058a)' AFTER resident_id,
    MODIFY COLUMN family_member_id BIGINT NULL COMMENT 'Người thân gửi (FR-058); trống khi người gửi là người cao tuổi',
    ADD COLUMN recorded_on_behalf_by BIGINT NULL
        COMMENT 'Hành chính, Điều dưỡng ghi thay người cao tuổi nói trực tiếp (FR-058a)' AFTER family_member_id,
    MODIFY COLUMN type ENUM ('SUGGESTION', 'COMPLAINT', 'QUESTION', 'WISH') NOT NULL,
    ADD COLUMN wish_topic ENUM ('VISITORS_PICKERS', 'OTHER') NULL
        COMMENT 'Chủ đề của loại Mong muốn (FR-058b)' AFTER content_group,
    MODIFY COLUMN source ENUM ('PORTAL', 'COUNTER', 'RESIDENT_CHANNEL', 'RECORDED_DIRECTLY') NOT NULL
        COMMENT 'COUNTER: Hành chính ghi thay người thân; RECORDED_DIRECTLY: nhân viên ghi thay người cao tuổi',
    MODIFY COLUMN close_method ENUM ('SENDER', 'AUTO', 'RECORDED_ON_BEHALF') NULL,
    ADD CONSTRAINT ck_feedback_family_sender
        CHECK ((sender_type = 'FAMILY_MEMBER') = (family_member_id IS NOT NULL)),
    ADD CONSTRAINT ck_feedback_wish
        CHECK ((type = 'WISH') = (wish_topic IS NOT NULL) AND (type <> 'WISH' OR sender_type = 'RESIDENT')),
    ADD CONSTRAINT ck_feedback_recorded_directly
        CHECK ((source = 'RECORDED_DIRECTLY') = (recorded_on_behalf_by IS NOT NULL)),
    ADD CONSTRAINT ck_feedback_resident_source
        CHECK ((sender_type = 'RESIDENT') = (source IN ('RESIDENT_CHANNEL', 'RECORDED_DIRECTLY'))),
    ADD KEY ix_feedback_sender (resident_id, sender_type, submitted_at);

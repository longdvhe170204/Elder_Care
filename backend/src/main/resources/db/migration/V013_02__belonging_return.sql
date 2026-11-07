-- 013 T017: lần trả và lần chặn trả (data-model mục 5; FR-016 → FR-019, BR-M12-04, SC-004). Theo R-15 không tạo bảng
-- lan_tra, lan_chan_tra: lần trả là bản ghi belonging_handover kind RETURN (mỗi đồ một bản ghi, dùng chung người nhận,
-- thời điểm, chữ ký), lần chặn trả là kind RETURN_BLOCKED (mỗi đồ trong lệnh bị chặn một bản ghi, lý do ở block_reason).
-- Xác nhận người nhận khác ở simple_record (M12.RECIPIENT_CONFIRMATION), bằng chứng liên hệ ở record_event
-- (M12.CONTACT_EVIDENCE): không có bảng mới.
-- Bản ghi RETURN_BLOCKED không đổi đồ gửi (current_handover_id giữ nguyên); mã ứng dụng loại nó khỏi lịch sử tính lại.
-- Thêm cột vào bảng nhóm 3 thì tạo lại trigger no_update có thêm cột mới (so từng cột, chỉ cho đặt incident_id một lần).
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (R-01).

ALTER TABLE belonging_handover
    MODIFY COLUMN kind ENUM ('INTAKE', 'GIVE_FOR_USE', 'TAKE_BACK', 'TRANSFER_CUSTODY', 'RETURN', 'REPORT_LOST',
        'RECORD_DAMAGE', 'FOUND', 'DISPOSAL', 'RETURN_BLOCKED') NOT NULL,
    ADD COLUMN recipient_id_document_type   VARCHAR(50)   NULL COMMENT 'Giấy tờ người nhận xuất trình (FR-016)' AFTER recipient_name,
    ADD COLUMN recipient_id_document_number VARCHAR(100)  NULL AFTER recipient_id_document_type,
    ADD COLUMN recipient_basis              ENUM ('ENTITLED_RECIPIENT', 'RESIDENT_Q253_EXCEPTION', 'RECIPIENT_CONFIRMATION') NULL
        COMMENT 'Căn cứ nhận: có quyền nhận, người cao tuổi ngoại lệ Q-253, xác nhận người nhận khác' AFTER recipient_id_document_number,
    ADD COLUMN verification_result          ENUM ('MATCHED', 'NO_DOCUMENT_ON_FILE_CROSS_CHECKED') NULL
        COMMENT 'Hồ sơ người thân chưa có giấy tờ thì ghi giấy tờ xuất trình (FR-016)' AFTER recipient_basis,
    ADD COLUMN signature_file_id            BIGINT        NULL COMMENT 'Bản ký scan phiếu trả (FR-019)' AFTER verification_result,
    ADD COLUMN no_signature_reason          VARCHAR(1000) NULL AFTER signature_file_id,
    ADD COLUMN condition_opinion            VARCHAR(2000) NULL AFTER no_signature_reason,
    ADD COLUMN block_reason                 ENUM ('ID_DOCUMENT_MISMATCH', 'NOT_ENTITLED_TO_RECEIVE', 'INVALID_RECIPIENT_CONFIRMATION',
        'NURSING_STAFF_VALUABLE_ITEM') NULL COMMENT 'Chỉ bản ghi RETURN_BLOCKED' AFTER condition_opinion,
    ADD CONSTRAINT ck_belonging_handover_block CHECK ((kind = 'RETURN_BLOCKED') = (block_reason IS NOT NULL));

DROP TRIGGER trg_belonging_handover_no_update;

DELIMITER $$

CREATE TRIGGER trg_belonging_handover_no_update
    BEFORE UPDATE
    ON belonging_handover
    FOR EACH ROW
BEGIN
    IF OLD.incident_id IS NOT NULL OR NEW.incident_id IS NULL
        OR NOT (OLD.belonging_id <=> NEW.belonging_id) OR NOT (OLD.resident_id <=> NEW.resident_id)
        OR NOT (OLD.kind <=> NEW.kind) OR NOT (OLD.target_status <=> NEW.target_status)
        OR NOT (OLD.giver_type <=> NEW.giver_type) OR NOT (OLD.giver_id <=> NEW.giver_id)
        OR NOT (OLD.giver_name <=> NEW.giver_name) OR NOT (OLD.giver_id_document <=> NEW.giver_id_document)
        OR NOT (OLD.recipient_type <=> NEW.recipient_type) OR NOT (OLD.recipient_id <=> NEW.recipient_id)
        OR NOT (OLD.recipient_name <=> NEW.recipient_name)
        OR NOT (OLD.recipient_id_document_type <=> NEW.recipient_id_document_type)
        OR NOT (OLD.recipient_id_document_number <=> NEW.recipient_id_document_number)
        OR NOT (OLD.recipient_basis <=> NEW.recipient_basis)
        OR NOT (OLD.verification_result <=> NEW.verification_result)
        OR NOT (OLD.signature_file_id <=> NEW.signature_file_id)
        OR NOT (OLD.no_signature_reason <=> NEW.no_signature_reason)
        OR NOT (OLD.condition_opinion <=> NEW.condition_opinion)
        OR NOT (OLD.block_reason <=> NEW.block_reason)
        OR NOT (OLD.occurred_at <=> NEW.occurred_at)
        OR NOT (OLD.quantity <=> NEW.quantity) OR NOT (OLD.cash_amount <=> NEW.cash_amount)
        OR NOT (OLD.condition_level_id <=> NEW.condition_level_id)
        OR NOT (OLD.condition_description <=> NEW.condition_description)
        OR NOT (OLD.storage_location_id <=> NEW.storage_location_id) OR NOT (OLD.reason <=> NEW.reason)
        OR NOT (OLD.holder_absent <=> NEW.holder_absent) OR NOT (OLD.valuable <=> NEW.valuable)
        OR NOT (OLD.photo_file_ids <=> NEW.photo_file_ids)
        OR NOT (OLD.recipient_confirmation_id <=> NEW.recipient_confirmation_id)
        OR NOT (OLD.approval_request_id <=> NEW.approval_request_id)
        OR NOT (OLD.receiving_unit <=> NEW.receiving_unit)
        OR NOT (OLD.inventory_check_id <=> NEW.inventory_check_id) OR NOT (OLD.note <=> NEW.note)
        OR NOT (OLD.recorded_by <=> NEW.recorded_by) OR NOT (OLD.recorded_at <=> NEW.recorded_at) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'belonging_handover is append-only; only incident_id may be set once (BR-M12-01)';
    END IF;
END$$

DELIMITER ;

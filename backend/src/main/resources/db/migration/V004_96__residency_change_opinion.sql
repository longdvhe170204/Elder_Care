-- 004 Phase 14, Bước 5 (FR-043h, FR-043i; Q-417, Q-418, chủ dự án chốt 2026-11-20).
-- residency_change_resident_opinion: ý kiến của chính người cao tuổi về một yêu cầu thay đổi lưu trú, chỉ thêm (nhóm 3);
--   bản mới nhất là hiện hành; content_digest là dấu nội dung thực chất của yêu cầu lúc ghi (R18).
-- residency_change_portal_digest: dấu nội dung thực chất lúc người đại diện gửi qua cổng, để biết Hành chính có đổi phần
--   thực chất không (R12); một dòng mỗi yêu cầu, ghi một lần.
-- Đánh số 96 (plan-bf08.md ghi V004_07/09): sau V004_94 đã chạy, số nhỏ hơn phải bật out-of-order (team-plan mục 8).
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

CREATE TABLE residency_change_resident_opinion
(
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    approval_request_id BIGINT        NOT NULL,
    resident_id         BIGINT        NOT NULL,
    opinion             ENUM ('AGREE', 'DISAGREE', 'UNABLE_TO_EXPRESS') NOT NULL,
    source              ENUM ('RESIDENT', 'STAFF_OBSERVATION') NOT NULL,
    note                VARCHAR(1000) NULL,
    content_digest      CHAR(64)      NOT NULL,
    recorded_by         BIGINT        NOT NULL,
    recorded_at         DATETIME(6)   NOT NULL,
    KEY ix_rcro_request (approval_request_id, recorded_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=RECORD';

CREATE TABLE residency_change_portal_digest
(
    approval_request_id BIGINT      NOT NULL PRIMARY KEY,
    digest              CHAR(64)    NOT NULL,
    created_at          DATETIME(6) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=TECHNICAL';

DELIMITER $$

CREATE TRIGGER trg_residency_change_resident_opinion_no_update
    BEFORE UPDATE
    ON residency_change_resident_opinion
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'resident opinions are append-only (constitution XII)';
END$$

CREATE TRIGGER trg_residency_change_resident_opinion_no_delete
    BEFORE DELETE
    ON residency_change_resident_opinion
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'resident opinions are removed only by the retention job (DBR-23)';
    END IF;
END$$

DELIMITER ;

GRANT INSERT ON residency_change_resident_opinion TO 'eldercare_app'@'%';
GRANT INSERT ON residency_change_portal_digest TO 'eldercare_app'@'%';
GRANT DELETE ON residency_change_resident_opinion TO 'eldercare_retention'@'%';
GRANT DELETE ON residency_change_portal_digest TO 'eldercare_retention'@'%';

-- 000 T101: sinh nhật ký ở quy mô NFR-01 để đo SC-006 (quickstart mục 5; research R-06).
-- 300 người cao tuổi, 100 nhân viên, khoảng 200 dòng mỗi người mỗi ngày: mặc định 365 ngày × 60 000 dòng ≈ 22 triệu.
-- Chỉ chạy trên schema demo hoặc schema đo riêng, không chạy trên dữ liệu thật: audit_log chỉ ghi thêm, dòng sinh ra
-- không xóa được bằng tài khoản ứng dụng. Mọi dòng có details.generated = true.
--
-- Cách chạy (tài khoản có INSERT trên audit_log, CREATE ROUTINE trên schema):
--   mysql -u <tài khoản> -p <schema> < generate-audit-log.sql
--   mysql -u <tài khoản> -p <schema> -e "CALL generate_audit_log(365, 60000)"

DROP PROCEDURE IF EXISTS generate_audit_log;

DELIMITER //
CREATE PROCEDURE generate_audit_log(IN p_days INT, IN p_rows_per_day INT)
BEGIN
    DECLARE v_day INT DEFAULT 0;
    DECLARE v_start DATETIME(6);
    DECLARE v_batch INT;
    DECLARE v_done INT;

    -- Bảng số 0 → 9 999 để sinh theo lô
    DROP TEMPORARY TABLE IF EXISTS gen_numbers;
    CREATE TEMPORARY TABLE gen_numbers (n INT NOT NULL PRIMARY KEY);
    INSERT INTO gen_numbers (n)
    SELECT a.d + 10 * b.d + 100 * c.d + 1000 * e.d
    FROM (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
         CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
         CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
         CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) e;

    -- Ngày đầu: p_days ngày trước hôm nay (UTC)
    SET v_start = TIMESTAMP(DATE_SUB(UTC_DATE(), INTERVAL p_days DAY));

    WHILE v_day < p_days DO
        SET v_done = 0;
        WHILE v_done < p_rows_per_day DO
            SET v_batch = LEAST(10000, p_rows_per_day - v_done);
            INSERT INTO audit_log (occurred_at, actor_type, actor_id, role, action, entity_type, entity_id,
                                   resident_id, data_before, data_after, reason, details)
            SELECT
                -- rải đều trong ngày
                v_start + INTERVAL v_day DAY + INTERVAL FLOOR((v_done + g.n) * 86400000000 / p_rows_per_day) MICROSECOND,
                'USER',
                1000 + ((v_done + g.n) * 7 + v_day) % 100,
                ELT(1 + (v_done + g.n) % 4, 'AC-04', 'AC-05', 'AC-04', 'AC-03'),
                ELT(1 + (v_done + g.n) % 5, 'TASK_COMPLETED', 'DOSE_CONFIRMED', 'VITAL_RECORDED', 'TASK_COMPLETED',
                    'CORRECTION_ADDED'),
                ELT(1 + (v_done + g.n) % 5, 'M04.CARE_TASK', 'M07.MEDICATION_DOSE', 'M06.VITAL_SIGN', 'M04.CARE_TASK',
                    'M07.MEDICATION_DOSE'),
                CAST(v_day * p_rows_per_day + v_done + g.n + 1 AS CHAR),
                1 + (v_done + g.n) % 300,
                NULL,
                JSON_OBJECT('result', 'DONE'),
                IF((v_done + g.n) % 5 = 4, 'Ghi nhầm khi ghi nhận', NULL),
                JSON_OBJECT('generated', TRUE)
            FROM gen_numbers g
            WHERE g.n < v_batch;
            SET v_done = v_done + v_batch;
        END WHILE;
        SET v_day = v_day + 1;
    END WHILE;

    DROP TEMPORARY TABLE gen_numbers;
END //
DELIMITER ;

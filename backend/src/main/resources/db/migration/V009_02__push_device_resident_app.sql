-- 009 T033: POST /push-devices nhận cả app của người cao tuổi (contracts/api-009.yaml, Q-365; app chung có phần người
-- cao tuổi).
ALTER TABLE push_device
    MODIFY app ENUM ('STAFF_APP', 'FAMILY_APP', 'RESIDENT_APP') NOT NULL;

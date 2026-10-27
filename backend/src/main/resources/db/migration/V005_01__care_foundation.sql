-- T005/C-01/C-15: chỉ nền khóa, mốc kỹ thuật và danh mục nhóm 1; không tạo công việc.
CREATE TABLE care_resident_lock (
 resident_id BIGINT NOT NULL PRIMARY KEY,
 version INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE m04_job_milestone (
 job VARCHAR(100) NOT NULL,
 milestone VARCHAR(100) NOT NULL,
 PRIMARY KEY(job,milestone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE care_task_type (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(200) NOT NULL,
 task_group ENUM('PERSONAL_HYGIENE','BATHING','DRESSING','EATING_ASSISTANCE','DRINKING_ASSISTANCE','TOILETING_ASSISTANCE','MOVEMENT','REHABILITATION','VITAL_SIGN_MEASUREMENT','ACTIVITY','SLEEP','MOOD','BEHAVIOR','DIAPER_ITEM_CHANGE','SHIFT_START_CHECK','SUPERVISION','MEDICATION','OTHER') NOT NULL,
 professional BOOLEAN NOT NULL,
 result_type ENUM('EATING','ML','MEASURED_VALUE','MOVEMENT','SLEEP','MOOD','BEHAVIOR','ITEM_QUANTITY','PERFORMED','NONE') NOT NULL,
 default_role VARCHAR(50) NOT NULL,
 default_importance ENUM('NORMAL','IMPORTANT','MANDATORY') NOT NULL,
 window_before_minutes SMALLINT NULL,
 window_after_minutes SMALLINT NULL,
 billable BOOLEAN NOT NULL,
 service_id BIGINT NULL,
 upper_limit INT NULL,
 vital_sign_type VARCHAR(100) NULL,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 version INT NOT NULL DEFAULT 0,
 active_name VARCHAR(200) GENERATED ALWAYS AS (IF(status='ACTIVE',name,NULL)) STORED,
 UNIQUE KEY uq_care_task_type_active_name(active_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE care_task_type_certificate (
 task_type_id BIGINT NOT NULL,
 certificate_code VARCHAR(100) NOT NULL,
 PRIMARY KEY(task_type_id,certificate_code),
 CONSTRAINT fk_care_task_certificate FOREIGN KEY(task_type_id) REFERENCES care_task_type(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE care_result_value (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 value_type ENUM('SLEEP_QUALITY','MOOD','BEHAVIOR') NOT NULL,
 code VARCHAR(100) NOT NULL,
 name VARCHAR(200) NOT NULL,
 rule_group ENUM('INSOMNIA','NEGATIVE') NULL,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 version INT NOT NULL DEFAULT 0,
 active_name VARCHAR(200) GENERATED ALWAYS AS (IF(status='ACTIVE',name,NULL)) STORED,
 UNIQUE KEY uq_care_result_active_name(active_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
GRANT INSERT,UPDATE,DELETE ON care_resident_lock TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON m04_job_milestone TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON care_task_type TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON care_task_type_certificate TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON care_result_value TO 'eldercare_app'@'%';

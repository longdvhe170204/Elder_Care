-- T005/T006/T007a, R-15: chỉ chuyển nền đã cài; không tạo bảng story và không sửa migration cũ.
-- Bảng đích đã khai DataGroup bởi 000. ID công khai cũ giữ trong attributes.legacy; adapter M04 ánh xạ khi đọc/ghi.
SET @care_catalog_base = (SELECT GREATEST(
    COALESCE((SELECT MAX(id) FROM catalog_item),0),
    COALESCE((SELECT MAX(id) FROM care_task_type),0),
    COALESCE((SELECT MAX(id) FROM care_result_value),0),
    COALESCE((SELECT MAX(id) FROM care_non_performance_reason_type),0)));

INSERT INTO catalog_item(id,catalog_type,module,name,status,version,attributes)
SELECT @care_catalog_base + ROW_NUMBER() OVER(ORDER BY t.id),'M04.TASK_TYPE','M04',t.name,t.status,t.version,
 JSON_OBJECT('group',t.task_group,'professional',IF(t.professional,CAST('true' AS JSON),CAST('false' AS JSON)),
 'resultType',t.result_type,'defaultRole',t.default_role,'defaultImportance',t.default_importance,
 'windowBeforeMinutes',t.window_before_minutes,'windowAfterMinutes',t.window_after_minutes,
 'billable',IF(t.billable,CAST('true' AS JSON),CAST('false' AS JSON)),'serviceId',t.service_id,
 'upperLimit',t.upper_limit,'vitalSignType',t.vital_sign_type,
 'certificates',COALESCE((SELECT JSON_ARRAYAGG(c.certificate_code) FROM care_task_type_certificate c WHERE c.task_type_id=t.id),JSON_ARRAY()),
 'legacy',JSON_OBJECT('table','care_task_type','id',t.id))
FROM care_task_type t;
SET @care_catalog_base = (SELECT GREATEST(@care_catalog_base,COALESCE(MAX(id),0)) FROM catalog_item);

INSERT INTO catalog_item(id,catalog_type,module,name,status,version,attributes)
SELECT @care_catalog_base + ROW_NUMBER() OVER(ORDER BY id),'M04.RESULT_VALUE','M04',name,status,version,
 JSON_OBJECT('type',value_type,'code',code,'ruleGroup',rule_group,'legacy',JSON_OBJECT('table','care_result_value','id',id))
FROM care_result_value;
SET @care_catalog_base = (SELECT GREATEST(@care_catalog_base,COALESCE(MAX(id),0)) FROM catalog_item);

INSERT INTO catalog_item(id,catalog_type,module,name,status,version,attributes)
SELECT @care_catalog_base + ROW_NUMBER() OVER(ORDER BY id),'M04.NON_PERFORMANCE_REASON_TYPE','M04',name,status,version,
 JSON_OBJECT('fixed',IF(fixed,CAST('true' AS JSON),CAST('false' AS JSON)),
 'legacy',JSON_OBJECT('table','care_non_performance_reason_type','id',id))
FROM care_non_performance_reason_type;

INSERT INTO resident_module_state(resident_id,module,data,version,updated_at)
SELECT resident_id,'M04',JSON_OBJECT('version',version),version,UTC_TIMESTAMP(6) FROM care_resident_lock;

INSERT INTO reminder_marker(kind,entity_type,entity_id,marker,marked_at,details)
SELECT 'M04.DAILY_WORK','M04.DAILY_WORK',0,SHA2(CONCAT(job,':',milestone),256),UTC_TIMESTAMP(6),
 JSON_OBJECT('job',job,'milestone',milestone,'legacy',JSON_OBJECT('table','m04_job_milestone'))
FROM m04_job_milestone;

DROP TABLE care_task_type_certificate;
DROP TABLE care_task_type;
DROP TABLE care_result_value;
DROP TABLE care_non_performance_reason_type;
DROP TABLE care_resident_lock;
DROP TABLE m04_job_milestone;

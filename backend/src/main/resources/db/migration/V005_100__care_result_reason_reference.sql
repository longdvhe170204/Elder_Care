-- FR-028a/R-15: khóa vật lý là catalog_item.id; public id cũ chỉ dùng tại API/audit.
ALTER TABLE care_task ADD CONSTRAINT fk_care_task_reason_type
 FOREIGN KEY(reason_type_id) REFERENCES catalog_item(id) ON DELETE RESTRICT;

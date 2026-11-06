-- 008 T050: chỉ mục riêng của E trên bảng chung, A review; không đổi API hoặc định nghĩa khác.
ALTER TABLE catalog_item
 ADD COLUMN m09_legacy_id BIGINT GENERATED ALWAYS AS
  (IF(module='M09',CAST(JSON_UNQUOTE(attributes->'$.legacy.id') AS UNSIGNED),NULL)) STORED,
 ADD UNIQUE KEY uq_catalog_m09_legacy(catalog_type,m09_legacy_id);
ALTER TABLE record_event
 ADD COLUMN m09_legacy_id BIGINT GENERATED ALWAYS AS
  (IF(module='M09',CAST(JSON_UNQUOTE(content->'$.legacy.id') AS UNSIGNED),NULL)) STORED,
 ADD UNIQUE KEY uq_event_m09_legacy(kind,m09_legacy_id);
ALTER TABLE simple_record
 ADD COLUMN m09_legacy_id BIGINT GENERATED ALWAYS AS
  (IF(module='M09',CAST(JSON_UNQUOTE(content->'$.legacy.id') AS UNSIGNED),NULL)) STORED,
 ADD UNIQUE KEY uq_simple_m09_legacy(kind,m09_legacy_id);

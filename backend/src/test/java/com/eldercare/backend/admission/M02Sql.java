package com.eldercare.backend.admission;

/** Câu lệnh dựng dữ liệu dùng chung cho test 004 trên bảng gộp (R-15): dịch vụ là dòng {@code catalog_item} loại {@code M02.SERVICE}. */
final class M02Sql {

    static final String DEACTIVATE_CARE_SERVICES = "UPDATE catalog_item SET status = 'INACTIVE'"
            + " WHERE catalog_type = 'M02.SERVICE' AND attributes ->> '$.type' = 'CARE'";
    static final String DEACTIVATE_SERVICE = "UPDATE catalog_item SET status = 'INACTIVE' WHERE id = ?";
    static final String SERVICE_ID =
            "SELECT id FROM catalog_item WHERE catalog_type = 'M02.SERVICE' AND name = ?" + " ORDER BY id DESC LIMIT 1";
    static final String SERVICE_STATUS = "SELECT status FROM catalog_item WHERE id = ?";
    static final String COUNT_SERVICES_NAMED =
            "SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M02.SERVICE' AND name = ?";
    static final String INSERT_SERVICE = "INSERT INTO catalog_item (catalog_type, module, name, status, attributes)"
            + " VALUES ('M02.SERVICE', 'M02', ?, 'ACTIVE', ?)";

    private M02Sql() {}

    /** Thuộc tính JSON của dịch vụ theo {@code ServiceAttributes}. */
    static String attributes(
            String type, String unit, String period, String careLevel, Long roomTypeId, Long medicationId) {
        return "{\"type\":\"%s\",\"unit\":\"%s\",\"billingPeriod\":\"%s\",\"careLevelCode\":%s,\"roomTypeId\":%s,\"medicationId\":%s}"
                .formatted(
                        type,
                        unit,
                        period,
                        careLevel == null ? "null" : "\"" + careLevel + "\"",
                        roomTypeId == null ? "null" : roomTypeId.toString(),
                        medicationId == null ? "null" : medicationId.toString());
    }

    static String attributes(String type, String unit, String period, String careLevel) {
        return attributes(type, unit, period, careLevel, null, null);
    }

    /** Trạng thái lưu trú của người cao tuổi trong {@code resident_module_state} (R-15). */
    static String residencyField(String field) {
        return "SELECT data ->> '$." + field + "' FROM resident_module_state WHERE resident_id = ? AND module = 'M02'";
    }
}

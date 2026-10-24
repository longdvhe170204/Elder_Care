package com.cambae.backend.m14.internal.metric.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Bộ lọc của báo cáo và xuất (FR-060; contracts tham số của {@code GET /reports/{group}}): trường rỗng là không lọc. Lọc
 * chỉ thu hẹp trong phạm vi của người xem (FR-013), không mở rộng.
 */
public record ReportFilters(
        Set<Long> floorAreaIds,
        String residenceType,
        Long careLevelId,
        Long residentId,
        Long staffId,
        Long shiftId,
        CostMode costMode) {

    public static final ReportFilters NONE = new ReportFilters(Set.of(), null, null, null, null, null, null);

    /** Chế độ chi phí (FR-051; contracts {@code CostMode}). */
    public enum CostMode {
        FINALIZED,
        PROVISIONAL
    }

    public ReportFilters {
        floorAreaIds = floorAreaIds == null ? Set.of() : Set.copyOf(floorAreaIds);
    }

    /** Bộ lọc chung: tầng/khu lúc phát sinh, người cao tuổi. */
    public boolean matches(MetricRecord r) {
        return (floorAreaIds.isEmpty() || (r.floorAreaId() != null && floorAreaIds.contains(r.floorAreaId())))
                && (residentId == null || residentId.equals(r.residentId()));
    }

    /** Bộ lọc đã đặt, theo tên tham số của hợp đồng, cho nhật ký và đầu file xuất (FR-061, FR-062). */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (!floorAreaIds.isEmpty()) {
            map.put("floorAreaId", floorAreaIds.stream().sorted().toList());
        }
        put(map, "residenceType", residenceType);
        put(map, "careLevelId", careLevelId);
        put(map, "residentId", residentId);
        put(map, "staffId", staffId);
        put(map, "shiftId", shiftId);
        put(map, "costMode", costMode);
        return map;
    }

    private static void put(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value instanceof Enum<?> e ? e.name() : value);
        }
    }
}

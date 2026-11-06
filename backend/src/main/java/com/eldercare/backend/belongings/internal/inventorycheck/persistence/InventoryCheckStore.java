package com.eldercare.backend.belongings.internal.inventorycheck.persistence;

import com.eldercare.backend.belongings.internal.inventorycheck.domain.HeldBelonging;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Truy vấn đồ có giá trị Đang giữ theo vị trí cho phiếu kiểm kê (FR-028a); chỉ đọc bảng {@code belonging}. */
@Repository
public class InventoryCheckStore {

    private final JdbcTemplate jdbc;

    InventoryCheckStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Đồ Đang giữ thuộc các loại {@code typeIds} (đồ có giá trị) tại vị trí, chưa bị loại, theo mã đồ. */
    public List<HeldBelonging> heldAt(long storageLocationId, Collection<Long> typeIds) {
        if (typeIds.isEmpty()) {
            return List.of();
        }
        Object[] args = new Object[typeIds.size() + 1];
        args[0] = storageLocationId;
        int i = 1;
        for (Long type : typeIds) {
            args[i++] = type;
        }
        return jdbc.query(
                "SELECT id, resident_id, COALESCE(quantity, cash_amount), condition_level_id FROM belonging"
                        + " WHERE status = 'HELD' AND excluded = FALSE AND storage_location_id = ?"
                        + " AND type_id IN (" + marks(typeIds.size()) + ") ORDER BY id",
                (rs, n) -> new HeldBelonging(rs.getLong(1), rs.getLong(2), rs.getBigDecimal(3), rs.getLong(4)),
                args);
    }

    /** Số đồ có giá trị Đang giữ theo vị trí, theo mã vị trí tăng (không xếp thứ tự ưu tiên, Q-306). */
    public Map<Long, Integer> countByLocation(Collection<Long> typeIds) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        if (typeIds.isEmpty()) {
            return result;
        }
        jdbc.query(
                "SELECT storage_location_id, COUNT(*) FROM belonging WHERE status = 'HELD' AND excluded = FALSE"
                        + " AND storage_location_id IS NOT NULL AND type_id IN (" + marks(typeIds.size()) + ")"
                        + " GROUP BY storage_location_id ORDER BY storage_location_id",
                rs -> {
                    result.put(rs.getLong(1), rs.getInt(2));
                },
                typeIds.toArray());
        return result;
    }

    private static String marks(int n) {
        return java.util.stream.IntStream.range(0, n).mapToObj(i -> "?").collect(Collectors.joining(", "));
    }
}

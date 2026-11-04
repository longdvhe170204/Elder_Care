package com.eldercare.backend.belongings.internal.belonging.persistence;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.HolderType;
import com.eldercare.backend.belongings.internal.belonging.domain.LockedBelonging;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc ghi bảng {@code belonging} (data-model mục 3). */
@Repository
public class BelongingStore {

    private final JdbcTemplate jdbc;

    BelongingStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Khóa các đồ của một người cao tuổi theo {@code id} tăng (K-03); đồ của người khác không có trong kết quả. */
    public List<LockedBelonging> lockForUpdate(long residentId, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] args = new Object[ids.size() + 1];
        args[0] = residentId;
        int i = 1;
        for (Long id : ids) {
            args[i++] = id;
        }
        return jdbc.query(
                "SELECT id, resident_id, type_id, status, holder_type, holder_id, storage_location_id,"
                        + " condition_level_id, quantity, cash_amount, version FROM belonging"
                        + " WHERE resident_id = ? AND excluded = FALSE AND id IN (" + placeholders + ")"
                        + " ORDER BY id FOR UPDATE",
                BelongingStore::locked,
                args);
    }

    /** Số đồ Đang giữ hoặc Hư hỏng tại vị trí (FR-002). */
    public int countHeldOrDamagedAt(long storageLocationId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM belonging WHERE storage_location_id = ? AND excluded = FALSE"
                        + " AND status IN ('HELD', 'DAMAGED')",
                Integer.class,
                storageLocationId);
        return count == null ? 0 : count;
    }

    /** Người cao tuổi chủ đồ. */
    public Optional<Long> residentOf(long belongingId) {
        return jdbc.queryForList("SELECT resident_id FROM belonging WHERE id = ?", Long.class, belongingId).stream()
                .findFirst();
    }

    /** Người cao tuổi của bản ghi bàn giao. */
    public Optional<Long> residentOfHandover(long handoverId) {
        return jdbc
                .queryForList("SELECT resident_id FROM belonging_handover WHERE id = ?", Long.class, handoverId)
                .stream()
                .findFirst();
    }

    private static LockedBelonging locked(ResultSet rs, int row) throws SQLException {
        String holderType = rs.getString("holder_type");
        return new LockedBelonging(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                rs.getLong("type_id"),
                BelongingStatus.valueOf(rs.getString("status")),
                holderType == null ? null : HolderType.valueOf(holderType),
                rs.getObject("holder_id", Long.class),
                rs.getObject("storage_location_id", Long.class),
                rs.getLong("condition_level_id"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("cash_amount"),
                rs.getInt("version"));
    }
}

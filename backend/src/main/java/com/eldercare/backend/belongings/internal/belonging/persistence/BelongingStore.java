package com.eldercare.backend.belongings.internal.belonging.persistence;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.api.dto.HolderType;
import com.eldercare.backend.belongings.internal.belonging.domain.LockedBelonging;
import com.eldercare.backend.belongings.internal.belonging.domain.NewBelonging;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Đọc ghi bảng {@code belonging} (data-model mục 3). */
@Repository
public class BelongingStore {

    private final JdbcTemplate jdbc;

    BelongingStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String VIEW_COLUMNS = "id, resident_id, type_id, description, quantity, cash_amount, unit,"
            + " status, holder_type, holder_id, storage_location_id, original_belonging_id, current_handover_at, version";

    public long insert(NewBelonging b) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps = connection.prepareStatement(
                            "INSERT INTO belonging (resident_id, type_id, description, quantity, cash_amount, unit,"
                                    + " status, holder_type, holder_id, storage_location_id, condition_level_id,"
                                    + " created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    int i = 1;
                    ps.setLong(i++, b.residentId());
                    ps.setLong(i++, b.typeId());
                    ps.setString(i++, b.description());
                    ps.setBigDecimal(i++, b.quantity());
                    ps.setBigDecimal(i++, b.cashAmount());
                    ps.setString(i++, b.unit());
                    ps.setString(i++, b.status().name());
                    ps.setString(
                            i++, b.holderType() == null ? null : b.holderType().name());
                    ps.setObject(i++, b.holderId());
                    ps.setObject(i++, b.storageLocationId());
                    ps.setLong(i++, b.conditionLevelId());
                    ps.setLong(i++, b.createdBy());
                    ps.setObject(i, BelongingHandoverStore.utc(b.createdAt()));
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Đặt bản ghi Tiếp nhận gốc (DBR-22) trong cùng lệnh tạo đồ. */
    public void setIntakeHandover(long belongingId, long handoverId) {
        jdbc.update("UPDATE belonging SET intake_handover_id = ? WHERE id = ?", handoverId, belongingId);
    }

    /** Cột suy ra theo bản ghi bàn giao hiện hành (research K-04, FR-011); tăng phiên bản (FR-011b). */
    public void applyHandover(
            long belongingId,
            BelongingStatus status,
            HolderType holderType,
            Long holderId,
            Long storageLocationId,
            long conditionLevelId,
            long handoverId,
            Instant handoverAt) {
        jdbc.update(
                "UPDATE belonging SET status = ?, holder_type = ?, holder_id = ?, storage_location_id = ?,"
                        + " condition_level_id = ?, current_handover_id = ?, current_handover_at = ?,"
                        + " version = version + 1 WHERE id = ?",
                status.name(),
                holderType == null ? null : holderType.name(),
                holderId,
                storageLocationId,
                conditionLevelId,
                handoverId,
                BelongingHandoverStore.utc(handoverAt),
                belongingId);
    }

    /** Đồ của người cao tuổi, không gồm đồ bị loại (FR-011a); {@code status} rỗng là mọi trạng thái. */
    public List<BelongingView> ofResident(long residentId, BelongingStatus status) {
        return status == null
                ? jdbc.query(
                        "SELECT " + VIEW_COLUMNS
                                + " FROM belonging WHERE resident_id = ? AND excluded = FALSE ORDER BY id",
                        BelongingStore::view,
                        residentId)
                : jdbc.query(
                        "SELECT " + VIEW_COLUMNS + " FROM belonging WHERE resident_id = ? AND status = ?"
                                + " AND excluded = FALSE ORDER BY id",
                        BelongingStore::view,
                        residentId,
                        status.name());
    }

    public Optional<BelongingView> find(long belongingId) {
        return jdbc
                .query("SELECT " + VIEW_COLUMNS + " FROM belonging WHERE id = ?", BelongingStore::view, belongingId)
                .stream()
                .findFirst();
    }

    public Optional<Long> intakeHandoverOf(long belongingId) {
        return jdbc
                .queryForList("SELECT intake_handover_id FROM belonging WHERE id = ?", Long.class, belongingId)
                .stream()
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    private static BelongingView view(ResultSet rs, int row) throws SQLException {
        String holderType = rs.getString("holder_type");
        return new BelongingView(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                rs.getLong("type_id"),
                rs.getString("description"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("cash_amount"),
                rs.getString("unit"),
                BelongingStatus.valueOf(rs.getString("status")),
                holderType == null ? null : HolderType.valueOf(holderType),
                rs.getObject("holder_id", Long.class),
                rs.getObject("storage_location_id", Long.class),
                rs.getObject("original_belonging_id", Long.class),
                BelongingHandoverStore.instant(rs, "current_handover_at"),
                rs.getInt("version"));
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

package com.eldercare.backend.belongings.internal.belonging.persistence;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.api.dto.HolderType;
import com.eldercare.backend.belongings.internal.belonging.domain.IntakeGroupMember;
import com.eldercare.backend.belongings.internal.belonging.domain.LockedBelonging;
import com.eldercare.backend.belongings.internal.belonging.domain.NewBelonging;
import java.math.BigDecimal;
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

    /**
     * Đồ tách khi bàn giao một phần (FR-012, FR-013): trỏ đồ nó được tách ra và bản ghi Tiếp nhận gốc của đồ đó (DBR-22).
     */
    public long insertSplit(NewBelonging b, long originalBelongingId, long intakeHandoverId) {
        long id = insert(b);
        jdbc.update(
                "UPDATE belonging SET original_belonging_id = ?, intake_handover_id = ? WHERE id = ?",
                originalBelongingId,
                intakeHandoverId,
                id);
        return id;
    }

    /** Số lượng (hoặc số tiền) còn lại của đồ sau khi tách, đính chính; tăng phiên bản (FR-011b). */
    public void setAmount(long belongingId, BigDecimal quantity, BigDecimal cashAmount) {
        jdbc.update(
                "UPDATE belonging SET quantity = ?, cash_amount = ?, version = version + 1 WHERE id = ?",
                quantity,
                cashAmount,
                belongingId);
    }

    /** Hủy ghi nhận bản ghi đầu tiên của đồ: đồ bị loại khỏi mọi danh sách, bản ghi giữ nguyên (FR-011a). */
    public void exclude(long belongingId) {
        jdbc.update("UPDATE belonging SET excluded = TRUE, version = version + 1 WHERE id = ?", belongingId);
    }

    /** Khóa mọi đồ của một lần tiếp nhận (gồm đồ tách, đồ bị loại) theo {@code id} tăng. */
    public List<IntakeGroupMember> lockIntakeGroup(long residentId, long intakeHandoverId) {
        return jdbc.query(
                "SELECT id, original_belonging_id, quantity, cash_amount, excluded, status, holder_type, holder_id,"
                        + " storage_location_id, condition_level_id, current_handover_id FROM belonging"
                        + " WHERE resident_id = ? AND intake_handover_id = ? ORDER BY id FOR UPDATE",
                (rs, row) -> {
                    String holderType = rs.getString("holder_type");
                    return new IntakeGroupMember(
                            rs.getLong("id"),
                            rs.getObject("original_belonging_id", Long.class),
                            rs.getBigDecimal("quantity"),
                            rs.getBigDecimal("cash_amount"),
                            rs.getBoolean("excluded"),
                            BelongingStatus.valueOf(rs.getString("status")),
                            holderType == null ? null : HolderType.valueOf(holderType),
                            rs.getObject("holder_id", Long.class),
                            rs.getObject("storage_location_id", Long.class),
                            rs.getLong("condition_level_id"),
                            rs.getObject("current_handover_id", Long.class));
                },
                residentId,
                intakeHandoverId);
    }

    /**
     * Đồ Đang giữ do nhân viên giữ thuộc các loại {@code typeIds} (đồ có giá trị, FR-014a); {@code staffIds} rỗng là mọi
     * nhân viên giữ.
     */
    public List<BelongingView> heldByStaff(Collection<Long> typeIds, Collection<Long> staffIds) {
        if (typeIds.isEmpty() || (staffIds != null && staffIds.isEmpty())) {
            return List.of();
        }
        List<Object> args = new java.util.ArrayList<>(typeIds);
        String sql = "SELECT " + VIEW_COLUMNS + " FROM belonging WHERE status = 'HELD' AND holder_type = 'STAFF'"
                + " AND excluded = FALSE AND type_id IN (" + marks(typeIds.size()) + ")";
        if (staffIds != null) {
            sql += " AND holder_id IN (" + marks(staffIds.size()) + ")";
            args.addAll(staffIds);
        }
        return jdbc.query(sql + " ORDER BY id", BelongingStore::view, args.toArray());
    }

    /** Đồ Đang giữ của người cao tuổi tại các vị trí (FR-028). */
    public List<BelongingView> heldAt(long residentId, Collection<Long> storageLocationIds) {
        if (storageLocationIds.isEmpty()) {
            return List.of();
        }
        List<Object> args = new java.util.ArrayList<>();
        args.add(residentId);
        args.addAll(storageLocationIds);
        return jdbc.query(
                "SELECT " + VIEW_COLUMNS + " FROM belonging WHERE resident_id = ? AND status = 'HELD'"
                        + " AND excluded = FALSE AND storage_location_id IN (" + marks(storageLocationIds.size())
                        + ") ORDER BY id",
                BelongingStore::view,
                args.toArray());
    }

    /** Đồ Đang được sử dụng của một loại (FR-015a: bật dấu "cần đồng ý"). */
    public List<BelongingView> inUseOfType(long typeId) {
        return jdbc.query(
                "SELECT " + VIEW_COLUMNS + " FROM belonging WHERE type_id = ? AND status = 'IN_USE'"
                        + " AND excluded = FALSE ORDER BY id",
                BelongingStore::view,
                typeId);
    }

    private static String marks(int n) {
        return String.join(", ", java.util.Collections.nCopies(n, "?"));
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

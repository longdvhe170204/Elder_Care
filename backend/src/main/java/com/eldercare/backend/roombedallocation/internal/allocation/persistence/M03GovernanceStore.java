package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationResident;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.TimedM03Record;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T010: chỉ truy vấn bảng M03; bảng của user story chưa triển khai được nhận diện rõ trước khi truy vấn. */
@Repository
public class M03GovernanceStore {
    /** Lịch sử cách ly phòng: dòng {@code status_history} của M03 (R-15, gộp 2026-11-13). */
    public static final String ROOM_ISOLATION = "status_history:M03.ROOM/ISOLATION";

    private static final String ROOM_ISOLATION_ROWS = "entity_type='M03.ROOM' AND attribute='ISOLATION'";

    private final JdbcTemplate jdbc;

    public M03GovernanceStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean exists(String table) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",
                        Integer.class,
                        table)
                > 0;
    }

    public Target scope(String type, long id) {
        String sql = switch (type) {
            case "M03.BED" ->
                "SELECT NULL resident_id,r.floor_id FROM bed b JOIN room r ON r.id=b.room_id WHERE b.id=?";
            case "M03.ROOM" -> "SELECT NULL resident_id,floor_id FROM room WHERE id=?";
            case "M03.BED_ALLOCATION" ->
                exists("bed_allocation")
                        ? "SELECT a.resident_id,r.floor_id FROM bed_allocation a JOIN bed b ON b.id=a.bed_id JOIN room r ON r.id=b.room_id WHERE a.id=?"
                        : null;
            default -> throw new IllegalArgumentException("Loại phạm vi không thuộc M03");
        };
        if (sql == null) throw missing(id);
        return jdbc
                .query(
                        sql,
                        (rs, i) -> new Target(type, Long.toString(id), rs.getObject(1, Long.class), rs.getLong(2)),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> missing(id));
    }

    private static InvalidPermissionDataException missing(long id) {
        return new InvalidPermissionDataException("M03.SCOPE", "MISSING_RECORD", id);
    }

    public Stream<AllocationResident> allocations() {
        if (!exists("bed_allocation")) return Stream.empty();
        return jdbc
                .query(
                        "SELECT id,resident_id FROM bed_allocation WHERE status IN ('CLOSED','CANCELLED') ORDER BY id",
                        (rs, i) -> new AllocationResident(rs.getLong(1), rs.getLong(2)))
                .stream();
    }

    public Optional<Long> allocationResident(long id) {
        if (!exists("bed_allocation")) return Optional.empty();
        return jdbc.query("SELECT resident_id FROM bed_allocation WHERE id=?", (rs, i) -> rs.getLong(1), id).stream()
                .findFirst();
    }

    public Stream<TimedM03Record> timed(String table, Instant cutoff) {
        if (table.equals(ROOM_ISOLATION))
            return jdbc
                    .query(
                            "SELECT id,starts_at FROM status_history WHERE " + ROOM_ISOLATION_ROWS
                                    + " AND starts_at<? ORDER BY starts_at,id",
                            (rs, i) -> new TimedM03Record(
                                    rs.getLong(1),
                                    rs.getObject(2, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                                    null),
                            LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC))
                    .stream();
        if (!table.equals("bed_state_history")) throw new IllegalArgumentException("Bảng lịch sử không thuộc M03");
        if (!exists(table)) return Stream.empty();
        String allocation =
                table.equals("bed_state_history") ? "IF(basis_type='BED_ALLOCATION',basis_id,NULL)" : "NULL";
        // Giữ căn cứ khôi phục trạng thái/lý do của giường còn bảo trì hoặc tạm ngừng.
        String activeStateHistory = table.equals("bed_state_history") ? """
                AND NOT (
                    (h.event='ASSET_MAINTENANCE'
                        AND EXISTS (SELECT 1 FROM bed b WHERE b.id=h.bed_id AND b.state='UNDER_MAINTENANCE')
                        AND h.id=(SELECT MAX(previous.id) FROM bed_state_history previous
                            WHERE previous.bed_id=h.bed_id AND previous.event='ASSET_MAINTENANCE'))
                    OR (h.event='SUSPEND'
                        AND EXISTS (SELECT 1 FROM bed b WHERE b.id=h.bed_id AND b.state IN ('SUSPENDED','UNDER_MAINTENANCE'))
                        AND h.id=(SELECT MAX(previous.id) FROM bed_state_history previous
                            WHERE previous.bed_id=h.bed_id AND previous.event='SUSPEND'))
                )
                """ : "";
        return jdbc
                .query(
                        "SELECT id,occurred_at," + allocation + " FROM " + table + " h WHERE occurred_at<? "
                                + activeStateHistory + " ORDER BY occurred_at,id",
                        (rs, i) -> new TimedM03Record(
                                rs.getLong(1),
                                rs.getObject(2, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                                rs.getObject(3, Long.class)),
                        LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC))
                .stream();
    }

    public Set<DataRef> allocationReferences(long id) {
        Set<DataRef> refs = new LinkedHashSet<>();
        if (exists("bed_allocation"))
            refs.addAll(jdbc.query(
                    "SELECT id FROM bed_allocation WHERE previous_allocation_id=?",
                    (rs, i) -> new DataRef("M03.BED_ALLOCATION", rs.getLong(1)),
                    id));
        refs.addAll(jdbc.query(
                "SELECT id FROM bed WHERE last_closed_allocation_id=?",
                (rs, i) -> new DataRef("M03.BED", rs.getLong(1)),
                id));
        refs.addAll(jdbc.query(
                "SELECT id FROM bed_state_history WHERE basis_type='BED_ALLOCATION' AND basis_id=?",
                (rs, i) -> new DataRef("M03.BED_STATE_HISTORY", rs.getLong(1)),
                id));
        return Set.copyOf(refs);
    }
    /** SQL xóa chỉ nhận bảng cố định, dùng kết nối retention do job truyền vào. */
    public void delete(JdbcTemplate retention, String table, List<Long> ids) {
        if (!Set.of("bed_allocation", "bed_state_history", ROOM_ISOLATION).contains(table))
            throw new IllegalArgumentException("Bảng không thuộc chính sách M03");
        if (ids.isEmpty()) return;
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        Object[] values = ids.toArray();
        if (table.equals(ROOM_ISOLATION)) {
            retention.update(
                    "DELETE FROM status_history WHERE " + ROOM_ISOLATION_ROWS + " AND id IN (" + placeholders + ")",
                    values);
            return;
        }
        if (!exists(table)) return;
        if (table.equals("bed_allocation")) {
            // Chỉ mục yêu cầu nhập bù của phân bổ ở record_event (R-15, gộp 2026-11-13).
            retention.update(
                    "DELETE FROM record_event WHERE entity_type='M03.BED_ALLOCATION'"
                            + " AND kind='M03.BED_BACKFILL_REQUEST' AND entity_id IN (" + placeholders + ")",
                    values);
            // T066: dấu nhắc của phân bổ trên reminder_marker (eldercare_retention có quyền DELETE, V000_10).
            retention.update(
                    "DELETE FROM reminder_marker WHERE kind LIKE 'M03.ALLOCATION\\_REMINDER.%'"
                            + " AND entity_type='BED_ALLOCATION' AND entity_id IN (" + placeholders + ")",
                    values);
        }
        retention.update("DELETE FROM " + table + " WHERE id IN (" + placeholders + ")", values);
    }
}

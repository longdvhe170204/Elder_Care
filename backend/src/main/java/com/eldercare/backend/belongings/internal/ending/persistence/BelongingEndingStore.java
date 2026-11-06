package com.eldercare.backend.belongings.internal.ending.persistence;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.internal.ending.domain.OpenBelonging;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Các cột theo dõi sau trạng thái cuối của bảng {@code belonging}: mốc tính xử lý khi Tìm thấy sau khi mục sau qua đời
 * đã Hoàn thành (FR-026a) và lần nhắc cuối (FR-027, CFG-M12-02).
 */
@Repository
public class BelongingEndingStore {

    private final JdbcTemplate jdbc;

    BelongingEndingStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Mốc tính xử lý của các đồ đã có mốc (FR-026a). */
    public Map<Long, Instant> clockStarts(Collection<Long> belongingIds) {
        Map<Long, Instant> result = new HashMap<>();
        if (belongingIds.isEmpty()) {
            return result;
        }
        String marks = belongingIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        jdbc.query(
                "SELECT id, disposal_clock_start_at FROM belonging WHERE disposal_clock_start_at IS NOT NULL"
                        + " AND id IN (" + marks + ")",
                rs -> {
                    result.put(rs.getLong(1), instant(rs.getObject(2, LocalDateTime.class)));
                },
                belongingIds.toArray());
        return result;
    }

    public void startDisposalClock(long belongingId, Instant at) {
        jdbc.update(
                "UPDATE belonging SET disposal_clock_start_at = ? WHERE id = ? AND disposal_clock_start_at IS NULL",
                LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                belongingId);
    }

    /** Đồ Đang giữ, Đang được sử dụng, Hư hỏng của các người cao tuổi (chưa bị loại), theo mã đồ. */
    public List<OpenBelonging> openOf(Collection<Long> residentIds) {
        if (residentIds.isEmpty()) {
            return List.of();
        }
        String marks = residentIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        return jdbc.query(
                "SELECT id, resident_id, status, disposal_clock_start_at, last_reminded_at FROM belonging"
                        + " WHERE excluded = FALSE AND status IN ('HELD', 'IN_USE', 'DAMAGED')"
                        + " AND resident_id IN (" + marks + ") ORDER BY id",
                (rs, i) -> new OpenBelonging(
                        rs.getLong(1),
                        rs.getLong(2),
                        BelongingStatus.valueOf(rs.getString(3)),
                        instant(rs.getObject(4, LocalDateTime.class)),
                        instant(rs.getObject(5, LocalDateTime.class))),
                residentIds.toArray());
    }

    public void markReminded(Collection<Long> belongingIds, Instant at) {
        if (belongingIds.isEmpty()) {
            return;
        }
        String marks = belongingIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        Object[] args = new Object[belongingIds.size() + 1];
        args[0] = LocalDateTime.ofInstant(at, ZoneOffset.UTC);
        int i = 1;
        for (Long id : belongingIds) {
            args[i++] = id;
        }
        jdbc.update("UPDATE belonging SET last_reminded_at = ? WHERE id IN (" + marks + ")", args);
    }

    private static Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}

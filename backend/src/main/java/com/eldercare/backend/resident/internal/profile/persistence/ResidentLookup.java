package com.eldercare.backend.resident.internal.profile.persistence;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Tra cứu hồ sơ bằng các cột sinh và chỉ mục của bảng {@code resident} (research H-04, H-05): CCCD đang mở
 * ({@code open_citizen_id}), CCCD của hồ sơ trạng thái cuối, trùng họ tên và ngày sinh ({@code full_name_match}),
 * tìm kiếm có phân trang theo con trỏ.
 */
@Repository
public class ResidentLookup {

    public static final int MAX_PAGE_SIZE = 100;
    private static final String FINAL_STATUSES = "'RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED'";

    /** Hồ sơ đang mang một CCCD hoặc một phần nhận dạng của hồ sơ trùng. */
    public record Match(long id, String profileCode, String fullName, LocalDate dateOfBirth, ResidentStatus status) {}

    private final JdbcTemplate jdbc;

    ResidentLookup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Hồ sơ chưa ở trạng thái cuối đang mang CCCD này (DBR-01). */
    public Optional<Match> findOpenByCitizenId(String citizenId) {
        return one("WHERE open_citizen_id = ?", citizenId);
    }

    /** Hồ sơ gần nhất ở trạng thái cuối mang CCCD này (FR-004). */
    public Optional<Match> findLatestClosedByCitizenId(String citizenId) {
        return one(
                "WHERE id_document_type = 'CITIZEN_ID' AND id_document_number = ? AND status IN ("
                        + FINAL_STATUSES
                        + ") ORDER BY id DESC LIMIT 1",
                citizenId);
    }

    /** Hồ sơ ở mọi trạng thái trùng họ tên (không phân biệt hoa thường, có phân biệt dấu) và ngày sinh (FR-005). */
    public List<Match> findSameNameAndBirthDate(String fullName, LocalDate dateOfBirth) {
        return jdbc.query(
                MATCH_SELECT
                        + " WHERE full_name_match = TRIM(REGEXP_REPLACE(?, '[[:space:]]+', ' ')) AND date_of_birth = ?"
                        + " ORDER BY id LIMIT 10",
                (rs, i) -> match(rs),
                fullName,
                dateOfBirth);
    }

    /**
     * Mã hồ sơ, mới nhất trước, trả tối đa {@code limit + 1} phần tử để biết còn trang sau. {@code scopedIds} là
     * {@code null} khi người xem có phạm vi toàn viện, rỗng khi không có hồ sơ nào trong phạm vi.
     */
    public List<Long> search(String q, Set<ResidentStatus> statuses, Set<Long> scopedIds, Long beforeId, int limit) {
        if (scopedIds != null && scopedIds.isEmpty()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder("SELECT id FROM resident WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String text = q.strip();
            sql.append(" AND (full_name LIKE ? OR profile_code = ? OR id_document_number = ?)");
            args.add("%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
            args.add(text);
            args.add(text);
        }
        if (!statuses.isEmpty()) {
            sql.append(" AND status IN (").append(placeholders(statuses.size())).append(")");
            statuses.forEach(s -> args.add(s.name()));
        }
        if (scopedIds != null) {
            sql.append(" AND id IN (").append(placeholders(scopedIds.size())).append(")");
            args.addAll(scopedIds);
        }
        if (beforeId != null) {
            sql.append(" AND id < ?");
            args.add(beforeId);
        }
        sql.append(" ORDER BY id DESC LIMIT ?");
        args.add(Math.min(Math.max(limit, 1), MAX_PAGE_SIZE) + 1);
        return jdbc.queryForList(sql.toString(), Long.class, args.toArray());
    }

    private static final String MATCH_SELECT =
            "SELECT id, profile_code, full_name, date_of_birth, status FROM resident";

    private Optional<Match> one(String where, Object... args) {
        return jdbc.query(MATCH_SELECT + " " + where, (rs, i) -> match(rs), args).stream()
                .findFirst();
    }

    private static Match match(ResultSet rs) throws SQLException {
        return new Match(
                rs.getLong("id"),
                rs.getString("profile_code"),
                rs.getString("full_name"),
                rs.getObject("date_of_birth", LocalDate.class),
                ResidentStatus.valueOf(rs.getString("status")));
    }

    private static String placeholders(int count) {
        return IntStream.range(0, count).mapToObj(i -> "?").collect(Collectors.joining(", "));
    }
}

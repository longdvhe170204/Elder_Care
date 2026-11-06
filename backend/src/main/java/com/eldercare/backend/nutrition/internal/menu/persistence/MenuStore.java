package com.eldercare.backend.nutrition.internal.menu.persistence;

import com.eldercare.backend.nutrition.internal.menu.domain.Menu;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuContent;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuStatus;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Bảng riêng {@code menu} của M08 (V011_04; data-model mục 5). Danh mục bữa, chế độ ăn, món đọc qua
 * {@code NutritionCatalogStore} (R-15); lệnh ghi chạy trong transaction của service.
 */
@Repository
public class MenuStore {

    private static final String SELECT = "SELECT id, week_start, status, content, processing_check_confirmed,"
            + " processing_note, processing_checked_by, processing_checked_at, confirmed_warnings, submitted_by,"
            + " submitter_staff_id, submitted_at, published_by, published_at, applied_at, return_reason, cancel_reason,"
            + " copied_from_id, last_swap_id, version FROM menu";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    MenuStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** {@code DuplicateKeyException} khi tuần đã có thực đơn chưa hủy ({@code uq_menu_active_week}). */
    public long insert(LocalDate weekStart, MenuContent content, Long copiedFromId, long createdBy, Instant at) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO menu (week_start, status, content, copied_from_id, created_by, created_at)"
                                    + " VALUES (?, 'DRAFT', CAST(? AS JSON), ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setObject(1, weekStart);
                    ps.setString(2, json.writeValueAsString(content));
                    ps.setObject(3, copiedFromId);
                    ps.setLong(4, createdBy);
                    ps.setObject(5, utc(at));
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    public Optional<Menu> find(long id) {
        return jdbc.query(SELECT + " WHERE id = ?", this::row, id).stream().findFirst();
    }

    /** Khóa dòng cho lệnh trạng thái (một lệnh mỗi thực đơn tại một thời điểm). */
    public Optional<Menu> lock(long id) {
        return jdbc.query(SELECT + " WHERE id = ? FOR UPDATE", this::row, id).stream()
                .findFirst();
    }

    /** Theo tuần; {@code null} là mọi tuần, mới nhất trước. */
    public List<Menu> list(LocalDate weekStart) {
        return weekStart == null
                ? jdbc.query(SELECT + " ORDER BY week_start DESC, id DESC", this::row)
                : jdbc.query(SELECT + " WHERE week_start = ? ORDER BY id DESC", this::row, weekStart);
    }

    /** Thực đơn chưa hủy của tuần (tối đa một). */
    public Optional<Menu> activeOfWeek(LocalDate weekStart) {
        return jdbc.query(SELECT + " WHERE active_week = ?", this::row, weekStart).stream()
                .findFirst();
    }

    /** Thực đơn chưa hủy có tuần chưa kết thúc tại {@code today} (FR-002, FR-016). */
    public List<Menu> notEnded(LocalDate today) {
        return jdbc.query(
                SELECT + " WHERE status <> 'CANCELLED' AND week_start > ? ORDER BY week_start",
                this::row,
                today.minusDays(7));
    }

    /** Nội dung đã Công bố hoặc Đã áp dụng giao khoảng ngày (FR-028, FR-063, FR-026). */
    public List<MenuDish> publishedDishes(LocalDate from, LocalDate to) {
        return jdbc
                .query(
                        "SELECT content FROM menu WHERE status IN ('PUBLISHED','APPLIED')"
                                + " AND week_start <= ? AND DATE_ADD(week_start, INTERVAL 6 DAY) >= ?"
                                + " ORDER BY week_start, id",
                        (rs, n) -> json.readValue(rs.getString(1), MenuContent.class),
                        to,
                        from)
                .stream()
                .flatMap(content -> content.dishes().stream())
                .filter(dish -> !dish.date().isBefore(from) && !dish.date().isAfter(to))
                .toList();
    }

    /** {@code false} khi phiên bản đã đổi (412 ở service). */
    public boolean updateContent(long id, int version, MenuContent content) {
        return jdbc.update(
                        "UPDATE menu SET content = CAST(? AS JSON), version = version + 1"
                                + " WHERE id = ? AND version = ? AND status = 'DRAFT'",
                        json.writeValueAsString(content),
                        id,
                        version)
                == 1;
    }

    public void submit(long id, String note, List<String> confirmedWarnings, long by, Long staffId, Instant at) {
        jdbc.update(
                "UPDATE menu SET status = 'PENDING_PUBLICATION', processing_check_confirmed = TRUE, processing_note = ?,"
                        + " processing_checked_by = ?, processing_checked_at = ?, confirmed_warnings = CAST(? AS JSON),"
                        + " submitted_by = ?, submitter_staff_id = ?, submitted_at = ?, return_reason = NULL,"
                        + " version = version + 1 WHERE id = ?",
                note,
                by,
                utc(at),
                json.writeValueAsString(confirmedWarnings),
                by,
                staffId,
                utc(at),
                id);
    }

    /** Rút lại ({@code returnReason} {@code null}) hoặc trả lại: về Nháp, xác nhận kiểm tra chế biến phải làm lại. */
    public void toDraft(long id, String returnReason) {
        jdbc.update(
                "UPDATE menu SET status = 'DRAFT', processing_check_confirmed = FALSE, return_reason = ?,"
                        + " version = version + 1 WHERE id = ?",
                returnReason,
                id);
    }

    public void cancel(long id, String reason) {
        jdbc.update(
                "UPDATE menu SET status = 'CANCELLED', cancel_reason = ?, version = version + 1 WHERE id = ?",
                reason,
                id);
    }

    public void publish(long id, MenuStatus to, List<String> confirmedWarnings, long by, Instant at) {
        jdbc.update(
                "UPDATE menu SET status = ?, confirmed_warnings = CAST(? AS JSON), published_by = ?, published_at = ?,"
                        + " applied_at = ?, version = version + 1 WHERE id = ?",
                to.name(),
                json.writeValueAsString(confirmedWarnings),
                by,
                utc(at),
                to == MenuStatus.APPLIED ? utc(at) : null,
                id);
    }

    /** Đổi món (FR-031): trigger chỉ cho đổi nội dung ngoài Nháp khi kèm id bản ghi đổi món mới. */
    public void swap(long id, MenuContent content, long swapId) {
        jdbc.update(
                "UPDATE menu SET content = CAST(? AS JSON), last_swap_id = ?, version = version + 1 WHERE id = ?",
                json.writeValueAsString(content),
                swapId,
                id);
    }

    /** Id thực đơn Công bố có tuần đã bắt đầu tại {@code today} (FR-030). */
    public List<Long> dueForApplied(LocalDate today) {
        return jdbc.queryForList(
                "SELECT id FROM menu WHERE status = 'PUBLISHED' AND week_start <= ? ORDER BY id", Long.class, today);
    }

    /** {@code false} khi thực đơn đã không còn Công bố (chạy trùng). */
    public boolean markApplied(long id, Instant at) {
        return jdbc.update(
                        "UPDATE menu SET status = 'APPLIED', applied_at = ?, version = version + 1"
                                + " WHERE id = ? AND status = 'PUBLISHED'",
                        utc(at),
                        id)
                == 1;
    }

    private Menu row(ResultSet rs, int n) throws SQLException {
        String warnings = rs.getString("confirmed_warnings");
        return new Menu(
                rs.getLong("id"),
                rs.getObject("week_start", LocalDate.class),
                MenuStatus.valueOf(rs.getString("status")),
                json.readValue(rs.getString("content"), MenuContent.class),
                rs.getBoolean("processing_check_confirmed"),
                rs.getString("processing_note"),
                rs.getObject("processing_checked_by", Long.class),
                instant(rs, "processing_checked_at"),
                warnings == null ? List.of() : json.readValue(warnings, new TypeReference<List<String>>() {}),
                rs.getObject("submitted_by", Long.class),
                rs.getObject("submitter_staff_id", Long.class),
                instant(rs, "submitted_at"),
                rs.getObject("published_by", Long.class),
                instant(rs, "published_at"),
                instant(rs, "applied_at"),
                rs.getString("return_reason"),
                rs.getString("cancel_reason"),
                rs.getObject("copied_from_id", Long.class),
                rs.getObject("last_swap_id", Long.class),
                rs.getInt("version"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}

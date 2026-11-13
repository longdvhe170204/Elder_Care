package com.eldercare.backend.nutrition.internal.closing.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosing;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosingContent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lần chốt suất (nhóm 3, chỉ thêm): dòng {@code record_event} loại {@code M08.PORTION_CLOSING} của bữa ngày {@code
 * M08.DAILY_MEAL}, đã xác nhận ngay khi ghi; {@code actor_id} là người chốt, {@code occurred_at} là thời điểm chốt, {@code
 * unique_key} theo bữa ngày giữ "mỗi bữa chốt một lần" (FR-033); phần còn lại ở {@code content} (gộp bảng {@code
 * portion_closing} 2026-11-13, research R-15 của 000). Ghi qua {@link RecordEventApi}; đọc bằng SQL dòng loại của 011.
 */
@Repository
public class PortionClosingStore {

    private static final String DAILY_MEAL = "M08.DAILY_MEAL";
    private static final String MEAL_TIME =
            "CAST(REPLACE(REPLACE(content->>'$.mealTime', 'T', ' '), 'Z', '') AS DATETIME(6))";
    private static final String SELECT = "SELECT id, entity_id, actor_id, occurred_at, content FROM record_event"
            + " WHERE entity_type = '" + DAILY_MEAL + "' AND kind = 'M08.PORTION_CLOSING'";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final RecordEventApi events;

    PortionClosingStore(JdbcTemplate jdbc, JsonMapper json, RecordEventApi events) {
        this.jdbc = jdbc;
        this.json = json;
        this.events = events;
    }

    /** {@code DuplicateKeyException} khi bữa đã chốt (FR-033). */
    public long insert(PortionClosing c) {
        if (ofDailyMeal(c.dailyMealId()).isPresent()) {
            throw new DuplicateKeyException("Bữa ngày đã chốt: " + c.dailyMealId());
        }
        return events.add(
                        PortionClosingContent.KIND,
                        new EntityRef(DAILY_MEAL, c.dailyMealId()),
                        new NewRecordEvent<>(
                                new PortionClosingContent(
                                        c.mealId(),
                                        c.mealDate(),
                                        c.mealTime(),
                                        c.lateThresholdMinutes(),
                                        c.closedAfterMealTime(),
                                        c.menuId(),
                                        c.totalPortions(),
                                        c.persons(),
                                        c.portions(),
                                        c.flags(),
                                        c.missingData()),
                                null,
                                c.closedBy(),
                                false,
                                c.closedAt(),
                                null,
                                c.closedAt(),
                                null,
                                "M08.PORTION_CLOSING:" + c.dailyMealId()))
                .id();
    }

    public Optional<PortionClosing> ofDailyMeal(long dailyMealId) {
        return jdbc.query(SELECT + " AND entity_id = ?", this::row, dailyMealId).stream()
                .findFirst();
    }

    /** Lần chốt của các bữa ngày trong ngày. */
    public List<PortionClosing> onDate(LocalDate date) {
        return jdbc.query(SELECT + " AND content->>'$.mealDate' = ? ORDER BY " + MEAL_TIME, this::row, date.toString());
    }

    /** Lần chốt của các bữa có ngày trong {@code [from, to]} (số liệu cho báo cáo 016). */
    public List<PortionClosing> mealDateBetween(LocalDate from, LocalDate to) {
        return jdbc.query(
                SELECT + " AND content->>'$.mealDate' BETWEEN ? AND ? ORDER BY " + MEAL_TIME,
                this::row,
                from.toString(),
                to.toString());
    }

    /** Lần chốt có giờ bữa sau {@code at}: bữa đã chốt chưa tới giờ (FR-039). */
    public List<PortionClosing> beforeMealTime(Instant at) {
        return jdbc.query(SELECT + " AND " + MEAL_TIME + " > ? ORDER BY " + MEAL_TIME, this::row, utc(at));
    }

    /** Lần chốt có giờ bữa trong {@code [from, to)} (việc theo phút). */
    public List<PortionClosing> mealTimeBetween(Instant from, Instant to) {
        return jdbc.query(
                SELECT + " AND " + MEAL_TIME + " >= ? AND " + MEAL_TIME + " < ? ORDER BY " + MEAL_TIME,
                this::row,
                utc(from),
                utc(to));
    }

    private PortionClosing row(ResultSet rs, int n) throws SQLException {
        PortionClosingContent c = json.readValue(rs.getString("content"), PortionClosingContent.class);
        return new PortionClosing(
                rs.getLong("id"),
                rs.getLong("entity_id"),
                c.mealId(),
                c.mealDate(),
                c.mealTime(),
                c.lateThresholdMinutes(),
                rs.getLong("actor_id"),
                rs.getObject("occurred_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                c.closedAfterMealTime(),
                c.menuId(),
                c.totalPortions(),
                c.persons(),
                c.portions(),
                c.flags(),
                c.missingData());
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}

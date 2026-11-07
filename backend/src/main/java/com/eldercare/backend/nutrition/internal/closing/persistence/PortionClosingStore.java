package com.eldercare.backend.nutrition.internal.closing.persistence;

import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosing;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosing.FloorDietCount;
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
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Bảng riêng {@code portion_closing} (V011_05; nhóm 3, chỉ thêm). */
@Repository
public class PortionClosingStore {

    private static final String SELECT =
            "SELECT id, daily_meal_id, meal_id, meal_date, meal_time, late_threshold_minutes,"
                    + " closed_by, closed_at, closed_after_meal_time, menu_id, total_portions, counted_residents,"
                    + " portions_by_diet, flags, missing_data FROM portion_closing";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    PortionClosingStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** {@code DuplicateKeyException} khi bữa đã chốt (FR-033). */
    public long insert(PortionClosing c) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO portion_closing (daily_meal_id, meal_id, meal_date, meal_time,"
                                    + " late_threshold_minutes, closed_by, closed_at, closed_after_meal_time, menu_id,"
                                    + " total_portions, counted_residents, portions_by_diet, flags, missing_data)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON),"
                                    + " CAST(? AS JSON), CAST(? AS JSON))",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, c.dailyMealId());
                    ps.setLong(2, c.mealId());
                    ps.setObject(3, c.mealDate());
                    ps.setObject(4, utc(c.mealTime()));
                    ps.setInt(5, c.lateThresholdMinutes());
                    ps.setLong(6, c.closedBy());
                    ps.setObject(7, utc(c.closedAt()));
                    ps.setBoolean(8, c.closedAfterMealTime());
                    ps.setObject(9, c.menuId());
                    ps.setInt(10, c.totalPortions());
                    ps.setString(11, json.writeValueAsString(c.persons()));
                    ps.setString(12, json.writeValueAsString(c.portions()));
                    ps.setString(13, json.writeValueAsString(c.flags()));
                    ps.setString(14, json.writeValueAsString(c.missingData()));
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    public Optional<PortionClosing> ofDailyMeal(long dailyMealId) {
        return jdbc.query(SELECT + " WHERE daily_meal_id = ?", this::row, dailyMealId).stream()
                .findFirst();
    }

    /** Lần chốt của các bữa ngày trong ngày. */
    public List<PortionClosing> onDate(LocalDate date) {
        return jdbc.query(SELECT + " WHERE meal_date = ? ORDER BY meal_time", this::row, date);
    }

    /** Lần chốt của các bữa có ngày trong {@code [from, to]} (số liệu cho báo cáo 016). */
    public List<PortionClosing> mealDateBetween(LocalDate from, LocalDate to) {
        return jdbc.query(SELECT + " WHERE meal_date BETWEEN ? AND ? ORDER BY meal_time", this::row, from, to);
    }

    /** Lần chốt có giờ bữa sau {@code at}: bữa đã chốt chưa tới giờ (FR-039). */
    public List<PortionClosing> beforeMealTime(Instant at) {
        return jdbc.query(SELECT + " WHERE meal_time > ? ORDER BY meal_time", this::row, utc(at));
    }

    /** Lần chốt có giờ bữa trong {@code [from, to)} (việc theo phút). */
    public List<PortionClosing> mealTimeBetween(Instant from, Instant to) {
        return jdbc.query(
                SELECT + " WHERE meal_time >= ? AND meal_time < ? ORDER BY meal_time", this::row, utc(from), utc(to));
    }

    private PortionClosing row(ResultSet rs, int n) throws SQLException {
        return new PortionClosing(
                rs.getLong("id"),
                rs.getLong("daily_meal_id"),
                rs.getLong("meal_id"),
                rs.getObject("meal_date", LocalDate.class),
                instant(rs, "meal_time"),
                rs.getInt("late_threshold_minutes"),
                rs.getLong("closed_by"),
                instant(rs, "closed_at"),
                rs.getBoolean("closed_after_meal_time"),
                rs.getObject("menu_id", Long.class),
                rs.getInt("total_portions"),
                json.readValue(rs.getString("counted_residents"), new TypeReference<List<CountedPerson>>() {}),
                json.readValue(rs.getString("portions_by_diet"), new TypeReference<List<FloorDietCount>>() {}),
                json.readValue(rs.getString("flags"), new TypeReference<Set<String>>() {}),
                json.readValue(rs.getString("missing_data"), new TypeReference<Set<String>>() {}));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}

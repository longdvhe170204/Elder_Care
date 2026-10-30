package com.cambae.backend.m09.staff.internal.persistence;

import com.cambae.backend.m09.staff.internal.domain.StaffInput;
import com.cambae.backend.m09.staff.internal.domain.StaffProfile;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Chỉ đọc/ghi bảng M09; dịch vụ chịu trách nhiệm kiểm quyền và ghi nhật ký. */
@Repository
public class StaffStore {
    private final JdbcTemplate jdbc;

    public StaffStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT =
            "SELECT s.*, (SELECT d.department_id FROM staff_department d WHERE d.staff_id=s.id AND d.ends_at IS NULL) department_id FROM staff_profile s";

    public Optional<StaffProfile> find(long id) {
        return jdbc.query(SELECT + " WHERE s.id=?", this::map, id).stream().findFirst();
    }

    public StaffProfile lock(long id) {
        return jdbc.query(SELECT + " WHERE s.id=? FOR UPDATE", this::map, id).stream()
                .findFirst()
                .orElseThrow(com.cambae.backend.common.error.NotFoundException::new);
    }

    public List<StaffProfile> all() {
        return jdbc.query(SELECT + " ORDER BY s.id", this::map);
    }

    public long insert(StaffInput input, Long actor, Instant at) {
        var key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(
                            "INSERT INTO staff_profile(full_name,position,specialty,start_date,phone_number,normalized_phone,email,created_at,created_by) VALUES(?,?,?,?,?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, input.fullName().strip());
                    ps.setString(2, input.position().strip());
                    ps.setString(3, input.specialty());
                    ps.setObject(4, input.startDate());
                    ps.setString(5, input.phoneNumber());
                    ps.setString(6, normalize(input.phoneNumber()));
                    ps.setString(7, input.email());
                    ps.setObject(8, LocalDateTime.ofInstant(at, ZoneOffset.UTC));
                    ps.setObject(9, actor);
                    return ps;
                },
                key);
        long id = key.getKey().longValue();
        if (input.departmentId() != null)
            jdbc.update(
                    "INSERT INTO staff_department(staff_id,department_id,starts_at,actor_id,reason) VALUES (?,?,?,?,?)",
                    id,
                    input.departmentId(),
                    LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                    actor,
                    "Lập hồ sơ nhân viên");
        return id;
    }

    public void update(long id, StaffInput input) {
        jdbc.update(
                "UPDATE staff_profile SET full_name=?,position=?,specialty=?,start_date=?,phone_number=?,normalized_phone=?,email=?,version=version+1 WHERE id=?",
                input.fullName().strip(),
                input.position().strip(),
                input.specialty(),
                input.startDate(),
                input.phoneNumber(),
                normalize(input.phoneNumber()),
                input.email(),
                id);
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String digits = value.replaceAll("[\\s.\\-]", "");
        // Khóa tra cứu chỉ chứa chữ số; giữ nguyên liên hệ gốc khi không chuẩn hoá được.
        return digits.matches("[0-9]{1,15}") ? digits : null;
    }

    private StaffProfile map(ResultSet r, int row) throws SQLException {
        return new StaffProfile(
                r.getLong("id"),
                r.getString("full_name"),
                r.getString("position"),
                r.getString("specialty"),
                r.getObject("start_date", LocalDate.class),
                r.getString("phone_number"),
                r.getString("email"),
                r.getString("status"),
                r.getObject("account_id", Long.class),
                r.getString("source"),
                r.getObject("import_batch_id", Long.class),
                r.getObject("department_id", Long.class),
                r.getInt("version"));
    }
}

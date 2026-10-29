package com.eldercare.backend.staffing.staff.internal.persistence;

import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import com.eldercare.backend.staffing.staff.internal.domain.StaffImportData;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Truy vấn trùng theo chỉ mục và chèn theo lô, không đặt UNIQUE cho tạo tay. */
@Repository
public class StaffImportStore {
    private final JdbcTemplate jdbc;

    public StaffImportStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Long> duplicates(Collection<String> phones, boolean lock) {
        if (phones.isEmpty()) return Map.of();
        var sorted = phones.stream().distinct().sorted().toList();
        var result = new LinkedHashMap<String, Long>();
        jdbc.query(
                "SELECT normalized_phone,id FROM staff_profile FORCE INDEX (idx_staff_normalized_phone) WHERE normalized_phone IN ("
                        + String.join(",", Collections.nCopies(sorted.size(), "?")) + ") ORDER BY normalized_phone,id"
                        + (lock ? " FOR UPDATE" : ""),
                (org.springframework.jdbc.core.RowCallbackHandler)
                        r -> result.putIfAbsent(r.getString(1), r.getLong(2)),
                sorted.toArray());
        return result;
    }

    public long batch(String fileName, int count, long actor, Instant at) {
        var key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var p = c.prepareStatement(
                            "INSERT INTO staff_import_batch(file_name,row_count,created_by,created_at) VALUES (?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    p.setString(1, fileName);
                    p.setInt(2, count);
                    p.setLong(3, actor);
                    p.setObject(4, ShiftStore.sql(at));
                    return p;
                },
                key);
        return key.getKey().longValue();
    }

    public List<Long> insert(long batch, List<StaffImportData.Row> rows, long actor, Instant at) {
        jdbc.batchUpdate(
                "INSERT INTO staff_profile(full_name,position,phone_number,normalized_phone,start_date,specialty,email,source,import_batch_id,created_by,created_at) VALUES (?,?,?,?,?,?,?,'IMPORT',?,?,?)",
                rows,
                rows.size(),
                (p, r) -> {
                    p.setString(1, r.fullName());
                    p.setString(2, r.position());
                    p.setString(3, r.phoneNumber());
                    p.setString(4, r.phoneNumber());
                    p.setObject(5, r.startDate());
                    p.setString(6, r.specialty());
                    p.setString(7, r.email());
                    p.setLong(8, batch);
                    p.setLong(9, actor);
                    p.setObject(10, ShiftStore.sql(at));
                });
        return jdbc.queryForList("SELECT id FROM staff_profile WHERE import_batch_id=? ORDER BY id", Long.class, batch);
    }
}

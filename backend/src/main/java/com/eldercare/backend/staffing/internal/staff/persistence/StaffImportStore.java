package com.eldercare.backend.staffing.internal.staff.persistence;

import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.staff.domain.StaffImportData;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Truy vấn trùng theo chỉ mục và chèn theo lô, không đặt UNIQUE cho tạo tay. */
@Repository
public class StaffImportStore {
    private final JdbcTemplate jdbc;
    private final com.eldercare.backend.staffing.internal.persistence.StaffingRecordStore records;

    public StaffImportStore(
            JdbcTemplate jdbc, com.eldercare.backend.staffing.internal.persistence.StaffingRecordStore records) {
        this.jdbc = jdbc;
        this.records = records;
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

    public long batch(String name, int count, long actor, Instant at) {
        return records.addImport(name, count, actor);
    }

    public List<Long> insert(long batch, List<StaffImportData.Row> rows, long actor, Instant at) {
        long physicalBatch = records.importPhysicalId(batch);
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
                    p.setLong(8, physicalBatch);
                    p.setLong(9, actor);
                    p.setObject(10, ShiftStore.sql(at));
                });
        return jdbc.queryForList(
                "SELECT id FROM staff_profile WHERE import_batch_id=? ORDER BY id", Long.class, physicalBatch);
    }
}

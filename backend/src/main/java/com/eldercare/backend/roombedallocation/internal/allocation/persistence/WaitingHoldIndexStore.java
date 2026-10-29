package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Chỉ mục kỹ thuật trên dòng vị trí đã khóa; không lưu thêm bản ghi nghiệp vụ. */
@Repository
public class WaitingHoldIndexStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public WaitingHoldIndexStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<Long> current(long residentId) {
        var rows = jdbc.queryForList(
                "SELECT waiting_hold_bed_ids FROM resident_position WHERE resident_id=? FOR UPDATE",
                String.class,
                residentId);
        if (rows.isEmpty()) return List.of();
        var ids = new ArrayList<Long>();
        json.readTree(rows.getFirst()).forEach(node -> ids.add(node.longValue()));
        return ids.stream().distinct().sorted().toList();
    }

    public void update(long residentId, long bedId, boolean add) {
        jdbc.update(
                "INSERT INTO resident_position(resident_id) VALUES(?) ON DUPLICATE KEY UPDATE resident_id=resident_position.resident_id",
                residentId);
        var ids = new ArrayList<>(current(residentId));
        ids.remove(bedId);
        if (add) ids.add(bedId);
        jdbc.update(
                "UPDATE resident_position SET waiting_hold_bed_ids=? WHERE resident_id=?",
                json.writeValueAsString(ids.stream().sorted().toList()),
                residentId);
    }
}

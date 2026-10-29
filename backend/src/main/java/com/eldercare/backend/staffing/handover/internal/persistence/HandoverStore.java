package com.eldercare.backend.staffing.handover.internal.persistence;

import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.staffing.handover.internal.domain.*;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** T026: đọc trạng thái theo khóa duy nhất ca; chưa có bàn giao không đồng nghĩa đã hoàn tất. */
@Repository
public class HandoverStore {
    private final JdbcTemplate jdbc;
    private final tools.jackson.databind.json.JsonMapper json;

    public HandoverStore(JdbcTemplate jdbc, tools.jackson.databind.json.JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<Long> unconfirmedPrevious(Instant from, Instant to) {
        return jdbc.queryForList(
                "SELECT s.id FROM work_shift s LEFT JOIN shift_handover h ON h.shift_id=s.id WHERE s.requires_handover=TRUE AND s.scope_id IS NOT NULL AND s.status NOT IN ('DRAFT','CANCELLED') AND s.starts_at<? AND s.ends_at>=? AND (h.id IS NULL OR h.status<>'CONFIRMED') ORDER BY s.id",
                Long.class,
                ShiftStore.sql(to),
                ShiftStore.sql(from));
    }

    public Optional<String> statusForShift(long shiftId) {
        return jdbc.queryForList("SELECT status FROM shift_handover WHERE shift_id=?", String.class, shiftId).stream()
                .findFirst();
    }

    public List<Long> reminderCandidates(Instant at) {
        return jdbc.queryForList(
                "SELECT s.id FROM work_shift s LEFT JOIN shift_handover h ON h.shift_id=s.id WHERE s.requires_handover=TRUE AND s.scope_id IS NOT NULL AND s.status NOT IN ('DRAFT','CANCELLED') AND (s.status<>'CLOSED' OR h.id IS NOT NULL) AND (h.id IS NULL OR h.status<>'CONFIRMED') AND s.starts_at<=? AND (s.prepare_handover_reminded_at IS NULL OR s.shift_end_reminded_at IS NULL OR s.unconfirmed_handover_reminded_at IS NULL) ORDER BY s.id",
                Long.class,
                ShiftStore.sql(at));
    }

    public boolean claimReminder(long id, String column, Instant at) {
        if (!Set.of("prepare_handover_reminded_at", "shift_end_reminded_at", "unconfirmed_handover_reminded_at")
                .contains(column)) throw new IllegalArgumentException("Cột nhắc không hợp lệ");
        return jdbc.update(
                        "UPDATE work_shift SET " + column + "=? WHERE id=? AND " + column + " IS NULL",
                        ShiftStore.sql(at),
                        id)
                == 1;
    }

    private static Instant at(ResultSet row, String column) throws SQLException {
        var value = row.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static final RowMapper<HandoverRecord> PARENT = (r, n) -> new HandoverRecord(
            r.getLong("id"),
            r.getLong("shift_id"),
            r.getObject("next_shift_id", Long.class),
            r.getLong("handover_staff_id"),
            r.getObject("created_by", Long.class),
            r.getString("status"),
            at(r, "created_at"),
            at(r, "finalized_at"),
            at(r, "confirmed_at"),
            r.getLong("version"));
    private static final RowMapper<HandoverItem> ITEM = (r, n) -> new HandoverItem(
            r.getLong("id"),
            r.getLong("handover_id"),
            r.getString("item_group"),
            r.getString("source_type"),
            r.getString("source_id"),
            r.getObject("resident_id", Long.class),
            r.getObject("room_id", Long.class),
            r.getString("description"),
            r.getString("note"),
            r.getBoolean("resolved"),
            r.getString("section"),
            r.getString("status_at_finalization"),
            r.getString("note_author"));

    public HandoverRecord record(long id, boolean lock) {
        return jdbc.query("SELECT * FROM shift_handover WHERE id=?" + (lock ? " FOR UPDATE" : ""), PARENT, id).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public Optional<HandoverRecord> forShift(long id) {
        return jdbc.query("SELECT * FROM shift_handover WHERE shift_id=?", PARENT, id).stream()
                .findFirst();
    }

    public long create(long shift, Long next, long staff, long actor, Instant now, String basis) {
        return insert(
                "INSERT INTO shift_handover(shift_id,next_shift_id,handover_staff_id,created_by,created_at,duty_basis) VALUES (?,?,?,?,?,?)",
                shift,
                next,
                staff,
                actor,
                ShiftStore.sql(now),
                basis);
    }

    public List<HandoverItem> items(long id) {
        return jdbc.query("SELECT * FROM handover_item WHERE handover_id=? ORDER BY id", ITEM, id);
    }

    public HandoverItem item(long parent, long child) {
        return jdbc.query("SELECT * FROM handover_item WHERE handover_id=? AND id=?", ITEM, parent, child).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public long add(long parent, HandoverItemInput i, String section, String author, Instant now) {
        return insert(
                "INSERT INTO handover_item(handover_id,item_group,source_type,source_id,resident_id,room_id,description,note,note_author,resolved,section,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                parent,
                i.group(),
                i.sourceType(),
                i.sourceId(),
                i.residentId(),
                i.roomId(),
                i.description(),
                i.note(),
                author,
                i.resolved(),
                section,
                ShiftStore.sql(now));
    }

    public void update(long parent, long child, HandoverItemInput i, String author) {
        jdbc.update(
                "UPDATE handover_item SET item_group=?,source_type=?,source_id=?,resident_id=?,room_id=?,description=?,note=?,note_author=?,resolved=?,section=? WHERE handover_id=? AND id=?",
                i.group(),
                i.sourceType(),
                i.sourceId(),
                i.residentId(),
                i.roomId(),
                i.description(),
                i.note(),
                author,
                i.resolved(),
                i.resolved() ? "RESOLVED_DURING_SHIFT" : "FINALIZED",
                parent,
                child);
    }

    public void delete(long parent, long child) {
        jdbc.update("DELETE FROM handover_item WHERE handover_id=? AND id=?", parent, child);
    }

    public void bump(long id) {
        jdbc.update("UPDATE shift_handover SET version=version+1 WHERE id=?", id);
    }

    public void closeLeadHistory(long shift, Instant at) {
        jdbc.update(
                "UPDATE shift_lead_history SET ends_at=? WHERE shift_id=? AND ends_at IS NULL",
                ShiftStore.sql(at),
                shift);
    }

    public void assess(long id, String general, Map<Long, String> residents) {
        if (general != null)
            jdbc.update(
                    "INSERT INTO handover_assessment(handover_id,assessment_kind,content) VALUES (?,'GENERAL',?) ON DUPLICATE KEY UPDATE content=?",
                    id,
                    general,
                    general);
        for (var e : residents.entrySet())
            jdbc.update(
                    "INSERT INTO handover_assessment(handover_id,assessment_kind,resident_id,content) VALUES (?,'BY_RESIDENT',?,?) ON DUPLICATE KEY UPDATE content=?",
                    id,
                    e.getKey(),
                    e.getValue(),
                    e.getValue());
    }

    public String general(long id) {
        return jdbc
                .queryForList(
                        "SELECT content FROM handover_assessment WHERE handover_id=? AND assessment_kind='GENERAL'",
                        String.class,
                        id)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public List<Map<String, Object>> assessments(long id) {
        return jdbc.queryForList(
                "SELECT resident_id AS residentId,content FROM handover_assessment WHERE handover_id=? AND assessment_kind='BY_RESIDENT' ORDER BY id",
                id);
    }

    public void freezeItem(long id, String status) {
        jdbc.update("UPDATE handover_item SET status_at_finalization=? WHERE id=?", status, id);
    }

    public void complete(long id, long actor, Instant now, List<HandoverSource> sources) {
        var keys = sources.stream()
                .map(r -> r.dataUnavailable() ? "UNAVAILABLE:" + r.sourceType() : r.sourceType() + ":" + r.sourceId())
                .toList();
        jdbc.update(
                "UPDATE shift_handover SET status='PREPARED',finalized_by=?,finalized_at=?,source_keys_at_finalization=?,version=version+1 WHERE id=?",
                actor,
                ShiftStore.sql(now),
                json.writeValueAsString(keys),
                id);
    }

    public Optional<Set<String>> sourceKeys(long id) {
        String value = jdbc.queryForObject(
                "SELECT source_keys_at_finalization FROM shift_handover WHERE id=?", String.class, id);
        if (value == null) return Optional.empty();
        var keys = new LinkedHashSet<String>();
        for (var key : json.readTree(value)) keys.add(key.asString());
        return Optional.of(keys);
    }

    public void state(long id, String status) {
        jdbc.update("UPDATE shift_handover SET status=?,version=version+1 WHERE id=?", status, id);
    }

    public void confirm(long id, Long next, long actor, Instant now, String basis) {
        jdbc.update(
                "UPDATE shift_handover SET status='CONFIRMED',next_shift_id=?,confirmed_by=?,confirmed_at=?,duty_basis=?,version=version+1 WHERE id=?",
                next,
                actor,
                ShiftStore.sql(now),
                basis,
                id);
    }

    public long comment(long id, String content, String author, Instant now) {
        return insert(
                "INSERT INTO handover_comment(handover_id,content,sender,created_at) VALUES (?,?,?,?)",
                id,
                content,
                author,
                ShiftStore.sql(now));
    }

    public void reply(long id, String content, String author, Instant now) {
        jdbc.update(
                "INSERT INTO handover_reply(comment_id,content,sender,created_at) VALUES (?,?,?,?)",
                id,
                content,
                author,
                ShiftStore.sql(now));
    }

    public List<Map<String, Object>> comments(long id) {
        return jdbc.query(
                "SELECT * FROM handover_comment WHERE handover_id=? ORDER BY id",
                (r, n) -> {
                    var row = new LinkedHashMap<String, Object>();
                    long comment = r.getLong("id");
                    row.put("id", comment);
                    row.put("content", r.getString("content"));
                    row.put("sender", r.getString("sender"));
                    row.put("at", at(r, "created_at"));
                    row.put(
                            "replies",
                            jdbc.query(
                                    "SELECT content,sender,created_at FROM handover_reply WHERE comment_id=? ORDER BY id",
                                    (p, k) -> Map.of(
                                            "content",
                                            p.getString("content"),
                                            "sender",
                                            p.getString("sender"),
                                            "at",
                                            at(p, "created_at")),
                                    comment));
                    return row;
                },
                id);
    }

    public Set<Long> unanswered(long id) {
        return new LinkedHashSet<>(jdbc.queryForList(
                "SELECT c.id FROM handover_comment c WHERE c.handover_id=? AND NOT EXISTS(SELECT 1 FROM handover_reply r WHERE r.comment_id=c.id) ORDER BY c.id",
                Long.class,
                id));
    }

    private long insert(String sql, Object... values) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }
}

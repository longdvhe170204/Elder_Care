package com.eldercare.backend.staffing.internal.handover.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.staffing.internal.domain.StaffingCatalogData.Legacy;
import com.eldercare.backend.staffing.internal.domain.StaffingEventData;
import com.eldercare.backend.staffing.internal.handover.domain.*;
import com.eldercare.backend.staffing.internal.handover.domain.HandoverJsonData.*;
import com.eldercare.backend.staffing.internal.persistence.StaffingHistoryStore;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** T026: đọc trạng thái theo khóa duy nhất ca; chưa có bàn giao không đồng nghĩa đã hoàn tất. */
@Repository
public class HandoverStore {
    private final JdbcTemplate jdbc;
    private final tools.jackson.databind.json.JsonMapper json;
    private final RecordEventApi events;
    private final StaffingHistoryStore histories;

    public HandoverStore(
            JdbcTemplate jdbc,
            tools.jackson.databind.json.JsonMapper json,
            RecordEventApi events,
            StaffingHistoryStore histories) {
        this.jdbc = jdbc;
        this.json = json;
        this.events = events;
        this.histories = histories;
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

    public List<HandoverItem> items(long parent) {
        return Arrays.stream(json.readValue(json.writeValueAsString(itemsJson(parent)), Item[].class))
                .map(i -> i.view(parent))
                .sorted(Comparator.comparingLong(HandoverItem::id))
                .toList();
    }

    public HandoverItem item(long parent, long child) {
        return items(parent).stream().filter(i -> i.id() == child).findFirst().orElseThrow(NotFoundException::new);
    }

    public long add(long parent, HandoverItemInput input, String section, String author, Instant now) {
        record(parent, true);
        long id = jdbc.queryForObject("SELECT next_item_id FROM shift_handover WHERE id=?", Long.class, parent);
        var items = itemsJson(parent);
        items.add(json.readTree(json.writeValueAsString(new Item(
                id,
                input.group(),
                input.sourceType(),
                input.sourceId(),
                input.residentId(),
                input.roomId(),
                input.description(),
                input.note(),
                author,
                input.resolved(),
                section,
                null,
                now))));
        jdbc.update(
                "UPDATE shift_handover SET items=?,next_item_id=next_item_id+1 WHERE id=?",
                json.writeValueAsString(items),
                parent);
        return id;
    }

    public void update(long parent, long child, HandoverItemInput input, String author) {
        record(parent, true);
        var array = itemsJson(parent);
        int index = itemIndex(array, child);
        var old = json.treeToValue(array.get(index), Item.class);
        var next = new Item(
                child,
                input.group(),
                input.sourceType(),
                input.sourceId(),
                input.residentId(),
                input.roomId(),
                input.description(),
                input.note(),
                author,
                input.resolved(),
                input.resolved() ? "RESOLVED_DURING_SHIFT" : "FINALIZED",
                old.statusAtFinalization(),
                old.createdAt());
        array.set(index, json.readTree(json.writeValueAsString(next)));
        saveItems(parent, array);
    }

    public void delete(long parent, long child) {
        record(parent, true);
        var array = itemsJson(parent);
        array.remove(itemIndex(array, child));
        saveItems(parent, array);
    }

    public void bump(long id) {
        jdbc.update("UPDATE shift_handover SET version=version+1 WHERE id=?", id);
    }

    public void closeLeadHistory(long shift, Instant at) {
        histories.closeShiftLead(shift, at);
    }

    public void assess(long parent, String general, Map<Long, String> residents) {
        record(parent, true);
        var array = assessmentsJson(parent);
        if (general != null) upsertAssessment(array, new Assessment("GENERAL", null, 0, general));
        residents.forEach((id, content) -> upsertAssessment(array, new Assessment("BY_RESIDENT", id, id, content)));
        jdbc.update("UPDATE shift_handover SET assessments=? WHERE id=?", json.writeValueAsString(array), parent);
    }

    public String general(long parent) {
        return Arrays.stream(json.readValue(json.writeValueAsString(assessmentsJson(parent)), Assessment[].class))
                .filter(a -> "GENERAL".equals(a.assessmentKind()))
                .map(Assessment::content)
                .findFirst()
                .orElse(null);
    }

    public List<Map<String, Object>> assessments(long parent) {
        return Arrays.stream(json.readValue(json.writeValueAsString(assessmentsJson(parent)), Assessment[].class))
                .filter(a -> "BY_RESIDENT".equals(a.assessmentKind()))
                .<Map<String, Object>>map(a -> Map.of("residentId", a.residentId(), "content", a.content()))
                .toList();
    }

    public void freezeItem(long parent, long child, String status) {
        record(parent, true);
        var array = itemsJson(parent);
        ((ObjectNode) array.get(itemIndex(array, child))).put("statusAtFinalization", status);
        saveItems(parent, array);
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

    public long comment(long parent, String content, String author, Instant now) {
        requireState(parent, "PREPARED");
        return events.add(
                        StaffingEventData.COMMENT,
                        new EntityRef("M09.HANDOVER", parent),
                        new NewRecordEvent<>(
                                new StaffingEventData.Comment(content, author, null),
                                null,
                                null,
                                false,
                                now,
                                null,
                                now,
                                null,
                                null))
                .id();
    }

    public void reply(long id, String content, String author, Instant now) {
        var comment = commentRecord(id);
        requireState(comment.parent().entityId(), "HAS_COMMENTS");
        events.add(
                StaffingEventData.REPLY,
                comment.parent(),
                new NewRecordEvent<>(
                        new StaffingEventData.Comment(content, author, null),
                        comment.id(),
                        null,
                        false,
                        now,
                        null,
                        now,
                        null,
                        null));
    }

    public List<Map<String, Object>> comments(long parent) {
        var ref = new EntityRef("M09.HANDOVER", parent);
        var replies = events.list(StaffingEventData.REPLY, ref);
        var result = new ArrayList<Map<String, Object>>();
        for (var c : events.list(StaffingEventData.COMMENT, ref)) {
            var row = new LinkedHashMap<String, Object>();
            row.put("id", Legacy.logicalId(c.id(), c.content().legacy(), "handover_comment"));
            row.put("content", c.content().content());
            row.put("sender", c.content().sender());
            row.put("at", c.occurredAt());
            row.put(
                    "replies",
                    replies.stream()
                            .filter(r -> Objects.equals(r.replyToId(), c.id()))
                            .map(r -> Map.of(
                                    "content",
                                    r.content().content(),
                                    "sender",
                                    r.content().sender(),
                                    "at",
                                    r.occurredAt()))
                            .toList());
            result.add(row);
        }
        return result;
    }

    public Set<Long> unanswered(long parent) {
        var ref = new EntityRef("M09.HANDOVER", parent);
        var answered = events.list(StaffingEventData.REPLY, ref).stream()
                .map(RecordEvent::replyToId)
                .collect(java.util.stream.Collectors.toSet());
        var result = new LinkedHashSet<Long>();
        for (var c : events.list(StaffingEventData.COMMENT, ref))
            if (!answered.contains(c.id()))
                result.add(Legacy.logicalId(c.id(), c.content().legacy(), "handover_comment"));
        return result;
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

    public long originalRecorder(long id) {
        return jdbc.queryForObject(
                "SELECT COALESCE(finalized_by,created_by) FROM shift_handover WHERE id=?", Long.class, id);
    }

    public Long confirmedBy(long id) {
        return jdbc.queryForObject("SELECT confirmed_by FROM shift_handover WHERE id=?", Long.class, id);
    }

    private ArrayNode itemsJson(long parent) {
        return array("SELECT items FROM shift_handover WHERE id=?", parent);
    }

    private ArrayNode assessmentsJson(long parent) {
        return array("SELECT assessments FROM shift_handover WHERE id=?", parent);
    }

    private ArrayNode array(String statement, long parent) {
        var rows = jdbc.queryForList(statement, String.class, parent);
        return rows.isEmpty() ? json.createArrayNode() : (ArrayNode) json.readTree(rows.getFirst());
    }

    private int itemIndex(ArrayNode array, long child) {
        for (int i = 0; i < array.size(); i++) if (array.get(i).get("id").asLong() == child) return i;
        throw new NotFoundException();
    }

    private void saveItems(long parent, ArrayNode items) {
        jdbc.update("UPDATE shift_handover SET items=? WHERE id=?", json.writeValueAsString(items), parent);
    }

    private void upsertAssessment(ArrayNode array, Assessment input) {
        if (("GENERAL".equals(input.assessmentKind()) && input.residentId() != null)
                || ("BY_RESIDENT".equals(input.assessmentKind())
                        && (input.residentId() == null || input.residentId() <= 0)))
            throw new IllegalArgumentException("Nhận định không đúng loại hoặc người cao tuổi");
        for (int i = 0; i < array.size(); i++) {
            var old = json.treeToValue(array.get(i), Assessment.class);
            if (old.assessmentKind().equals(input.assessmentKind()) && old.residentKey() == input.residentKey()) {
                array.set(i, json.readTree(json.writeValueAsString(input)));
                return;
            }
        }
        array.add(json.readTree(json.writeValueAsString(input)));
    }

    private RecordEvent<StaffingEventData.Comment> commentRecord(long logicalId) {
        var row = events.findByUniqueKey(StaffingEventData.COMMENT, "M09.HANDOVER_COMMENT:legacy:" + logicalId)
                .orElseGet(() -> events.require(StaffingEventData.COMMENT, logicalId));
        if (Legacy.logicalId(row.id(), row.content().legacy(), "handover_comment") != logicalId)
            throw new NotFoundException();
        return row;
    }

    private void requireState(long parent, String expected) {
        if (!expected.equals(record(parent, true).status()))
            throw new com.eldercare.backend.common.error.BusinessException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "STATE_CHANGED",
                    "Trạng thái bàn giao đã đổi, hãy tải lại");
    }
}

package com.eldercare.backend.staffing.handover.internal.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.administration.correction.api.dto.*;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.events.HandoverCorrected;
import com.eldercare.backend.staffing.handover.internal.persistence.HandoverStore;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import com.eldercare.backend.staffing.staff.internal.persistence.StaffStore;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** T028: đính chính nội dung và phạm vi chính chủ M09. Quyền cách A/B/C vẫn do 000 kiểm tra. */
@Component
public class HandoverCorrection implements CorrectableRecordProvider, ScopeAssignmentProvider {
    private final HandoverStore store;
    private final HandoverContents contents;
    private final ShiftStore shifts;
    private final StaffStore staff;
    private final DutyApi duties;
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;
    private final HandoverNotifications notifications;
    private final BusinessTime time;
    private final ObjectProvider<HandoverService> handovers;
    private final ObjectProvider<CurrentUser> users;
    private final FieldVisibilityApi fields;

    public HandoverCorrection(
            HandoverStore store,
            HandoverContents contents,
            ShiftStore shifts,
            StaffStore staff,
            DutyApi duties,
            JdbcTemplate jdbc,
            ApplicationEventPublisher events,
            HandoverNotifications notifications,
            BusinessTime time,
            ObjectProvider<HandoverService> handovers,
            ObjectProvider<CurrentUser> users,
            FieldVisibilityApi fields) {
        this.store = store;
        this.contents = contents;
        this.shifts = shifts;
        this.staff = staff;
        this.duties = duties;
        this.jdbc = jdbc;
        this.events = events;
        this.notifications = notifications;
        this.time = time;
        this.handovers = handovers;
        this.users = users;
        this.fields = fields;
    }

    public String recordType() {
        return "M09.HANDOVER";
    }

    public String entityType() {
        return recordType();
    }

    public boolean isConfirmed(long id) {
        return "CONFIRMED".equals(store.record(id, false).status());
    }

    public long originalRecorder(long id) {
        return jdbc.queryForObject(
                "SELECT COALESCE(finalized_by,created_by) FROM shift_handover WHERE id=?", Long.class, id);
    }

    public Optional<Long> floorArea(long id) {
        return Optional.ofNullable(
                shifts.shift(store.record(id, false).shiftId(), false).scopeId());
    }

    public Target assignScope(long id) {
        return new Target(recordType(), Long.toString(id), null, floorArea(id).orElse(null));
    }

    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.NURSING_STAFF) || user.roles().contains(Role.FLOOR_MANAGER);
    }

    public Object originalContent(long id) {
        handovers.getObject().record(id);
        // Chuỗi 000 chứa cả nội dung gốc và các lần sửa: chỉ trả khi xem được đầy đủ mọi đối tượng trong chuỗi.
        var linked = new LinkedHashSet<Long>();
        store.items(id).stream()
                .map(i -> i.residentId())
                .filter(Objects::nonNull)
                .forEach(linked::add);
        store.assessments(id).forEach(a -> linked.add(((Number) a.get("residentId")).longValue()));
        var floor = floorArea(id).orElse(null);
        if (users.getObject().roles().contains(Role.CAREGIVER))
            throw new com.eldercare.backend.common.error.NotFoundException();
        for (var resident : linked)
            if (fields.canView(
                            users.getObject(),
                            InfoGroup.HEALTH,
                            new Target(recordType(), Long.toString(id), resident, floor))
                    != Visibility.FULL) throw new com.eldercare.backend.common.error.NotFoundException();
        return contents.original(id);
    }

    public void validateContent(Object value) {
        if (!(value instanceof Map<?, ?> m)
                || m.isEmpty()
                || !Set.of("generalAssessment", "items", "residentAssessments", "comments")
                        .containsAll(m.keySet())) throw invalid();
        if (m.containsKey("generalAssessment")
                && (!(m.get("generalAssessment") instanceof String text) || text.isBlank())) throw invalid();
        for (String field : List.of("items", "residentAssessments", "comments"))
            if (m.containsKey(field)) {
                if (!(m.get(field) instanceof List<?> rows)) throw invalid();
                for (var row : rows) {
                    if (!(row instanceof Map<?, ?> r)) throw invalid();
                    String key = field.equals("residentAssessments") ? "residentId" : "id";
                    if (!(r.get(key) instanceof Number)) throw invalid();
                    var allowed = field.equals("items")
                            ? Set.of("id", "description", "note")
                            : field.equals("comments")
                                    ? Set.of("id", "content", "replies")
                                    : Set.of("residentId", "content");
                    if (!allowed.containsAll(r.keySet())) throw invalid();
                    for (String text : List.of("description", "note", "content"))
                        if (r.containsKey(text) && r.get(text) != null && !(r.get(text) instanceof String))
                            throw invalid();
                    if (r.containsKey("description")
                            && (r.get("description") == null
                                    || r.get("description").toString().isBlank())) throw invalid();
                    if (r.containsKey("replies")) {
                        if (!(r.get("replies") instanceof List<?> replies)) throw invalid();
                        for (var reply : replies)
                            if (!(reply instanceof Map<?, ?> p)
                                    || !p.keySet().equals(Set.of("content"))
                                    || !(p.get("content") instanceof String)) throw invalid();
                    }
                }
            }
    }

    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        var h = store.record(id, false);
        var s = shifts.shift(h.shiftId(), false);
        var staffId = user.staffId().orElse(null);
        if (staffId == null) return false;
        var confirmed = jdbc.queryForObject("SELECT confirmed_by FROM shift_handover WHERE id=?", Long.class, id);
        return user.accountId() == originalRecorder(id)
                || Objects.equals(confirmed, user.accountId())
                || Objects.equals(s.leadStaffId(), staffId)
                || duties.assignedFloorManager(s.scopeId(), time.now())
                        .filter(staffId::equals)
                        .isPresent()
                || duties.floorManagerSubstitute(s.scopeId(), time.now())
                        .filter(staffId::equals)
                        .isPresent();
    }

    public void afterCorrection(CorrectionView correction) {
        long id = correction.originalRecordId();
        originalContent(id);
        if (correction.correctedContent() instanceof Map<?, ?> m) {
            var original = contents.original(id);
            for (String field : List.of("items", "residentAssessments", "comments"))
                if (m.containsKey(field)) {
                    String key = field.equals("residentAssessments") ? "residentId" : "id";
                    var before = HandoverContents.patches(original.get(field), key);
                    var edits = HandoverContents.patches(m.get(field), key);
                    if (!before.keySet().containsAll(edits.keySet()) || edits.size() != ((List<?>) m.get(field)).size())
                        throw invalid();
                    if (field.equals("comments"))
                        for (var e : edits.entrySet())
                            if (e.getValue().get("replies") instanceof List<?> replies
                                    && replies.size()
                                            != ((List<?>) before.get(e.getKey()).get("replies")).size())
                                throw invalid();
                }
        }
        var h = store.record(id, false);
        var shift = shifts.shift(h.shiftId(), false);
        Long receiver = jdbc.queryForObject("SELECT confirmed_by FROM shift_handover WHERE id=?", Long.class, id);
        var receiverStaff = receiver == null
                ? Optional.<Long>empty()
                : jdbc.queryForList("SELECT id FROM staff_profile WHERE account_id=?", Long.class, receiver).stream()
                        .findFirst();
        events.publishEvent(new HandoverCorrected(
                id,
                time.now(),
                notifications.corrected(id, h.handoverStaffId(), receiverStaff.orElse(null), shift, time.now())));
    }

    private static InvalidValueException invalid() {
        return new InvalidValueException(
                "Chỉ đính chính nội dung mục, nhận định, ý kiến và phản hồi đã có; giữ nguyên nguồn và đối tượng");
    }
}

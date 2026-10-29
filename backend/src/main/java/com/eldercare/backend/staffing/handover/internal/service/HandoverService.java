package com.eldercare.backend.staffing.handover.internal.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.staffing.api.events.*;
import com.eldercare.backend.staffing.assignment.internal.service.AssignmentSources;
import com.eldercare.backend.staffing.handover.internal.domain.*;
import com.eldercare.backend.staffing.handover.internal.persistence.HandoverStore;
import com.eldercare.backend.staffing.shift.internal.domain.WorkShift;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import com.eldercare.backend.staffing.staff.internal.persistence.StaffStore;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T028: lệnh do người dùng; khóa ca trước bàn giao; xác nhận và tác động nguồn cùng transaction. */
@Service
public class HandoverService {
    private final HandoverStore store;
    private final ShiftStore shifts;
    private final StaffStore staff;
    private final HandoverSources sources;
    private final AssignmentSources positions;
    private final ObjectProvider<CurrentUser> users;
    private final PermissionApi permissions;
    private final DutyApi duties;
    private final ParameterApi parameters;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final HandoverNotifications notifications;
    private final HandoverContents contents;

    public HandoverService(
            HandoverStore store,
            ShiftStore shifts,
            StaffStore staff,
            HandoverSources sources,
            AssignmentSources positions,
            ObjectProvider<CurrentUser> users,
            PermissionApi permissions,
            DutyApi duties,
            ParameterApi parameters,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            HandoverNotifications notifications,
            HandoverContents contents) {
        this.store = store;
        this.shifts = shifts;
        this.staff = staff;
        this.sources = sources;
        this.positions = positions;
        this.users = users;
        this.permissions = permissions;
        this.duties = duties;
        this.parameters = parameters;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.notifications = notifications;
        this.contents = contents;
    }

    public static BusinessException invalid(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    public HandoverRecord record(long id) {
        var h = store.record(id, false);
        requireView(h);
        return h;
    }

    public HandoverRecord forShift(long id) {
        var h = store.forShift(id).orElseThrow(NotFoundException::new);
        requireView(h);
        return h;
    }

    public Target target(long id) {
        var h = store.record(id, false);
        var s = shifts.shift(h.shiftId(), false);
        return new Target("M09.HANDOVER", Long.toString(id), null, s.scopeId());
    }

    public List<HandoverItem> items(long id) {
        var h = record(id);
        return contents.items(id).stream()
                .filter(i -> visible(i.residentId(), h))
                .toList();
    }

    public List<Map<String, Object>> assessments(long id) {
        var h = record(id);
        return contents.assessments(id).stream()
                .filter(r -> visible(((Number) r.get("residentId")).longValue(), h))
                .toList();
    }

    public String general(long id) {
        record(id);
        return contents.general(id);
    }

    public List<Map<String, Object>> comments(long id) {
        record(id);
        return users.getObject().roles().contains(Role.CAREGIVER) ? List.of() : contents.comments(id);
    }

    public List<HandoverSource> sources(long id, boolean afterPreparation) {
        var h = record(id);
        var included = store.items(id);
        var baseline = store.sourceKeys(id);
        var result = new ArrayList<HandoverSource>();
        var unknown = new HashSet<String>();
        for (var r : sources.list(shifts.shift(h.shiftId(), false))) {
            if (!visible(r.residentId(), h)) continue;
            if (afterPreparation && !r.dataUnavailable()) {
                if (h.finalizedAt() == null) continue;
                if (baseline.isPresent()) {
                    if (baseline.get().contains("UNAVAILABLE:" + r.sourceType())) {
                        if (unknown.add(r.sourceType()))
                            result.add(new HandoverSource(
                                    r.group(),
                                    r.sourceType(),
                                    null,
                                    null,
                                    null,
                                    "Nguồn chưa đọc được lúc chốt; chưa xác định được mục phát sinh sau khi lập",
                                    null,
                                    false,
                                    false,
                                    true,
                                    null,
                                    null));
                        continue;
                    }
                    if (baseline.get().contains(r.sourceType() + ":" + r.sourceId())) continue;
                } else if (r.at() == null || !r.at().isAfter(h.finalizedAt())) continue;
            }
            result.add(r.marked(included.stream()
                    .anyMatch(i -> Objects.equals(i.sourceType(), r.sourceType())
                            && Objects.equals(i.sourceId(), r.sourceId())
                            && r.sourceId() != null)));
        }
        return List.copyOf(result);
    }

    public Map<Long, HandoverSource> currentStates(long id) {
        var h = record(id);
        var visible = items(id);
        return visible.isEmpty() ? Map.of() : sources.current(shifts.shift(h.shiftId(), false), visible);
    }

    public List<String> allowedCommands(long id) {
        var h = record(id);
        var s = shifts.shift(h.shiftId(), false);
        var result = new ArrayList<String>();
        if ("DRAFT".equals(h.status()) && editor(s)) {
            result.addAll(List.of("addItem", "updateItem", "deleteItem", "assess"));
            if (sender(s)) result.add("complete");
        } else if (Set.of("PREPARED", "HAS_COMMENTS").contains(h.status())) {
            if (sender(s)) result.add("addItem");
            if ("HAS_COMMENTS".equals(h.status()) && sender(s)) result.add("supplementAndResubmit");
            if ("PREPARED".equals(h.status()) && receiver(s)) {
                result.add("confirm");
                result.add("comment");
            }
        }
        return List.copyOf(result);
    }

    public boolean canCreate(long shift) {
        var s = shifts.shift(shift, false);
        return s.handoverRequired()
                && !Set.of("DRAFT", "CANCELLED").contains(s.status())
                && store.forShift(shift).isEmpty()
                && !time.now().isBefore(preparationStarts(s))
                && sender(s);
    }

    @Transactional
    public HandoverRecord createHandover(long shift) {
        var s = shifts.shift(shift, true);
        if (!s.handoverRequired() || Set.of("DRAFT", "CANCELLED").contains(s.status()))
            throw invalid("CONDITION_NOT_MET", "Ca không yêu cầu bàn giao hoặc chưa công bố");
        if (time.now().isBefore(preparationStarts(s)))
            throw invalid("BEFORE_HANDOVER_PREPARATION_WINDOW", "Chưa tới mốc lập bàn giao");
        require(sender(s));
        if (store.forShift(shift).isPresent()) throw invalid("HANDOVER_ALREADY_EXISTS", "Ca đã có bàn giao");
        var next = next(s);
        long id;
        try {
            id = store.create(
                    shift,
                    next.map(WorkShift::id).orElse(null),
                    actor(),
                    users.getObject().accountId(),
                    time.now(),
                    basis(s));
        } catch (DuplicateKeyException ex) {
            throw invalid("HANDOVER_ALREADY_EXISTS", "Ca đã có bàn giao");
        }
        log("HANDOVER_CREATED", id, s, "DRAFT", Map.of());
        return store.record(id, false);
    }

    @Transactional
    public long addHandoverItem(long id, String ifMatch, HandoverItemInput input) {
        var h = lock(id);
        var s = shifts.shift(h.shiftId(), false);
        require("DRAFT".equals(h.status()) ? editor(s) : sender(s));
        if ("CONFIRMED".equals(h.status())) throw invalid("STATE_CHANGED", "Bàn giao đã xác nhận");
        VersionEtag.requireMatch(ifMatch, h.version(), h);
        validate(input, s);
        String section = h.finalizedAt() == null
                ? (input.resolved() ? "RESOLVED_DURING_SHIFT" : "FINALIZED")
                : "AFTER_PREPARATION";
        if (input.resolved()) validateResolved(input, s);
        long child = store.add(id, input, section, author(), time.now());
        sources.recorded(id, input);
        store.bump(id);
        log("HANDOVER_ITEM_ADDED", id, s, h.status(), Map.of("itemId", child));
        return child;
    }

    @Transactional
    public void updateItem(long id, long itemId, String ifMatch, HandoverItemInput input) {
        var h = lock(id);
        var s = shifts.shift(h.shiftId(), false);
        require(editor(s));
        draft(h);
        VersionEtag.requireMatch(ifMatch, h.version(), h);
        var original = store.item(id, itemId);
        if (Set.of("VITAL_SIGN", "RESIDENT_CHANGE").contains(original.group())
                && (!original.group().equals(input.group())
                        || !Objects.equals(original.sourceType(), input.sourceType())
                        || !Objects.equals(original.sourceId(), input.sourceId())
                        || !Objects.equals(original.residentId(), input.residentId())))
            throw invalid("HANDOVER_ITEM_REMOVAL_NOT_ALLOWED", "Không đổi nguồn hoặc nhóm của mục không bỏ được");
        if (Objects.equals(original.group(), input.group())
                && Objects.equals(original.sourceType(), input.sourceType())
                && Objects.equals(original.sourceId(), input.sourceId())
                && Objects.equals(original.residentId(), input.residentId())
                && Objects.equals(original.roomId(), input.roomId())) {
            if (blank(input.description())) throw invalid("CONDITION_NOT_MET", "Cần nội dung mục");
        } else validate(input, s);
        if (input.resolved()) validateResolved(input, s);
        store.update(id, itemId, input, author());
        store.bump(id);
        log("HANDOVER_ITEM_UPDATED", id, s, h.status(), Map.of("itemId", itemId));
    }

    @Transactional
    public void deleteItem(long id, long itemId, String ifMatch) {
        var h = lock(id);
        var s = shifts.shift(h.shiftId(), false);
        require(editor(s));
        draft(h);
        VersionEtag.requireMatch(ifMatch, h.version(), h);
        var item = store.item(id, itemId);
        if (Set.of("VITAL_SIGN", "RESIDENT_CHANGE").contains(item.group()))
            throw invalid("HANDOVER_ITEM_REMOVAL_NOT_ALLOWED", "Mục chỉ số và biến động người cao tuổi không bỏ được");
        store.delete(id, itemId);
        store.bump(id);
        log("HANDOVER_ITEM_REMOVED", id, s, h.status(), Map.of("itemId", itemId));
    }

    @Transactional
    public void assess(long id, String ifMatch, String general, Map<Long, String> residents) {
        var h = lock(id);
        var s = shifts.shift(h.shiftId(), false);
        require(editor(s));
        draft(h);
        VersionEtag.requireMatch(ifMatch, h.version(), h);
        for (var e : residents.entrySet()) {
            inLocation(e.getKey(), null, s);
            if (e.getValue() == null) throw invalid("CONDITION_NOT_MET", "Nhận định không hợp lệ");
        }
        store.assess(id, general, residents);
        store.bump(id);
        log("HANDOVER_ASSESSMENT_UPDATED", id, s, h.status(), Map.of());
    }

    @Transactional
    public void complete(long id) {
        var h = lock(id);
        var s = shifts.shift(h.shiftId(), false);
        require(sender(s));
        draft(h);
        if (blank(store.general(id)))
            throw invalid("CONDITION_NOT_MET", "Cần nhận định chung trước khi hoàn tất bàn giao");
        var items = store.items(id);
        var current = sources.current(s, items);
        var missing = items.stream()
                .filter(i -> current.get(i.id()).critical() && blank(i.note()))
                .map(HandoverItem::id)
                .toList();
        if (!missing.isEmpty())
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CRITICAL_ITEM_NOTE_REQUIRED",
                    "Các mục nghiêm trọng còn thiếu ghi chú",
                    Map.of(
                            "unmetConditions",
                            missing.stream()
                                    .map(i -> Map.of(
                                            "code",
                                            "CRITICAL_ITEM_NOTE_REQUIRED",
                                            "description",
                                            "Mục " + i + " cần ghi chú",
                                            "field",
                                            "items",
                                            "module",
                                            "008",
                                            "waivable",
                                            false))
                                    .toList()));
        for (var item : items)
            store.freezeItem(item.id(), current.get(item.id()).currentStatus());
        store.complete(id, users.getObject().accountId(), time.now(), sources.list(s));
        closeShift(s);
        log("HANDOVER_PREPARED", id, s, "PREPARED", Map.of());
        events.publishEvent(
                new HandoverPrepared(id, time.now(), notifications.prepared(id, s, next(s).orElse(null), time.now())));
    }

    @Transactional
    public void confirm(long id) {
        var h = lock(id);
        var s = shifts.shift(h.shiftId(), false);
        state(h, "PREPARED");
        require(receiver(s));
        var next = next(s);
        var taskIds = new LinkedHashSet<Long>();
        for (var i : store.items(id)) if ("M04.TASK".equals(i.sourceType())) taskIds.add(Long.valueOf(i.sourceId()));
        sources.confirm(id, s.id(), next.map(WorkShift::id).orElse(null), s.scopeId(), taskIds, time.now());
        store.confirm(
                id,
                next.map(WorkShift::id).orElse(null),
                users.getObject().accountId(),
                time.now(),
                basis(next.orElse(s)));
        log("HANDOVER_CONFIRMED", id, s, "CONFIRMED", Map.of());
        events.publishEvent(
                new HandoverConfirmed(id, s.id(), next.map(WorkShift::id).orElse(null), time.now()));
    }

    @Transactional
    public long comment(long id, String content) {
        var h = lock(id);
        var s = shifts.shift(h.shiftId(), false);
        state(h, "PREPARED");
        require(receiver(s));
        if (blank(content)) throw invalid("CONDITION_NOT_MET", "Nội dung ý kiến là bắt buộc");
        long child = store.comment(id, content.strip(), author(), time.now());
        store.state(id, "HAS_COMMENTS");
        log("HANDOVER_FEEDBACK_RECEIVED", id, s, "HAS_COMMENTS", Map.of("commentId", child));
        events.publishEvent(new HandoverFeedbackReceived(
                id, time.now(), notifications.feedback(id, h.handoverStaffId(), s, time.now())));
        return child;
    }

    @Transactional
    public void resubmit(long id, Map<Long, String> replies, List<HandoverItemInput> additional) {
        var h = lock(id);
        var s = shifts.shift(h.shiftId(), false);
        state(h, "HAS_COMMENTS");
        require(sender(s));
        var unanswered = store.unanswered(id);
        if (!replies.keySet().equals(unanswered) || replies.values().stream().anyMatch(HandoverService::blank))
            throw invalid("CONDITION_NOT_MET", "Cần phản hồi cho từng ý kiến chưa được trả lời");
        for (var item : additional) {
            validate(item, s);
            store.add(id, item, "AFTER_PREPARATION", author(), time.now());
            sources.recorded(id, item);
        }
        for (var reply : replies.entrySet())
            store.reply(reply.getKey(), reply.getValue().strip(), author(), time.now());
        store.state(id, "PREPARED");
        closeShift(s);
        log("HANDOVER_RESUBMITTED", id, s, "PREPARED", Map.of());
        events.publishEvent(
                new HandoverPrepared(id, time.now(), notifications.prepared(id, s, next(s).orElse(null), time.now())));
    }

    private HandoverRecord lock(long id) {
        var before = store.record(id, false);
        shifts.shift(before.shiftId(), true);
        return store.record(id, true);
    }

    private void closeShift(WorkShift s) {
        if (!s.endsAt().isAfter(time.now())
                && Set.of("IN_PROGRESS", "AWAITING_HANDOVER").contains(s.status())) {
            shifts.transition(s.id(), "CLOSED");
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("SHIFT_STATUS_CHANGED", false))
                    .entity("M09.SHIFT", s.id())
                    .before(Map.of("status", s.status()))
                    .after(Map.of("status", "CLOSED"))
                    .basis(new Basis("FR-022", null, s.id(), null))
                    .build());
            if ("IN_PROGRESS".equals(s.status())) {
                store.closeLeadHistory(s.id(), s.endsAt());
                events.publishEvent(new ShiftEnded(s.id(), s.scopeId(), "CLOSED", s.endsAt()));
            }
        }
    }

    public Optional<WorkShift> next(WorkShift s) {
        var gap = parameters.read(ParameterCode.CFG_M09_09).asDuration();
        return NextHandoverShift.find(
                s,
                shifts.floorRange(
                        s.scopeId(), s.startsAt(), s.endsAt().plus(gap).plusNanos(1000)),
                gap);
    }

    public Instant preparationStarts(WorkShift s) {
        var at = s.endsAt().minus(parameters.read(ParameterCode.CFG_M09_04).asDuration());
        return at.isBefore(s.startsAt()) ? s.startsAt() : at;
    }

    private boolean inWindow(WorkShift s) {
        var margin = parameters.read(ParameterCode.CFG_M15_07).asDuration();
        return !time.now().isBefore(s.startsAt().minus(margin))
                && !time.now().isAfter(s.endsAt().plus(margin));
    }

    private boolean present(WorkShift s) {
        return users.getObject()
                .staffId()
                .filter(id ->
                        staff.find(id).map(p -> "WORKING".equals(p.status())).orElse(false)
                                && shifts.members(s.id()).stream()
                                        .anyMatch(m -> m.staffId() == id && m.presentAt(time.now())))
                .isPresent();
    }

    private boolean lead(WorkShift s) {
        return !s.missingLead()
                && users.getObject()
                        .staffId()
                        .filter(id -> Objects.equals(id, s.leadStaffId()))
                        .isPresent()
                && present(s)
                && inWindow(s);
    }

    private boolean floor(WorkShift s) {
        var mine = users.getObject().staffId();
        return mine.isPresent()
                && (duties.assignedFloorManager(s.scopeId(), time.now())
                                .filter(mine.get()::equals)
                                .isPresent()
                        || duties.floorManagerSubstitute(s.scopeId(), time.now())
                                .filter(mine.get()::equals)
                                .isPresent());
    }

    private boolean permitted(Operation operation, WorkShift s, boolean ownMembership) {
        var check = permissions.hasPermission(
                users.getObject(), new PermissionKey(Function.SHIFT_HANDOVER, operation), target(s));
        return check.allowed()
                || ownMembership
                        && check.failedLayer().filter(FailedLayer.SCOPE::equals).isPresent();
    }

    private boolean sender(WorkShift s) {
        boolean lead = lead(s);
        boolean fallback = !leadAvailable(s) && floor(s);
        return (lead || fallback) && permitted(Operation.UPDATE, s, lead || fallback);
    }

    private boolean leadAvailable(WorkShift s) {
        return s.leadStaffId() != null
                && !s.missingLead()
                && inWindow(s)
                && staff.find(s.leadStaffId())
                        .map(p -> "WORKING".equals(p.status()))
                        .orElse(false)
                && shifts.members(s.id()).stream()
                        .anyMatch(m -> Objects.equals(m.staffId(), s.leadStaffId()) && m.presentAt(time.now()));
    }

    private boolean editor(WorkShift s) {
        boolean member = users.getObject().roles().contains(Role.NURSING_STAFF) && present(s) && inWindow(s);
        return sender(s) || member && permitted(Operation.UPDATE, s, true);
    }

    private boolean receiver(WorkShift s) {
        var next = next(s);
        boolean lead = next.map(this::lead).orElse(false);
        boolean manager = floor(s);
        return (lead || manager) && permitted(Operation.CONFIRM, next.orElse(s), lead || manager);
    }

    private Target target(WorkShift s) {
        return new Target("M09.SHIFT", Long.toString(s.id()), null, s.scopeId());
    }

    private void requireView(HandoverRecord h) {
        var s = shifts.shift(h.shiftId(), false);
        var allowedRoles =
                Set.of(Role.NURSING_HOME_MANAGER, Role.FLOOR_MANAGER, Role.NURSING_STAFF, Role.DOCTOR, Role.CAREGIVER);
        if (Collections.disjoint(users.getObject().roles(), allowedRoles)) throw new NotFoundException();
        boolean own = present(s) && inWindow(s);
        boolean allowed = permitted(Operation.VIEW, s, own);
        if (!allowed && users.getObject().roles().contains(Role.CAREGIVER))
            allowed = store.items(h.id()).stream().anyMatch(i -> i.residentId() != null && visible(i.residentId(), h));
        if (!allowed) throw new NotFoundException();
    }

    public boolean visible(Long resident, HandoverRecord h) {
        if (!users.getObject().roles().contains(Role.CAREGIVER)) return true;
        if (resident == null) return false;
        return permissions
                .hasPermission(
                        users.getObject(),
                        new PermissionKey(Function.SHIFT_HANDOVER, Operation.VIEW),
                        new Target(
                                "M09.HANDOVER",
                                Long.toString(h.id()),
                                resident,
                                shifts.shift(h.shiftId(), false).scopeId()))
                .allowed();
    }

    private void validate(HandoverItemInput i, WorkShift s) {
        if (i == null
                || blank(i.group())
                || blank(i.description())
                || !Set.of(
                                "TASK",
                                "DOSE",
                                "VITAL_SIGN",
                                "ALERT",
                                "INCIDENT",
                                "RESIDENT_CHANGE",
                                "BED_AWAITING_READINESS",
                                "STORED_BELONGING",
                                "FOLLOW_UP_ISSUE")
                        .contains(i.group())
                || i.residentId() == null && i.roomId() == null)
            throw invalid("CONDITION_NOT_MET", "Mục cần nhóm, nội dung và người cao tuổi hoặc phòng");
        if ((i.sourceId() == null) != (i.sourceType() == null))
            throw invalid("CONDITION_NOT_MET", "Liên kết nguồn cần đủ loại và mã");
        if (Set.of("TASK", "DOSE", "ALERT", "INCIDENT").contains(i.group()) && i.sourceId() == null)
            throw invalid("CONDITION_NOT_MET", "Chọn bản ghi nguồn; ghi tự do dùng Vấn đề cần theo dõi");
        if (i.sourceId() != null) {
            var current = sources.list(s).stream()
                    .filter(r -> !r.dataUnavailable()
                            && Objects.equals(r.sourceType(), i.sourceType())
                            && Objects.equals(r.sourceId(), i.sourceId())
                            && r.group().equals(i.group()))
                    .findFirst();
            // Thêm mới phải chứng minh nguồn thuộc danh sách của ca; trạng thái đơn lẻ không chứng minh được đối tượng.
            // updateItem đã giữ định danh của mục từng được kiểm, nên vẫn đọc được nguồn đã đóng để ghi đã xử lý.
            if (current.isEmpty()) throw HandoverSources.unavailable();
            if (current.isEmpty()
                    || !Objects.equals(current.get().residentId(), i.residentId())
                    || !Objects.equals(current.get().roomId(), i.roomId()))
                throw invalid("CONDITION_NOT_MET", "Mục không khớp người cao tuổi hoặc phòng của nguồn");
        } else inLocation(i.residentId(), i.roomId(), s);
    }

    private void inLocation(Long resident, Long room, WorkShift s) {
        if (resident != null) {
            var p = positions.position(resident, time.now());
            boolean matches = p.isPresent()
                    && ("FLOOR".equals(s.scopeType())
                            ? Objects.equals(p.get().floorId(), s.scopeId())
                            : Objects.equals(p.get().areaId(), s.scopeId()));
            if (!matches) throw invalid("OUTSIDE_SHIFT_SCOPE", "Người cao tuổi ngoài phạm vi ca");
        }
        if (room != null) {
            var r = positions.structure().room(room);
            boolean matches = r.isPresent()
                    && ("FLOOR".equals(s.scopeType())
                            ? Objects.equals(r.get().floorId(), s.scopeId())
                            : positions
                                    .structure()
                                    .floorsInArea(s.scopeId())
                                    .contains(r.get().floorId()));
            if (!matches) throw invalid("OUTSIDE_SHIFT_SCOPE", "Phòng ngoài phạm vi ca");
        }
    }

    private void validateResolved(HandoverItemInput i, WorkShift s) {
        if (i.sourceId() == null || Set.of("VITAL_SIGN", "RESIDENT_CHANGE").contains(i.group()))
            throw invalid("CONDITION_NOT_MET", "Nguồn này không có trạng thái đã xử lý");
        var item = new HandoverItem(
                -1,
                -1,
                i.group(),
                i.sourceType(),
                i.sourceId(),
                i.residentId(),
                i.roomId(),
                i.description(),
                i.note(),
                true,
                "FINALIZED",
                null,
                null);
        var status = sources.current(s, List.of(item)).get(-1L).currentStatus();
        boolean resolved = switch (i.group()) {
            case "TASK" ->
                Set.of("COMPLETED", "COMPLETED_LATE", "NOT_PERFORMED", "CANCELLED")
                        .contains(status);
            case "DOSE" -> !Set.of("LATE", "MISSED", "REFUSED", "CARRIED").contains(status);
            case "ALERT", "INCIDENT" -> Set.of("CLOSED", "CANCELLED").contains(status);
            case "BED_AWAITING_READINESS" -> !"AWAITING_READINESS".equals(status);
            case "STORED_BELONGING" -> "NOT_HELD".equals(status);
            default -> false;
        };
        if (!resolved) throw invalid("CONDITION_NOT_MET", "Nguồn chưa được xử lý xong");
    }

    private String basis(WorkShift s) {
        return lead(s) ? "SHIFT_LEAD" : "FLOOR_MANAGER";
    }

    private String author() {
        return staff.find(actor()).map(p -> p.fullName()).orElse(Long.toString(actor()));
    }

    private long actor() {
        return users.getObject().staffId().orElseThrow(() -> new PermissionDeniedException("Cần tài khoản nhân viên"));
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static void require(boolean allowed) {
        if (!allowed) throw new PermissionDeniedException("Không có quyền bàn giao trong phạm vi ca tại thời điểm này");
    }

    private static void state(HandoverRecord h, String expected) {
        if (!expected.equals(h.status())) throw invalid("STATE_CHANGED", "Trạng thái bàn giao đã đổi, hãy tải lại");
    }

    private static void draft(HandoverRecord h) {
        state(h, "DRAFT");
    }

    private void log(String action, long id, WorkShift s, String status, Map<String, Object> details) {
        var data = new LinkedHashMap<String, Object>(details);
        data.put("basis", basis(s));
        data.put("shiftId", s.id());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, false))
                .entity("M09.HANDOVER", id)
                .after(Map.of("status", status))
                .details(data)
                .build());
    }
}

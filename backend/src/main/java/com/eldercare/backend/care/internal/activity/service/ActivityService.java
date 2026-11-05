package com.eldercare.backend.care.internal.activity.service;

import static com.eldercare.backend.care.internal.activity.domain.ActivityKinds.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.care.internal.activity.domain.Activity;
import com.eldercare.backend.care.internal.activity.domain.RegularParticipant;
import com.eldercare.backend.care.internal.activity.domain.TemplateVersion;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 014 T004 (FR-002 → FR-005, FR-009, FR-010; research L-04): hoạt động do Trưởng tầng khai báo. */
@Service
public class ActivityService {
    private static final String ENTITY = "M04.ACTIVITY";

    private final SimpleRecordApi records;
    private final CatalogApi catalogs;
    private final CurrentUser user;
    private final AuditLogApi audit;
    private final BusinessTime time;

    public ActivityService(
            SimpleRecordApi records, CatalogApi catalogs, CurrentUser user, AuditLogApi audit, BusinessTime time) {
        this.records = records;
        this.catalogs = catalogs;
        this.user = user;
        this.audit = audit;
        this.time = time;
    }

    public static Target target(Long floorAreaId) {
        return new Target(ENTITY, null, null, floorAreaId);
    }

    public Target target(long id) {
        return target(records.require(ACTIVITIES, id).content().floorAreaId());
    }

    /** Hoạt động của tầng (kèm hoạt động toàn viện) hoặc mọi hoạt động khi không nêu tầng. */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<SimpleRecord<Activity>> list(Target target, Long floorAreaId) {
        return records.listByStatus(ACTIVITIES, Set.of(ACTIVE, INACTIVE)).stream()
                .filter(r -> floorAreaId == null
                        || Activity.FACILITY.equals(r.content().scope())
                        || floorAreaId.equals(r.content().floorAreaId()))
                .toList();
    }

    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public SimpleRecord<Activity> declare(Target target, Activity input, Boolean groupActivity) {
        requireFloorManager();
        var activity = validated(input, groupActivity, user.staffId().orElseThrow());
        var row = records.create(
                ACTIVITIES,
                new NewSimpleRecord<>(
                        null,
                        new EntityRef("M04.ACTIVITY_TYPE", activity.typeId()),
                        ACTIVE,
                        activity,
                        time.today(),
                        null,
                        null,
                        user.accountId(),
                        false));
        log(row.id(), "ACTIVITY_CREATED", null, Map.of("name", activity.name()), null);
        return row;
    }

    /** FR-002b, FR-003: sửa áp cho buổi tạo sau; giảm số lượng dưới số đăng ký xét ở buổi (US2). */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public SimpleRecord<Activity> update(
            Target target, long id, String ifMatch, Activity input, Boolean groupActivity) {
        var before = editable(id);
        VersionEtag.requireMatch(ifMatch, before.version(), Map.of("id", id, "version", before.version()));
        var activity = validated(input, groupActivity, before.content().createdByStaffId());
        var change = records.change(
                ACTIVITIES, id, before.version(), new RecordChange<>(before.status(), activity, null, null, false));
        log(id, "ACTIVITY_UPDATED", Map.of("name", before.content().name()), Map.of("name", activity.name()), null);
        return change.after();
    }

    /** FR-004: hủy buổi chưa điểm danh từ ngày ngừng thuộc US1 (chưa có bảng buổi). */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void deactivate(Target target, long id, LocalDate from, Reason reason) {
        var before = editable(id);
        var day = from == null ? time.today() : from;
        if (day.isBefore(time.today())) throw new InvalidValueException("Ngày ngừng không sớm hơn hôm nay");
        records.change(
                ACTIVITIES,
                id,
                before.version(),
                new RecordChange<>(INACTIVE, before.content().deactivated(day), null, null, false));
        log(id, "ACTIVITY_DEACTIVATED", Map.of("status", before.status()), Map.of("from", day.toString()), reason);
    }

    /** FR-009: phiên bản mẫu mới từ ngày áp dụng ≥ hôm nay, bắt buộc lý do; hủy buổi không khớp thuộc US1. */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long changeTemplate(
            Target target, long id, LocalDate effectiveDate, TemplateVersion template, Reason reason) {
        editable(id);
        if (reason == null) throw new InvalidValueException("Đổi mẫu lặp cần lý do");
        if (effectiveDate == null || effectiveDate.isBefore(time.today()))
            throw new InvalidValueException("Ngày áp dụng không sớm hơn hôm nay");
        if (template == null
                || template.startTime() == null
                || template.endTime() == null
                || template.startDate() == null
                || ((template.daysOfWeek() == null || template.daysOfWeek().isEmpty())
                        == (template.intervalDays() == null)))
            throw new InvalidValueException(
                    "Mẫu cần ngày trong tuần hoặc chu kỳ ngày, giờ bắt đầu, kết thúc, ngày bắt đầu");
        var version = new TemplateVersion(
                template.daysOfWeek(),
                template.intervalDays(),
                template.startTime(),
                template.endTime(),
                template.startDate(),
                template.endDate(),
                reason.value(),
                user.staffId().orElseThrow());
        var row = records.create(
                TEMPLATE_VERSIONS,
                new NewSimpleRecord<>(
                        null,
                        new EntityRef(ENTITY, id),
                        ACTIVE,
                        version,
                        effectiveDate,
                        null,
                        id + ":" + effectiveDate,
                        user.accountId(),
                        false));
        log(id, "ACTIVITY_TEMPLATE_CHANGED", null, Map.of("effectiveDate", effectiveDate.toString()), reason);
        return row.id();
    }

    /** FR-010: đăng ký vào buổi đã tạo thuộc US2; mỗi người một dòng Hiệu lực. */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void addRegularParticipant(Target target, long id, Long residentId) {
        editable(id);
        if (residentId == null) throw new InvalidValueException("Thiếu người cao tuổi");
        String key = id + ":" + residentId;
        if (records.findByUniqueKey(REGULAR_PARTICIPANTS, key).isPresent())
            throw new BusinessException(
                    HttpStatus.CONFLICT, "STATE_CHANGED", "Người này đã trong danh sách thường xuyên");
        records.create(
                REGULAR_PARTICIPANTS,
                new NewSimpleRecord<>(
                        residentId,
                        new EntityRef(ENTITY, id),
                        ACTIVE,
                        new RegularParticipant(id, null),
                        time.today(),
                        null,
                        key,
                        user.accountId(),
                        false));
        log(id, "REGULAR_PARTICIPANT_ADDED", null, Map.of("residentId", residentId), null);
    }

    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void removeRegularParticipant(Target target, long id, Long residentId, Reason reason) {
        editable(id);
        if (residentId == null) throw new InvalidValueException("Thiếu người cao tuổi");
        var row = records.findByUniqueKey(REGULAR_PARTICIPANTS, id + ":" + residentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT, "STATE_CHANGED", "Người này không trong danh sách thường xuyên"));
        records.change(
                REGULAR_PARTICIPANTS,
                row.id(),
                row.version(),
                new RecordChange<>(
                        ENDED,
                        new RegularParticipant(id, reason == null ? null : reason.value()),
                        time.today(),
                        null,
                        false));
        log(id, "REGULAR_PARTICIPANT_REMOVED", Map.of("residentId", residentId), null, reason);
    }

    /** FR-002b: chỉ Trưởng tầng; hoạt động toàn viện do người tạo sửa (phần tầng của địa điểm buổi thuộc US1). */
    private SimpleRecord<Activity> editable(long id) {
        requireFloorManager();
        var row = records.lockForUpdate(ACTIVITIES, id);
        if (Activity.FACILITY.equals(row.content().scope())
                && !user.staffId()
                        .map(s -> s == row.content().createdByStaffId())
                        .orElse(false))
            throw new PermissionDeniedException("Hoạt động toàn viện do Trưởng tầng đã tạo sửa");
        return row;
    }

    private void requireFloorManager() {
        if (!user.roles().contains(Role.FLOOR_MANAGER))
            throw new PermissionDeniedException("Chỉ Trưởng tầng khai báo, sửa hoạt động");
    }

    /** FR-002: phạm vi, số lượng, thu phí, dấu hoạt động nhóm theo loại. */
    private Activity validated(Activity in, Boolean requestedGroup, long creator) {
        if (in.name() == null || in.name().isBlank()) throw new InvalidValueException("Thiếu tên hoạt động");
        var type = catalogs.requireActive(ACTIVITY_TYPES, in.typeId());
        if (Activity.FLOOR.equals(in.scope())) {
            if (in.floorAreaId() == null) throw new InvalidValueException("Hoạt động theo tầng cần tầng/khu vực");
        } else if (Activity.FACILITY.equals(in.scope())) {
            if (in.floorAreaId() != null) throw new InvalidValueException("Hoạt động toàn viện không gắn tầng");
        } else throw new InvalidValueException("Phạm vi phải là FLOOR hoặc FACILITY");
        if (in.maxParticipants() < 1) throw new InvalidValueException("Số lượng tối đa từ 1");
        if (in.charged() && in.serviceId() == null) throw new InvalidValueException("Hoạt động thu phí cần dịch vụ");
        boolean group = requestedGroup == null ? type.attributes().groupActivity() : requestedGroup;
        String reason = in.groupActivityChangeReason();
        if (group != type.attributes().groupActivity() && (reason == null || reason.isBlank()))
            throw new InvalidValueException("Đổi dấu hoạt động nhóm so với loại cần lý do");
        return new Activity(
                in.name().trim(),
                in.typeId(),
                in.scope(),
                in.floorAreaId(),
                in.defaultLocation(),
                in.defaultResponsibleStaffId(),
                in.durationMinutes(),
                in.maxParticipants(),
                in.targetGroup(),
                in.charged(),
                in.charged() ? in.serviceId() : null,
                group,
                group == type.attributes().groupActivity() ? null : reason.trim(),
                creator,
                null);
    }

    private void log(long id, String action, Map<String, Object> before, Map<String, Object> after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, reason != null))
                .entity(ENTITY, id)
                .before(before == null ? Map.of() : before)
                .after(after == null ? Map.of() : after)
                .reason(reason)
                .build());
    }
}

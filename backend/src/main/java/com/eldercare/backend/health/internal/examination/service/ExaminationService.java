package com.eldercare.backend.health.internal.examination.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản ghi khám, đánh giá, chẩn đoán, chỉ định, điều trị, kết quả (FR-072 → FR-074; BR-M06-05). Chẩn đoán, chỉ định, điều
 * trị tại viện chỉ lưu khi thao tác chuyên môn {@code (EXAM_TREATMENT, PROFESSIONAL)} thỏa ở lớp pháp lý của 002 (giấy
 * phép bác sĩ và phạm vi giấy phép cơ sở). Bản ghi không sửa, không xóa; đính chính theo 000 (FR-074).
 */
@Service
public class ExaminationService {
    public static final String RECORD_TYPE = "M06.EXAMINATION_RECORD";
    public static final SharedKind<Map> KIND =
            SharedKind.of(RECORD_TYPE, Map.class).notDeletable();
    private static final AuditAction RECORDED = new AuditAction("EXAMINATION_RECORDED", false);
    private static final Set<String> TYPES =
            Set.of("EXAMINATION", "ASSESSMENT", "DIAGNOSIS", "ORDER", "TREATMENT", "RESULT");
    /** Loại cần điều kiện chuyên môn khi thực hiện tại viện. */
    private static final Set<String> PROFESSIONAL = Set.of("DIAGNOSIS", "ORDER", "TREATMENT");
    /** Điều dưỡng ghi lại tại viện chỉ loại khám, kết quả (CHK019). */
    private static final Set<String> NURSE_AT_FACILITY = Set.of("EXAMINATION", "RESULT");

    private final RecordEventApi records;
    private final PermissionApi permissions;
    private final AccountApi accounts;
    private final AuditLogApi audit;
    private final ObjectProvider<CurrentUser> users;
    private final BusinessTime time;

    public ExaminationService(
            RecordEventApi records,
            PermissionApi permissions,
            AccountApi accounts,
            AuditLogApi audit,
            ObjectProvider<CurrentUser> users,
            BusinessTime time) {
        this.records = records;
        this.permissions = permissions;
        this.accounts = accounts;
        this.audit = audit;
        this.users = users;
        this.time = time;
    }

    public record ExaminationInput(
            String type,
            String content,
            boolean atFacility,
            String externalFacilityName,
            Long performerId,
            String externalPerformerName,
            Instant performedAt,
            List<Long> fileIds) {}

    public record ExaminationView(
            long id,
            String type,
            String content,
            boolean atFacility,
            String externalFacilityName,
            Long performerId,
            String externalPerformerName,
            Instant performedAt,
            List<Long> fileIds) {}

    @RequiresPermission(function = Function.EXAM_TREATMENT, operation = Operation.CREATE)
    @Transactional
    public ExaminationView record(Target target, long residentId, ExaminationInput input) {
        var user = users.getObject();
        var now = time.now();
        validate(input, now);
        Long performer = null;
        if (input.atFacility()) {
            performer = input.performerId();
            if (user.roles().contains(Role.DOCTOR)) {
                if (performer == null) performer = user.accountId();
                if (!accounts.hasRole(performer, Role.DOCTOR)) {
                    throw new InvalidValueException("Người thực hiện tại viện phải là Bác sĩ");
                }
            } else {
                if (!NURSE_AT_FACILITY.contains(input.type())) {
                    throw new PermissionDeniedException("Điều dưỡng không tự chẩn đoán, chỉ định hay điều trị");
                }
                if (performer == null || !accounts.hasRole(performer, Role.DOCTOR)) {
                    throw new InvalidValueException(
                            "Bản ghi tại viện do Điều dưỡng ghi cần người thực hiện là một Bác sĩ");
                }
            }
            if (PROFESSIONAL.contains(input.type())) requireProfessional(user, target);
        }
        Map<String, Object> content = new HashMap<>();
        content.put("type", input.type());
        content.put("content", input.content().strip());
        content.put("atFacility", input.atFacility());
        content.put(
                "externalFacilityName",
                input.atFacility() ? "" : input.externalFacilityName().strip());
        content.put("performerId", performer);
        content.put(
                "externalPerformerName",
                input.atFacility() ? "" : input.externalPerformerName().strip());
        content.put("fileIds", input.fileIds() == null ? List.of() : List.copyOf(input.fileIds()));
        var saved = records.add(
                KIND,
                new EntityRef("M01.RESIDENT", residentId),
                new NewRecordEvent<>(
                        content, null, user.accountId(), false, input.performedAt(), null, now, null, null));
        audit.write(AuditLogEntry.builder()
                .action(RECORDED)
                .entity(RECORD_TYPE, saved.id())
                .resident(residentId)
                .after(Map.of("type", input.type(), "atFacility", input.atFacility()))
                .build());
        return view(saved.id(), saved.content(), saved.occurredAt());
    }

    @RequiresPermission(function = Function.EXAM_TREATMENT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<ExaminationView> list(Target target, long residentId) {
        return records.list(KIND, new EntityRef("M01.RESIDENT", residentId)).stream()
                .map(event -> view(event.id(), event.content(), event.occurredAt()))
                .sorted(Comparator.comparing(ExaminationView::performedAt).reversed())
                .toList();
    }

    private void requireProfessional(CurrentUser user, Target target) {
        PermissionResult result = permissions.hasPermission(
                user, new PermissionKey(Function.EXAM_TREATMENT, Operation.PROFESSIONAL), target);
        if (result.allowed()) return;
        var layer = result.failedLayer();
        String code =
                layer.filter(l -> l == FailedLayer.LEGAL).isPresent() ? "LEGAL_CONDITION_NOT_MET" : "PERMISSION_DENIED";
        Map<String, Object> properties =
                layer.isPresent() ? Map.of("failedLayer", layer.get().name()) : Map.of();
        throw new PermissionDeniedException(code, "Thiếu điều kiện chuyên môn để thực hiện tại viện", properties);
    }

    private static void validate(ExaminationInput input, Instant now) {
        if (input.type() == null || !TYPES.contains(input.type())) {
            throw new InvalidValueException("Loại bản ghi không hợp lệ");
        }
        if (blank(input.content())) throw new InvalidValueException("Thiếu nội dung");
        if (input.performedAt() == null) throw new InvalidValueException("Thiếu thời điểm thực hiện");
        if (input.performedAt().isAfter(now)) {
            throw new InvalidValueException("Thời điểm thực hiện không được ở tương lai");
        }
        if (!input.atFacility() && (blank(input.externalFacilityName()) || blank(input.externalPerformerName()))) {
            throw new InvalidValueException("Cơ sở bên ngoài cần tên cơ sở và người thực hiện");
        }
    }

    private static ExaminationView view(long id, Map<?, ?> content, Instant performedAt) {
        return new ExaminationView(
                id,
                (String) content.get("type"),
                (String) content.get("content"),
                Boolean.TRUE.equals(content.get("atFacility")),
                blankToNull((String) content.get("externalFacilityName")),
                content.get("performerId") instanceof Number n ? n.longValue() : null,
                blankToNull((String) content.get("externalPerformerName")),
                performedAt,
                content.get("fileIds") instanceof List<?> l
                        ? l.stream().map(o -> ((Number) o).longValue()).toList()
                        : List.of());
    }

    private static String blankToNull(String value) {
        return blank(value) ? null : value;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}

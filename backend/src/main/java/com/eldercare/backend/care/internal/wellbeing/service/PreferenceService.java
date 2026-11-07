package com.eldercare.backend.care.internal.wellbeing.service;

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
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.wellbeing.domain.Preference;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.OpinionSourceApi;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T022 (FR-051, FR-052; research L-09; Q-363): Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc ghi sở thích trong phạm vi
 * (quyền dòng "Hoạt động, ngoài viện"); nguồn kiểm qua {@link OpinionSourceApi} của 001. Không gợi ý hoạt động (Q-307).
 */
@Service
public class PreferenceService {
    public static final String ENTITY = "M04.PREFERENCE";
    private static final Set<Role> RECORDERS = Set.of(Role.FLOOR_MANAGER, Role.NURSING_STAFF, Role.CAREGIVER);

    private final SimpleRecordApi records;
    private final CatalogApi catalogs;
    private final OpinionSourceApi opinions;
    private final CurrentUser user;
    private final BusinessTime time;
    private final AuditLogApi audit;

    public PreferenceService(
            SimpleRecordApi records,
            CatalogApi catalogs,
            OpinionSourceApi opinions,
            CurrentUser user,
            BusinessTime time,
            AuditLogApi audit) {
        this.records = records;
        this.catalogs = catalogs;
        this.opinions = opinions;
        this.user = user;
        this.time = time;
        this.audit = audit;
    }

    public static Target target(long residentId) {
        return new Target(ENTITY, null, residentId, null);
    }

    public Target targetOf(long id) {
        var row = records.require(Preference.KIND, id);
        return new Target(ENTITY, Long.toString(id), row.residentId(), null);
    }

    /** Nhóm và nguồn đã có mục hiện hành: 409 nêu mục cũ, trừ khi người ghi xác nhận thay (mục cũ thành Đã thay). */
    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long record(
            Target target,
            long residentId,
            Long interestGroupId,
            String description,
            String level,
            OpinionSource source,
            boolean confirmReplacement) {
        recorder();
        if (interestGroupId == null) throw new InvalidValueException("Cần nhóm sở thích");
        if (!Preference.LIKE.equals(level) && !Preference.DISLIKE.equals(level))
            throw new InvalidValueException("Mức ưa thích phải là Thích hoặc Không thích");
        catalogs.requireActive(ActivityKinds.INTEREST_GROUPS, interestGroupId);
        opinions.validate(residentId, source);
        var now = time.now();
        String key = Preference.currentKey(residentId, interestGroupId, source);
        var existing = records.findByUniqueKey(Preference.KIND, key);
        if (existing.isPresent()) {
            var old = existing.get().content();
            if (!confirmReplacement)
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "PREFERENCE_REPLACEMENT_CONFIRMATION_REQUIRED",
                        "Nhóm sở thích và nguồn này đã có mục hiện hành",
                        Map.of(
                                "unmetConditions",
                                List.of(new UnmetCondition(
                                        "PREFERENCE_REPLACEMENT_CONFIRMATION_REQUIRED",
                                        "Mục hiện hành: " + levelText(old.level())
                                                + (old.description() == null ? "" : " – " + old.description())))));
            var locked = records.lockForUpdate(Preference.KIND, existing.get().id());
            if (!Preference.CURRENT.equals(locked.status()))
                throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Mục hiện hành vừa thay đổi");
            records.change(
                    Preference.KIND,
                    locked.id(),
                    locked.version(),
                    new RecordChange<>(
                            Preference.REPLACED,
                            locked.content().ended(now, user.accountId(), null),
                            null,
                            null,
                            true));
        }
        var row = records.create(
                Preference.KIND,
                new NewSimpleRecord<>(
                        residentId,
                        null,
                        Preference.CURRENT,
                        new Preference(interestGroupId, description, level, source, now, null, null, null),
                        null,
                        null,
                        key,
                        user.accountId(),
                        false));
        log(row.id(), "PREFERENCE_RECORDED", Map.of("interestGroupId", interestGroupId, "level", level), null);
        return row.id();
    }

    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void remove(Target target, long id, Reason reason) {
        recorder();
        if (reason == null) throw new InvalidValueException("Bỏ sở thích cần lý do");
        var locked = records.lockForUpdate(Preference.KIND, id);
        if (!Preference.CURRENT.equals(locked.status()))
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Sở thích không còn hiện hành");
        records.change(
                Preference.KIND,
                id,
                locked.version(),
                new RecordChange<>(
                        Preference.REMOVED,
                        locked.content().ended(time.now(), user.accountId(), reason.value()),
                        null,
                        null,
                        true));
        log(id, "PREFERENCE_REMOVED", Map.of("status", Preference.REMOVED), reason);
    }

    /**
     * FR-052: mỗi mục "không thích" hiện hành khớp nhóm của loại hoạt động là một cảnh báo nêu nguồn; mục "thích" của chính
     * người cao tuổi không che các mục "không thích" của nguồn khác.
     */
    @Transactional(readOnly = true)
    public List<UnmetCondition> dislikeWarnings(long residentId, Collection<Long> interestGroupIds) {
        if (interestGroupIds.isEmpty()) return List.of();
        var warnings = new ArrayList<UnmetCondition>();
        for (var row : records.listByResident(Preference.KIND, residentId, Set.of(Preference.CURRENT))) {
            var p = row.content();
            if (!Preference.DISLIKE.equals(p.level()) || !interestGroupIds.contains(p.interestGroupId())) continue;
            String group = catalogs.require(ActivityKinds.INTEREST_GROUPS, p.interestGroupId())
                    .name();
            warnings.add(new UnmetCondition(
                    "DISLIKED_INTEREST", "Không thích " + group + " (nguồn: " + sourceText(p.source()) + ")"));
        }
        return warnings;
    }

    private static String sourceText(OpinionSource source) {
        if (source.type() == OpinionSourceType.FAMILY_MEMBER) return "người thân #" + source.familyMemberId();
        if (source.type() == OpinionSourceType.RESIDENT) return "người cao tuổi";
        return "nhân viên quan sát";
    }

    private static String levelText(String level) {
        return Preference.LIKE.equals(level) ? "Thích" : "Không thích";
    }

    private void recorder() {
        if (Collections.disjoint(user.roles(), RECORDERS))
            throw new PermissionDeniedException("Chỉ Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc ghi sở thích");
    }

    private void log(long id, String action, Map<String, Object> after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, reason != null))
                .entity(ENTITY, id)
                .before(Map.of())
                .after(after)
                .reason(reason)
                .build());
    }
}

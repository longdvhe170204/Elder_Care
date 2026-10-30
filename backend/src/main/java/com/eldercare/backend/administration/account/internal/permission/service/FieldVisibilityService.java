package com.eldercare.backend.administration.account.internal.permission.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.administration.account.api.spi.AssignmentApi;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.account.api.spi.FootnoteCondition;
import com.eldercare.backend.administration.account.internal.permission.domain.FieldVisibilityAttributes;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionScope;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Service;

/**
 * Mức xem theo nhóm thông tin (002 T023; research A-08; FR-022, FR-034, BR-M15-11): xét từng vai trò của tài khoản,
 * mức của vai trò (danh mục {@code M15.FIELD_VISIBILITY}, không có mục là {@code FULL}) chỉ tính khi đối tượng nằm trong phạm
 * vi của chính vai trò đó ({@code role.execution_scope}: toàn viện, hoặc {@code ScopeApi.inScope}); rồi lấy mức cao
 * nhất. Hai bảng là nhóm 1 cố định nạp bằng migration (không có API sửa) nên đọc một lần. Người thân xem nhóm sức
 * khỏe, thuốc, nguyện vọng chỉ khi điều kiện chú thích ¹ (bản đồng ý bao gồm người đó, 001) thỏa ở chính lần xem
 * này (FR-046, BR-M01-08; các dòng Phụ lục 27 của những nhóm này ở cột Người thân đều mang ¹); chưa có bản cài thì
 * không xem (002 T071).
 */
@Service
@Fallback
class FieldVisibilityService implements FieldVisibilityApi {

    private final CatalogApi catalog;
    private final RoleCatalog roleCatalog;
    private final ScopeApi scope;
    private final ObjectProvider<FootnoteCondition> conditions;
    private final BusinessTime time;
    private final ObjectProvider<AssignmentApi> assignments;
    private final ObjectProvider<DutyApi> duties;
    private final ObjectProvider<com.eldercare.backend.administration.account.api.spi.StaffAbsenceApi> absences;
    private static final Set<InfoGroup> CONSENT_GROUPS =
            Set.of(InfoGroup.HEALTH, InfoGroup.MEDICATION, InfoGroup.WISHES);

    private volatile Map<String, Map<InfoGroup, Visibility>> rules;
    private volatile Map<String, PermissionScope> executionScopes;

    FieldVisibilityService(
            CatalogApi catalog,
            RoleCatalog roleCatalog,
            ScopeApi scope,
            ObjectProvider<FootnoteCondition> conditions,
            BusinessTime time,
            ObjectProvider<AssignmentApi> assignments,
            ObjectProvider<DutyApi> duties,
            ObjectProvider<com.eldercare.backend.administration.account.api.spi.StaffAbsenceApi> absences) {
        this.catalog = catalog;
        this.roleCatalog = roleCatalog;
        this.scope = scope;
        this.conditions = conditions;
        this.time = time;
        this.assignments = assignments;
        this.duties = duties;
        this.absences = absences;
    }

    @Override
    public Visibility canView(CurrentUser user, InfoGroup group, Target target) {
        if (user.isScheduler()) {
            return Visibility.FULL;
        }
        if (group == InfoGroup.STAFF_CONTACT
                || group == InfoGroup.STAFF_RESTRICTED
                || group == InfoGroup.STAFF_REASON) {
            return staffVisibility(user, group, target);
        }
        load();
        Visibility best = Visibility.NONE;
        for (Role role : user.roles()) {
            PermissionScope roleScope = executionScopes.get(role.code());
            Visibility level = rules.getOrDefault(role.code(), Map.of()).getOrDefault(group, Visibility.FULL);
            if (roleScope == null || level == Visibility.NONE || rank(level) <= rank(best)) {
                continue;
            }
            boolean inRoleScope = roleScope == PermissionScope.FACILITY
                    || (!target.equals(Target.UNSPECIFIED) && scope.inScope(user, target));
            if (inRoleScope
                    && (role != Role.FAMILY_MEMBER || !CONSENT_GROUPS.contains(group) || consented(user, target))) {
                best = level;
            }
        }
        return best;
    }

    /** FR-052: liên hệ nhân viên không kế thừa quyền mặc định của CONTACT. */
    private Visibility staffVisibility(CurrentUser user, InfoGroup group, Target target) {
        if (user.roles().contains(Role.NURSING_HOME_MANAGER)) return Visibility.FULL;
        if (group == InfoGroup.STAFF_REASON) {
            if (!"M09.STAFF_ABSENCE".equals(target.type()) || target.id() == null) return Visibility.NONE;
            var api = absences.getIfAvailable();
            if (api == null) return Visibility.NONE;
            try {
                return api.canViewReason(user, Long.parseLong(target.id())) ? Visibility.FULL : Visibility.NONE;
            } catch (NumberFormatException ignored) {
                return Visibility.NONE;
            }
        }
        if (group != InfoGroup.STAFF_CONTACT
                || !user.roles().contains(Role.FLOOR_MANAGER)
                || user.staffId().isEmpty()
                || !"M09.STAFF".equals(target.type())
                || target.id() == null) return Visibility.NONE;
        var assignmentApi = assignments.getIfAvailable();
        var dutyApi = duties.getIfAvailable();
        if (assignmentApi == null || dutyApi == null) return Visibility.NONE;
        long staffId;
        try {
            staffId = Long.parseLong(target.id());
        } catch (NumberFormatException e) {
            return Visibility.NONE;
        }
        var start = time.startOfDay(time.today().withDayOfMonth(1));
        var end = time.startOfDay(time.today().withDayOfMonth(1).plusMonths(1));
        boolean allowed = assignmentApi.history(staffId, start, end).stream()
                .filter(a -> a.kind() == AssignmentKind.SHIFT_MEMBER || a.kind() == AssignmentKind.SHIFT_LEAD)
                .filter(a ->
                        a.floorAreaId() != null && a.from().isBefore(end) && (a.to() == null || a.to().isAfter(start)))
                .anyMatch(a -> dutyApi.assignedFloorManager(a.floorAreaId(), time.now())
                        .equals(user.staffId()));
        return allowed ? Visibility.FULL : Visibility.NONE;
    }

    /**
     * Mức cao nhất của các vai trò, bỏ điều kiện phạm vi (016 Q-196). Nhóm cần bản đồng ý của người thân không xét được
     * khi không có đối tượng nên vai trò Người thân không góp mức cho các nhóm đó.
     */
    @Override
    public Visibility canViewByRole(CurrentUser user, InfoGroup group) {
        if (user.isScheduler()) {
            return Visibility.FULL;
        }
        load();
        Visibility best = Visibility.NONE;
        for (Role role : user.roles()) {
            if (executionScopes.get(role.code()) == null
                    || (role == Role.FAMILY_MEMBER && CONSENT_GROUPS.contains(group))) {
                continue;
            }
            Visibility level = rules.getOrDefault(role.code(), Map.of()).getOrDefault(group, Visibility.FULL);
            if (rank(level) > rank(best)) {
                best = level;
            }
        }
        return best;
    }

    private boolean consented(CurrentUser user, Target target) {
        return conditions
                .orderedStream()
                .filter(c -> c.footnoteCode().equals("1"))
                .anyMatch(c -> c.isSatisfied(user, target, time.now()));
    }

    private static int rank(Visibility v) {
        return switch (v) {
            case NONE -> 0;
            case PRESENCE_ONLY -> 1;
            case FULL -> 2;
        };
    }

    private void load() {
        if (rules != null) {
            return;
        }
        // Mã mục <vai trò>:<nhóm thông tin> (data-model 002 mục 0.1)
        Map<String, Map<InfoGroup, Visibility>> loaded = new HashMap<>();
        for (CatalogItem<FieldVisibilityAttributes> rule : catalog.list(FieldVisibilityAttributes.KIND, null, false)) {
            String[] key = rule.code().split(":", 2);
            loaded.computeIfAbsent(key[0], r -> new HashMap<>())
                    .put(InfoGroup.valueOf(key[1]), rule.attributes().visibility());
        }
        executionScopes = Map.copyOf(roleCatalog.executionScopes());
        rules = Map.copyOf(loaded);
    }
}

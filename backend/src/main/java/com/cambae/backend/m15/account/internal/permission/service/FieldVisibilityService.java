package com.cambae.backend.m15.account.internal.permission.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.FieldVisibilityApi;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.dto.Visibility;
import com.cambae.backend.m15.account.api.spi.AssignmentApi;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.DutyApi;
import com.cambae.backend.m15.account.api.spi.FootnoteCondition;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionScope;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Fallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Mức xem theo nhóm thông tin (002 T023; research A-08; FR-022, FR-034, BR-M15-11): xét từng vai trò của tài khoản,
 * mức của vai trò ({@code field_visibility_rule}, không có dòng là {@code FULL}) chỉ tính khi đối tượng nằm trong phạm
 * vi của chính vai trò đó ({@code role.execution_scope}: toàn viện, hoặc {@code ScopeApi.inScope}); rồi lấy mức cao
 * nhất. Hai bảng là nhóm 1 cố định nạp bằng migration (không có API sửa) nên đọc một lần. Người thân xem nhóm sức
 * khỏe, thuốc, nguyện vọng chỉ khi điều kiện chú thích ¹ (bản đồng ý bao gồm người đó, 001) thỏa ở chính lần xem
 * này (FR-046, BR-M01-08; các dòng Phụ lục 27 của những nhóm này ở cột Người thân đều mang ¹); chưa có bản cài thì
 * không xem (002 T071).
 */
@Service
@Fallback
class FieldVisibilityService implements FieldVisibilityApi {

    private final JdbcTemplate jdbc;
    private final ScopeApi scope;
    private final ObjectProvider<FootnoteCondition> conditions;
    private final BusinessTime time;
    private final ObjectProvider<AssignmentApi> assignments;
    private final ObjectProvider<DutyApi> duties;
    private static final Set<InfoGroup> CONSENT_GROUPS =
            Set.of(InfoGroup.HEALTH, InfoGroup.MEDICATION, InfoGroup.WISHES);

    private volatile Map<String, Map<InfoGroup, Visibility>> rules;
    private volatile Map<String, PermissionScope> executionScopes;

    FieldVisibilityService(
            JdbcTemplate jdbc,
            ScopeApi scope,
            ObjectProvider<FootnoteCondition> conditions,
            BusinessTime time,
            ObjectProvider<AssignmentApi> assignments,
            ObjectProvider<DutyApi> duties) {
        this.jdbc = jdbc;
        this.scope = scope;
        this.conditions = conditions;
        this.time = time;
        this.assignments = assignments;
        this.duties = duties;
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
        // STAFF_REASON chưa có luồng đọc ở Phase 2; mở theo người ghi/tầng khi triển khai FR-020.
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
        Map<String, Map<InfoGroup, Visibility>> loaded = new HashMap<>();
        jdbc.query("SELECT role, info_group, visibility FROM field_visibility_rule", rs -> {
            loaded.computeIfAbsent(rs.getString(1), r -> new HashMap<>())
                    .put(InfoGroup.valueOf(rs.getString(2)), Visibility.valueOf(rs.getString(3)));
        });
        Map<String, PermissionScope> scopes = new HashMap<>();
        jdbc.query("SELECT code, execution_scope FROM role", rs -> {
            scopes.put(rs.getString(1), PermissionScope.valueOf(rs.getString(2)));
        });
        executionScopes = Map.copyOf(scopes);
        rules = Map.copyOf(loaded);
    }
}

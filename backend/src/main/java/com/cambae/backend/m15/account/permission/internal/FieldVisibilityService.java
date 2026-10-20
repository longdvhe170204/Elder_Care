package com.cambae.backend.m15.account.permission.internal;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.FieldVisibilityApi;
import com.cambae.backend.m15.account.api.FootnoteCondition;
import com.cambae.backend.m15.account.api.InfoGroup;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.account.api.Visibility;
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
    private static final Set<InfoGroup> CONSENT_GROUPS =
            Set.of(InfoGroup.HEALTH, InfoGroup.MEDICATION, InfoGroup.WISHES);

    private volatile Map<String, Map<InfoGroup, Visibility>> rules;
    private volatile Map<String, PermissionScope> executionScopes;

    FieldVisibilityService(
            JdbcTemplate jdbc, ScopeApi scope, ObjectProvider<FootnoteCondition> conditions, BusinessTime time) {
        this.jdbc = jdbc;
        this.scope = scope;
        this.conditions = conditions;
        this.time = time;
    }

    @Override
    public Visibility canView(CurrentUser user, InfoGroup group, Target target) {
        if (user.isScheduler()) {
            return Visibility.FULL;
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

package com.cambae.backend.support;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.ApproverApi;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.FieldVisibilityApi;
import com.cambae.backend.m15.account.api.InfoGroup;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.PermissionKey;
import com.cambae.backend.m15.account.api.PermissionResult;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.account.api.Visibility;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bản giả quyền của 002 cho test của 000 (000 T025): bảng vai trò → quyền tối thiểu. Mặc định mọi đối tượng trong phạm
 * vi và chỉ Quản lý viện xem đủ mọi nhóm thông tin; test đặt thêm mức xem theo vai trò, đối tượng ngoài phạm vi (000
 * T062, T063). Thay bằng bản thật khi làm 002.
 */
public class FakePermissions implements PermissionApi, ScopeApi, FieldVisibilityApi, ApproverApi, AccountApi {

    private final Map<Role, Set<PermissionKey>> grants = new EnumMap<>(Role.class);
    private final Map<Long, CurrentUser> accounts = new LinkedHashMap<>();
    private final Map<Long, Long> shiftLeads = new HashMap<>();
    private final Map<Long, Long> floorManagers = new HashMap<>();
    private final Map<Role, Map<InfoGroup, Visibility>> visibility = new EnumMap<>(Role.class);
    private final Set<String> outOfScope = new HashSet<>();

    public static CurrentUser user(long accountId, Role... roles) {
        Set<Role> set = roles.length == 0 ? EnumSet.noneOf(Role.class) : EnumSet.of(roles[0], roles);
        boolean scheduler = set.contains(Role.SCHEDULER);
        return new FakeUser(accountId, Set.copyOf(set), scheduler);
    }

    public FakePermissions grant(Role role, PermissionKey key) {
        grants.computeIfAbsent(role, r -> new HashSet<>()).add(key);
        return this;
    }

    public FakePermissions account(CurrentUser user) {
        accounts.put(user.accountId(), user);
        return this;
    }

    public FakePermissions shiftLead(long floorAreaId, long accountId) {
        shiftLeads.put(floorAreaId, accountId);
        return this;
    }

    public FakePermissions floorManager(long floorAreaId, long accountId) {
        floorManagers.put(floorAreaId, accountId);
        return this;
    }

    public FakePermissions visibility(Role role, InfoGroup group, Visibility level) {
        visibility.computeIfAbsent(role, r -> new EnumMap<>(InfoGroup.class)).put(group, level);
        return this;
    }

    public FakePermissions outOfScope(String type, String id) {
        outOfScope.add(type + "/" + id);
        return this;
    }

    @Override
    public PermissionResult hasPermission(CurrentUser user, PermissionKey key, Target target) {
        return new PermissionResult(user.roles().stream()
                .anyMatch(role -> grants.getOrDefault(role, Set.of()).contains(key)));
    }

    @Override
    public boolean inScope(CurrentUser user, Target target) {
        return !outOfScope.contains(target.type() + "/" + target.id());
    }

    @Override
    public Optional<Long> shiftLead(long floorAreaId, Instant at) {
        return Optional.ofNullable(shiftLeads.get(floorAreaId));
    }

    @Override
    public Optional<Long> assignedFloorManager(long floorAreaId) {
        return Optional.ofNullable(floorManagers.get(floorAreaId));
    }

    @Override
    public Visibility canView(CurrentUser user, InfoGroup group, Target target) {
        if (user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            return Visibility.FULL;
        }
        // Tài khoản nhiều vai trò lấy mức cao nhất (FULL đứng đầu enum)
        return user.roles().stream()
                .map(role -> {
                    Map<InfoGroup, Visibility> byGroup = visibility.get(role);
                    return byGroup == null || group == null
                            ? Visibility.NONE
                            : byGroup.getOrDefault(group, Visibility.NONE);
                })
                .min(Comparator.naturalOrder())
                .orElse(Visibility.NONE);
    }

    @Override
    public boolean hasAnyApprover(PermissionKey key, Target target) {
        return !approvers(key, target).isEmpty();
    }

    @Override
    public List<Long> approvers(PermissionKey key, Target target) {
        return accounts.values().stream()
                .filter(u -> hasPermission(u, key, target).allowed())
                .map(CurrentUser::accountId)
                .toList();
    }

    @Override
    public boolean hasRole(long accountId, Role... roles) {
        CurrentUser user = accounts.get(accountId);
        return user != null && Set.of(roles).stream().anyMatch(user.roles()::contains);
    }

    private record FakeUser(long accountId, Set<Role> roles, boolean isScheduler) implements CurrentUser {}
}

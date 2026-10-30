package com.eldercare.backend.support;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.ApproverApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.AccessChannel;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.AccountView;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.MetricGroupRestrictions;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.ReviewRecipient;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.administration.auditlog.api.Basis;
import com.eldercare.backend.common.Role;
import java.time.Instant;
import java.util.ArrayList;
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
import java.util.stream.Collectors;

/**
 * Bản giả quyền của 002 cho test của 000 (000 T025): bảng vai trò → quyền tối thiểu. Mặc định mọi đối tượng trong phạm
 * vi và chỉ Quản lý viện xem đủ mọi nhóm thông tin; test đặt thêm mức xem theo vai trò, đối tượng ngoài phạm vi (000
 * T062, T063). Thay bằng bản thật khi làm 002.
 */
public class FakePermissions implements PermissionApi, ScopeApi, FieldVisibilityApi, ApproverApi, AccountApi {

    /** Bản giả không có hồ sơ: họ tên rỗng như 000 trước 002 T032a. */
    @Override
    public Optional<String> fullName(long accountId) {
        return Optional.empty();
    }

    private final Map<Role, Set<PermissionKey>> grants = new EnumMap<>(Role.class);
    private final Map<Role, Set<PermissionKey>> assignedGrants = new EnumMap<>(Role.class);
    private final Map<Long, CurrentUser> accounts = new LinkedHashMap<>();
    private final Map<Long, Long> shiftLeads = new HashMap<>();
    private final Map<Long, Long> floorManagers = new HashMap<>();
    private final Map<Role, Map<InfoGroup, Visibility>> visibility = new EnumMap<>(Role.class);
    private final Set<String> outOfScope = new HashSet<>();
    private final List<String> sideEffects = new ArrayList<>();
    private MetricGroupRestrictions metricGroupRestrictions = MetricGroupRestrictions.NONE;

    /**
     * Người dùng giả: loại tài khoản suy từ vai trò (Người thân, Người cao tuổi, còn lại nhân viên); id hồ sơ liên kết bằng
     * id tài khoản; kênh web.
     */
    public static CurrentUser user(long accountId, Role... roles) {
        Set<Role> set = roles.length == 0 ? EnumSet.noneOf(Role.class) : EnumSet.of(roles[0], roles);
        boolean scheduler = set.contains(Role.SCHEDULER);
        AccountType type = set.contains(Role.FAMILY_MEMBER)
                ? AccountType.FAMILY_MEMBER
                : set.contains(Role.RESIDENT) ? AccountType.RESIDENT : AccountType.STAFF;
        return new FakeUser(
                accountId,
                type,
                type == AccountType.STAFF ? Optional.of(accountId) : Optional.empty(),
                type == AccountType.FAMILY_MEMBER ? Optional.of(accountId) : Optional.empty(),
                type == AccountType.RESIDENT ? Optional.of(accountId) : Optional.empty(),
                Set.copyOf(set),
                scheduler,
                "test-device-" + accountId,
                AccessChannel.WEB);
    }

    /** Quyền toàn viện (ô X, C, T, D phạm vi toàn viện của Phụ lục 27). */
    public FakePermissions grant(Role role, PermissionKey key) {
        grants.computeIfAbsent(role, r -> new HashSet<>()).add(key);
        return this;
    }

    /** Quyền theo phân công (ô P; T của Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc): không đạt với {@code Target.UNSPECIFIED}. */
    public FakePermissions grantAssigned(Role role, PermissionKey key) {
        assignedGrants.computeIfAbsent(role, r -> new HashSet<>()).add(key);
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
        boolean specific = !Target.UNSPECIFIED.equals(target);
        // Như bản thật: lớp phạm vi nằm trong hasPermission (đối tượng đặt ngoài phạm vi qua outOfScope)
        if (specific && outOfScope.contains(target.type() + "/" + target.id())) {
            return PermissionResult.denied(FailedLayer.SCOPE);
        }
        boolean allowed = user.roles().stream()
                .anyMatch(role -> grants.getOrDefault(role, Set.of()).contains(key)
                        || (specific
                                && assignedGrants.getOrDefault(role, Set.of()).contains(key)));
        return allowed ? PermissionResult.allowed(PermissionBasis.ROLE) : PermissionResult.denied(FailedLayer.ROLE);
    }

    @Override
    public PermissionResult hasPermissionAt(CurrentUser user, PermissionKey key, Target target, Instant at) {
        return hasPermission(user, key, target);
    }

    @Override
    public Set<PermissionKey> permissions(CurrentUser user) {
        Set<PermissionKey> keys = new HashSet<>();
        for (Role role : user.roles()) {
            keys.addAll(grants.getOrDefault(role, Set.of()));
            keys.addAll(assignedGrants.getOrDefault(role, Set.of()));
        }
        return keys;
    }

    /** Thu hẹp chi tiết theo nhóm chỉ tiêu, kênh (016 FR-010a) trả cho mọi người dùng. */
    public FakePermissions metricGroupRestrictions(MetricGroupRestrictions restrictions) {
        this.metricGroupRestrictions = restrictions;
        return this;
    }

    @Override
    public MetricGroupRestrictions metricGroupRestrictions(CurrentUser user) {
        return metricGroupRestrictions;
    }

    @Override
    public Set<String> allowedCommands(CurrentUser user, Target target, Map<String, PermissionKey> commands) {
        return commands.entrySet().stream()
                .filter(e -> hasPermission(user, e.getValue(), target).allowed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean inScope(CurrentUser user, Target target) {
        return !outOfScope.contains(target.type() + "/" + target.id());
    }

    /** Quyền toàn viện thì cả viện; quyền theo phân công thì bộ lọc rỗng (test đặt đối tượng qua {@link #outOfScope}). */
    @Override
    public ScopeFilter filter(CurrentUser user, PermissionKey key) {
        return hasPermission(user, key, Target.UNSPECIFIED).allowed()
                ? ScopeFilter.FACILITY
                : new ScopeFilter(false, Set.of(), Set.of(), Set.of());
    }

    @Override
    public ScopeFilter historyFilter(CurrentUser user, PermissionKey key, Instant from, Instant to) {
        return filter(user, key);
    }

    @Override
    public Optional<Long> shiftLead(long floorAreaId, Instant at) {
        return Optional.ofNullable(shiftLeads.get(floorAreaId));
    }

    @Override
    public Optional<Long> assignedFloorManager(long floorAreaId) {
        return Optional.ofNullable(floorManagers.get(floorAreaId));
    }

    /** Trưởng tầng của tầng; không có thì người phụ trách ca; bản ghi không gắn tầng thì Quản lý viện (CHK056). */
    @Override
    public ReviewRecipient reviewDecider(Optional<Long> floorAreaId) {
        Optional<Long> decider =
                floorAreaId.flatMap(id -> assignedFloorManager(id).or(() -> Optional.ofNullable(shiftLeads.get(id))));
        return new ReviewRecipient(decider.map(Set::of)
                .orElseGet(() -> accounts.values().stream()
                        .filter(u -> u.roles().contains(Role.NURSING_HOME_MANAGER))
                        .map(CurrentUser::accountId)
                        .collect(Collectors.toSet())));
    }

    /** Bản giả không xét phạm vi khi xem theo nhóm nên mức theo vai trò như {@link #canView}. */
    @Override
    public Visibility canViewByRole(CurrentUser user, InfoGroup group) {
        return canView(user, group, Target.UNSPECIFIED);
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

    /** Tác động kèm lệnh đã gọi, dạng {@code "lockOnResignation:<staffId>"}, để test kiểm. */
    public List<String> sideEffects() {
        return List.copyOf(sideEffects);
    }

    @Override
    public void lockOnResignation(long staffId, Basis basis) {
        sideEffects.add("lockOnResignation:" + staffId);
    }

    @Override
    public void recheckFamilyMember(long familyMemberId, Basis basis) {
        sideEffects.add("recheckFamilyMember:" + familyMemberId);
    }

    @Override
    public void lockResidentOnFinalStatus(long residentId, Basis basis) {
        sideEffects.add("lockResidentOnFinalStatus:" + residentId);
    }

    @Override
    public List<AccountView> activeWithRole(Role role) {
        return accounts.values().stream()
                .filter(u -> u.roles().contains(role))
                .map(u -> new AccountView(
                        u.accountId(),
                        u.type(),
                        AccountStatus.ACTIVE,
                        u.roles(),
                        u.staffId().orElse(null)))
                .toList();
    }

    @Override
    public Optional<AccountView> byId(long accountId) {
        return find(u -> u.accountId() == accountId);
    }

    @Override
    public Optional<AccountView> byStaff(long staffId) {
        return find(u -> u.staffId().equals(Optional.of(staffId)));
    }

    @Override
    public Optional<AccountView> byFamilyMember(long familyMemberId) {
        return find(u -> u.familyMemberId().equals(Optional.of(familyMemberId)));
    }

    @Override
    public Optional<AccountView> byResident(long residentId) {
        return find(u -> u.residentId().equals(Optional.of(residentId)));
    }

    private Optional<AccountView> find(java.util.function.Predicate<CurrentUser> match) {
        return accounts.values().stream()
                .filter(match)
                .findFirst()
                .map(u -> new AccountView(
                        u.accountId(),
                        u.type(),
                        AccountStatus.ACTIVE,
                        u.roles(),
                        u.staffId().orElse(null)));
    }

    @Override
    public boolean hasRole(long accountId, Role... roles) {
        CurrentUser user = accounts.get(accountId);
        return user != null && Set.of(roles).stream().anyMatch(user.roles()::contains);
    }

    private record FakeUser(
            long accountId,
            AccountType type,
            Optional<Long> staffId,
            Optional<Long> familyMemberId,
            Optional<Long> residentId,
            Set<Role> roles,
            boolean isScheduler,
            String deviceId,
            AccessChannel channel)
            implements CurrentUser {}
}

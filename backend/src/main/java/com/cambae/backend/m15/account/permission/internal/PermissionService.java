package com.cambae.backend.m15.account.permission.internal;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.FailedLayer;
import com.cambae.backend.m15.account.api.FootnoteCondition;
import com.cambae.backend.m15.account.api.Function;
import com.cambae.backend.m15.account.api.InvalidPermissionDataException;
import com.cambae.backend.m15.account.api.Operation;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.PermissionBasis;
import com.cambae.backend.m15.account.api.PermissionKey;
import com.cambae.backend.m15.account.api.PermissionResult;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.account.scope.internal.PermissionDataErrors;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Service;

/**
 * Điểm kiểm quyền duy nhất (002 T022; research A-10; BR-M15-01, FR-041 → FR-044a), theo thứ tự:
 *
 * <ol>
 *   <li>vai trò ({@link RolePermissionCalculator}) cùng chú thích ({@link FootnoteRules}): giữ các phương án của khóa
 *       có mọi chú thích thỏa; không còn phương án thì lớp {@code ROLE};
 *   <li>nhiệm vụ ({@link DutyPermissions}, T053) khi vai trò hoặc phạm vi không thỏa; căn cứ là nhiệm vụ;
 *   <li>phạm vi: phương án toàn viện thì qua; theo phân công, theo quan hệ thì đối tượng phải cụ thể và
 *       {@code ScopeApi.inScope}; trượt thì lớp {@code SCOPE}. {@code (INCIDENT_EMERGENCY, CREATE)} trên một người cao
 *       tuổi được miễn lớp này (FR-044a); căn cứ là ngoài phạm vi khi người ghi không xem được hồ sơ người đó
 *       ({@code (RESIDENT_PROFILE, VIEW)} theo vai trò ∩ phạm vi; chủ dự án chọn 2026-10-20, US1 #9);
 *   <li>pháp lý: thao tác {@code PROFESSIONAL} bị từ chối lớp {@code LEGAL} cho tới khi T064 (US5) nối kiểm giấy phép.
 * </ol>
 *
 * Bộ lập lịch được phép, căn cứ Bộ lập lịch (FR-044). Module nguồn ném {@link InvalidPermissionDataException} thì lớp
 * {@code SOURCE_DATA}, trừ khóa sự cố như FR-044a (FR-041b). {@code @Fallback}: test của 000 tự khai bản giả quyền thì
 * bản giả được dùng.
 */
@Service
@Fallback
class PermissionService implements PermissionApi {

    private static final PermissionKey INCIDENT_CREATE =
            new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.CREATE);
    private static final PermissionKey PROFILE_VIEW = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);

    private final RolePermissionCalculator roles;
    private final ScopeApi scope;
    private final ObjectProvider<FootnoteCondition> conditions;
    private final PermissionDataErrors dataErrors;
    private final DutyPermissions duties;
    private final BusinessTime time;

    PermissionService(
            RolePermissionCalculator roles,
            ScopeApi scope,
            ObjectProvider<FootnoteCondition> conditions,
            PermissionDataErrors dataErrors,
            DutyPermissions duties,
            BusinessTime time) {
        this.roles = roles;
        this.scope = scope;
        this.conditions = conditions;
        this.dataErrors = dataErrors;
        this.duties = duties;
        this.time = time;
    }

    @Override
    public PermissionResult hasPermission(CurrentUser user, PermissionKey key, Target target) {
        if (user.isScheduler()) {
            return PermissionResult.allowed(PermissionBasis.SCHEDULER);
        }
        return evaluate(user, key, target, time.now(), roles.current(user.accountId()));
    }

    /** Quyền vai trò, nhiệm vụ, phạm vi tại {@code at} (FR-041a); trạng thái tài khoản tại {@code at} do người gọi kiểm. */
    @Override
    public PermissionResult hasPermissionAt(CurrentUser user, PermissionKey key, Target target, Instant at) {
        if (user.isScheduler()) {
            return PermissionResult.allowed(PermissionBasis.SCHEDULER);
        }
        return evaluate(user, key, target, at, roles.at(user.accountId(), at));
    }

    /** Khóa có ít nhất một phương án không vướng chú thích nhiệm vụ hay chú thích điều kiện chưa có bản cài (FR-042). */
    @Override
    public Set<PermissionKey> permissions(CurrentUser user) {
        if (user.isScheduler()) {
            return Set.of();
        }
        Set<String> registered = registeredConditions().keySet();
        return roles.current(user.accountId()).entrySet().stream()
                .filter(e -> e.getValue().stream()
                        .anyMatch(g -> !FootnoteRules.blockedAtRoleLayer(g.footnotes(), registered)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> allowedCommands(CurrentUser user, Target target, Map<String, PermissionKey> commands) {
        Set<String> allowed = new LinkedHashSet<>();
        commands.forEach((name, key) -> {
            if (hasPermission(user, key, target).allowed()) {
                allowed.add(name);
            }
        });
        return allowed;
    }

    private PermissionResult evaluate(
            CurrentUser user, PermissionKey key, Target target, Instant at, Map<PermissionKey, Set<RoleGrant>> grants) {
        try {
            // 1. Vai trò cùng chú thích
            Map<String, FootnoteCondition> registered = registeredConditions();
            List<RoleGrant> passing = grants.getOrDefault(key, Set.of()).stream()
                    .filter(g -> g.footnotes().stream().allMatch(c -> satisfied(c, registered, user, target, at)))
                    .toList();
            // 2. Nhiệm vụ khi vai trò hoặc phạm vi không thỏa; giới hạn của nhiệm vụ là phạm vi của quyền đó
            if (passing.isEmpty()) {
                return byDuty(user, key, target, at, FailedLayer.ROLE);
            }
            // 3. Phạm vi; ghi sự cố trên một người cao tuổi được miễn (FR-044a)
            if (key.equals(INCIDENT_CREATE) && target.residentId() != null) {
                return evaluate(user, PROFILE_VIEW, target, at, grants).allowed()
                        ? PermissionResult.allowed(PermissionBasis.ROLE)
                        : PermissionResult.allowed(PermissionBasis.OUT_OF_SCOPE);
            }
            if (!inScope(passing, user, target, at)) {
                return byDuty(user, key, target, at, FailedLayer.SCOPE);
            }
        } catch (InvalidPermissionDataException e) {
            dataErrors.record(e);
            return key.equals(INCIDENT_CREATE) && target.residentId() != null
                    ? PermissionResult.allowed(PermissionBasis.OUT_OF_SCOPE)
                    : PermissionResult.denied(FailedLayer.SOURCE_DATA);
        }
        // 4. Pháp lý: T064 nối kiểm giấy phép
        if (key.operation() == Operation.PROFESSIONAL) {
            return PermissionResult.denied(FailedLayer.LEGAL);
        }
        return PermissionResult.allowed(PermissionBasis.ROLE);
    }

    /** Quyền theo nhiệm vụ (FR-019a → g); không nhiệm vụ nào thỏa thì từ chối ở lớp đã trượt. */
    private PermissionResult byDuty(
            CurrentUser user, PermissionKey key, Target target, Instant at, FailedLayer failed) {
        return duties.grant(user, key, target, at)
                .map(d -> PermissionResult.allowed(PermissionBasis.duty(d)))
                .orElseGet(() -> PermissionResult.denied(failed));
    }

    private boolean inScope(List<RoleGrant> passing, CurrentUser user, Target target, Instant at) {
        if (passing.stream().anyMatch(g -> g.scope() == PermissionScope.FACILITY)) {
            return true;
        }
        if (target.equals(Target.UNSPECIFIED)) {
            return false;
        }
        boolean in = scope.inScope(user, target, at);
        // Đọc được dữ liệu nguồn của người cao tuổi: lỗi đang mở của bản ghi đó coi là đã khắc phục (CHK059)
        if (target.residentId() != null) {
            dataErrors.markFixed(target.residentId());
        }
        return in;
    }

    private static boolean satisfied(
            String footnote, Map<String, FootnoteCondition> registered, CurrentUser user, Target target, Instant at) {
        if (FootnoteRules.DUTIES.contains(footnote)) {
            return false;
        }
        if (FootnoteRules.CONDITIONS.contains(footnote)) {
            FootnoteCondition condition = registered.get(footnote);
            return condition != null && condition.isSatisfied(user, target, at);
        }
        return true;
    }

    private Map<String, FootnoteCondition> registeredConditions() {
        return conditions
                .orderedStream()
                .collect(Collectors.toUnmodifiableMap(FootnoteCondition::footnoteCode, c -> c));
    }
}

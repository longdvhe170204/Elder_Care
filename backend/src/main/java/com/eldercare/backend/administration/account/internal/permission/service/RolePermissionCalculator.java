package com.eldercare.backend.administration.account.internal.permission.service;

import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.internal.permission.domain.RoleGrant;
import com.eldercare.backend.administration.account.internal.permission.domain.RolePermissionRules;
import com.eldercare.backend.administration.account.internal.permission.persistence.PermissionReads;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Quyền vai trò của một tài khoản (002 research A-05; FR-025, FR-026a, BR-M15-18), theo thứ tự:
 *
 * <ol>
 *   <li>với từng vai trò đang gán: ma trận trừ thu hẹp theo vai trò;
 *   <li>hợp các vai trò; cùng khóa thì gộp phương án (toàn viện thắng);
 *   <li>trừ thu hẹp theo tài khoản;
 *   <li>cộng quyền gán thêm (chỉ Duyệt kế hoạch chăm sóc, theo phân công như Điều dưỡng, FR-024), chỉ khi tài khoản
 *       đang có vai trò Điều dưỡng.
 * </ol>
 *
 * <p>Thứ tự ở {@link RolePermissionRules}; dòng đọc ở {@link PermissionReads}, tính được tại thời điểm quá khứ. Điều
 * kiện chú thích, nhiệm vụ, phạm vi dữ liệu, pháp lý tính ở {@code PermissionApi} (research A-10).
 */
@Component
public class RolePermissionCalculator {

    private final PermissionReads reads;
    private final RoleCatalog roles;
    private final BusinessTime time;
    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    RolePermissionCalculator(PermissionReads reads, RoleCatalog roles, BusinessTime time) {
        this.reads = reads;
        this.roles = roles;
        this.time = time;
    }

    /**
     * Quyền hiện hành, có bộ đệm trong tiến trình theo {@code (account_id, account.permission_version, tổng số phiên bản
     * quyền của các vai trò đang gán)}: lệnh đổi quyền tăng số phiên bản nên yêu cầu kế tiếp tính lại (FR-027).
     */
    public Map<PermissionKey, Set<RoleGrant>> current(long accountId) {
        Versions versions =
                new Versions(reads.accountPermissionVersion(accountId), roles.versionSum(reads.openRoles(accountId)));
        Cached cached = cache.get(accountId);
        if (cached != null && cached.versions().equals(versions)) {
            return cached.permissions();
        }
        Map<PermissionKey, Set<RoleGrant>> permissions = at(accountId, time.now());
        cache.put(accountId, new Cached(versions, permissions));
        return permissions;
    }

    /** Quyền tại thời điểm {@code at}, không qua bộ đệm (FR-026a). */
    public Map<PermissionKey, Set<RoleGrant>> at(long accountId, Instant at) {
        return RolePermissionRules.combine(
                reads.matrixOfAssignedRoles(accountId, at),
                reads.roleRestrictionRules(accountId, at),
                reads.accountRestrictionRules(accountId, at),
                // Quyền gán thêm chỉ tính khi tài khoản đang có vai trò Điều dưỡng tại t (FR-024 "tài khoản Điều
                // dưỡng")
                Set.copyOf(reads.nurseGrants(accountId, at)));
    }

    /**
     * Quyền tối đa (ma trận) và quyền hiện hành của một vai trò sau thu hẹp theo vai trò, tại {@code at} (002 T076;
     * {@code GET /roles/{code}/permissions}, FR-018, FR-020).
     */
    public RolePermissions ofRole(String role, Instant at) {
        Map<PermissionKey, Set<RoleGrant>> matrix = reads.matrixOfRole(role);
        return new RolePermissions(
                Map.copyOf(matrix),
                Map.copyOf(RolePermissionRules.ofRole(matrix, reads.restrictionRulesOfRole(role, at))));
    }

    /** Ma trận và quyền sau thu hẹp của một vai trò. */
    public record RolePermissions(
            Map<PermissionKey, Set<RoleGrant>> maximum, Map<PermissionKey, Set<RoleGrant>> current) {}

    private record Versions(long account, long roles) {}

    private record Cached(Versions versions, Map<PermissionKey, Set<RoleGrant>> permissions) {}
}

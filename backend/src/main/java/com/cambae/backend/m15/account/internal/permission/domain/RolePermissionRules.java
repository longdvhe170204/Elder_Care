package com.cambae.backend.m15.account.internal.permission.domain;

import com.cambae.backend.m15.account.api.dto.PermissionKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Thứ tự tính quyền vai trò (002 research A-05; FR-025, BR-M15-18, CHK017), tách khỏi phần đọc bảng để test đơn vị:
 *
 * <ol>
 *   <li>với từng vai trò: ma trận trừ thu hẹp theo vai trò;
 *   <li>hợp các vai trò; cùng khóa thì gộp phương án bằng {@link RoleGrants} (toàn viện thắng);
 *   <li>trừ thu hẹp theo tài khoản;
 *   <li>cộng quyền gán thêm (theo phân công như Điều dưỡng, FR-024).
 * </ol>
 */
public final class RolePermissionRules {

    private RolePermissionRules() {}

    /**
     * Một dòng thu hẹp đang hiệu lực: {@code REMOVE_OPERATION} bỏ khóa, {@code LOWER_SCOPE} hạ phạm vi; {@code role}
     * là {@code null} với thu hẹp theo tài khoản.
     */
    public record RestrictionRule(String role, PermissionKey key, boolean lowerScope) {

        void applyTo(Map<PermissionKey, Set<RoleGrant>> permissions) {
            if (lowerScope) {
                permissions.computeIfPresent(key, (k, grants) -> RoleGrants.lower(grants));
            } else {
                permissions.remove(key);
            }
        }
    }

    /** Bước 1 cho một vai trò: ma trận trừ thu hẹp theo vai trò đó. */
    public static Map<PermissionKey, Set<RoleGrant>> ofRole(
            Map<PermissionKey, Set<RoleGrant>> matrix, List<RestrictionRule> roleRestrictions) {
        Map<PermissionKey, Set<RoleGrant>> result = new HashMap<>(matrix);
        roleRestrictions.forEach(r -> r.applyTo(result));
        return result;
    }

    /** Bốn bước trên; {@code matrixByRole} theo mã vai trò, {@code roleRestrictions} lọc theo {@code role}. */
    public static Map<PermissionKey, Set<RoleGrant>> combine(
            Map<String, Map<PermissionKey, Set<RoleGrant>>> matrixByRole,
            List<RestrictionRule> roleRestrictions,
            List<RestrictionRule> accountRestrictions,
            Set<PermissionKey> additionalGrants) {
        Map<PermissionKey, Set<RoleGrant>> result = new HashMap<>();
        matrixByRole.forEach((role, matrix) -> ofRole(
                        matrix,
                        roleRestrictions.stream()
                                .filter(r -> role.equals(r.role()))
                                .toList())
                .forEach((key, grants) -> {
                    for (RoleGrant grant : grants) {
                        result.put(key, RoleGrants.add(result.getOrDefault(key, Set.of()), grant));
                    }
                }));
        accountRestrictions.forEach(r -> r.applyTo(result));
        for (PermissionKey key : additionalGrants) {
            result.put(
                    key,
                    RoleGrants.add(
                            result.getOrDefault(key, Set.of()),
                            new RoleGrant(PermissionScope.ASSIGNED, Set.of(), GrantSource.ADDITIONAL_GRANT)));
        }
        return Map.copyOf(result);
    }
}

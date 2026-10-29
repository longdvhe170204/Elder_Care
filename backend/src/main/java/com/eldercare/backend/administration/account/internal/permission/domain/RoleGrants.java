package com.eldercare.backend.administration.account.internal.permission.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * Gộp phương án của một khóa quyền (research A-05; FR-025): phương án bị phủ thì bỏ. {@code a} phủ {@code b} khi phạm vi
 * của {@code a} phủ phạm vi của {@code b} và chú thích của {@code a} là tập con của chú thích của {@code b} (ít điều
 * kiện bằng hoặc hơn). Nhờ vậy toàn viện thắng mà điều kiện của vai trò này không bị gán cho vai trò kia.
 */
public final class RoleGrants {

    private RoleGrants() {}

    /** Thêm một phương án, bỏ các phương án bị phủ. */
    public static Set<RoleGrant> add(Set<RoleGrant> grants, RoleGrant grant) {
        if (grants.stream().anyMatch(g -> covers(g, grant))) {
            return grants;
        }
        Set<RoleGrant> result = new HashSet<>();
        for (RoleGrant g : grants) {
            if (!covers(grant, g)) {
                result.add(g);
            }
        }
        result.add(grant);
        return Set.copyOf(result);
    }

    /** Thu hẹp kiểu {@code LOWER_SCOPE}: toàn viện xuống theo phân công; phạm vi khác giữ nguyên. */
    public static Set<RoleGrant> lower(Set<RoleGrant> grants) {
        Set<RoleGrant> result = Set.of();
        for (RoleGrant g : grants) {
            result = add(
                    result,
                    g.scope() == PermissionScope.FACILITY
                            ? new RoleGrant(PermissionScope.ASSIGNED, g.footnotes(), g.source())
                            : g);
        }
        return result;
    }

    private static boolean covers(RoleGrant a, RoleGrant b) {
        return a.scope().covers(b.scope()) && b.footnotes().containsAll(a.footnotes());
    }
}

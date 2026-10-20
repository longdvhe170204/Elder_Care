package com.cambae.backend.m15.account.internal.permission;

import java.util.Set;

/**
 * Một phương án được phép của một khóa quyền: phạm vi và mã chú thích có điều kiện phải thỏa (research A-04, A-05).
 * Một khóa có thể có nhiều phương án khi phạm vi rộng hơn lại kèm điều kiện mà phạm vi hẹp hơn thì không.
 */
public record RoleGrant(PermissionScope scope, Set<String> footnotes, GrantSource source) {

    public RoleGrant {
        footnotes = Set.copyOf(footnotes);
    }
}

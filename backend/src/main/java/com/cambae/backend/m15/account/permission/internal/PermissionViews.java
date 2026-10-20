package com.cambae.backend.m15.account.permission.internal;

import com.cambae.backend.m15.account.api.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Kiểu trả về về quyền theo contracts/api-002.yaml (FR-026, FR-026a, FR-042). */
public final class PermissionViews {

    private PermissionViews() {}

    /** Một phương án quyền hiện hành của một khóa. */
    @Schema(name = "EffectivePermission")
    public record EffectivePermission(
            String function,
            Operation operation,
            PermissionScope scope,
            List<String> footnoteCodes,
            GrantSource source) {}

    /** Kiểu thu hẹp (FR-025). */
    public enum RestrictionKind {
        REMOVE_OPERATION,
        LOWER_SCOPE
    }

    @Schema(name = "Restriction")
    public record Restriction(
            long id,
            String function,
            Operation operation,
            RestrictionKind kind,
            Instant effectiveFrom,
            String reason) {}

    /** Tập quyền của một tài khoản; ETag là {@code permission_version} (FR-026). */
    @Schema(name = "AccountPermissions")
    public record AccountPermissions(
            long accountId,
            Instant asOf,
            String status,
            List<String> roles,
            List<Restriction> roleRestrictions,
            List<Restriction> accountRestrictions,
            List<EffectivePermission> additionalGrants,
            List<EffectivePermission> current) {}
}

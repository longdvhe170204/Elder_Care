package com.eldercare.backend.administration.account.internal.permission.domain;

import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
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

    /** Dòng thu hẹp cho lệnh đổi quyền, theo vai trò ({@code role}) hoặc theo tài khoản ({@code accountId}). */
    public record RestrictionRow(
            long id,
            String role,
            Long accountId,
            PermissionKey key,
            RestrictionKind kind,
            String metricGroup,
            String channel) {}

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

    /** Một vai trò ({@code GET /roles}, FR-016). */
    @Schema(name = "RoleInfo")
    public record RoleInfo(String code, String name, String accountType, PermissionScope executionScope) {}

    /**
     * Quyền tối đa (ma trận), thu hẹp đang hiệu lực và quyền sau thu hẹp của một vai trò (FR-018, FR-020, FR-025); ETag
     * là {@code role.permission_version}.
     */
    @Schema(name = "RolePermissions")
    public record RolePermissions(
            String role,
            List<EffectivePermission> maximum,
            List<Restriction> restrictions,
            List<EffectivePermission> current) {}
}

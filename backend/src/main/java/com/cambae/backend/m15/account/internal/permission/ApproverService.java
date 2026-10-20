package com.cambae.backend.m15.account.internal.permission;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.ApproverApi;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.api.dto.PermissionBasis;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.PermissionResult;
import com.cambae.backend.m15.account.api.dto.Target;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Fallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Người có quyền duyệt (002 T054; contracts mục 1.5; 000 BR-M15-09, FR-031a): tài khoản nhân viên Hoạt động (kể cả Khóa
 * tạm đã quá hạn) được {@code PermissionApi} cho phép khóa trên đối tượng tại lúc hỏi, gồm quyền vai trò, nhiệm vụ và
 * phạm vi; căn cứ ngoại lệ ghi sự cố FR-044a không tính (danh sách luôn xét phạm vi). Không đệm. {@code @Fallback}: bản
 * giả của test 000 vẫn thắng.
 */
@Service
@Fallback
class ApproverService implements ApproverApi {

    private final JdbcTemplate jdbc;
    private final PermissionApi permissions;
    private final BusinessTime time;

    ApproverService(JdbcTemplate jdbc, PermissionApi permissions, BusinessTime time) {
        this.jdbc = jdbc;
        this.permissions = permissions;
        this.time = time;
    }

    @Override
    public boolean hasAnyApprover(PermissionKey key, Target target) {
        return candidates().stream().anyMatch(u -> approves(u, key, target));
    }

    @Override
    public List<Long> approvers(PermissionKey key, Target target) {
        return candidates().stream()
                .filter(u -> approves(u, key, target))
                .map(CurrentUser::accountId)
                .toList();
    }

    private boolean approves(CurrentUser user, PermissionKey key, Target target) {
        PermissionResult result = permissions.hasPermission(user, key, target);
        return result.allowed() && !result.basis().equals(Optional.of(PermissionBasis.OUT_OF_SCOPE));
    }

    /** Tài khoản nhân viên đang Hoạt động cùng vai trò đang gán. */
    private List<CurrentUser> candidates() {
        LocalDateTime now = LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC);
        Map<Long, Candidate> byAccount = new LinkedHashMap<>();
        jdbc.query(
                "SELECT a.id, a.staff_id, r.role FROM account a JOIN role_assignment r ON r.account_id = a.id"
                        + " WHERE a.type = 'STAFF' AND (a.status = 'ACTIVE'"
                        + " OR (a.status = 'TEMPORARILY_LOCKED' AND a.locked_until <= ?))"
                        + " AND r.effective_from <= ? AND (r.effective_to IS NULL OR r.effective_to > ?)"
                        + " ORDER BY a.id",
                rs -> {
                    long staffId = rs.getLong(2);
                    Candidate c = byAccount.computeIfAbsent(
                            rs.getLong(1), id -> new Candidate(id, staffId, EnumSet.noneOf(Role.class)));
                    String code = rs.getString(3);
                    Arrays.stream(Role.values())
                            .filter(r -> r.code().equals(code))
                            .forEach(c.roles()::add);
                },
                now,
                now,
                now);
        return List.copyOf(byAccount.values());
    }

    /** Người dùng dựng từ tài khoản để hỏi quyền, không có phiên. */
    private record Candidate(long accountId, long staff, Set<Role> roles) implements CurrentUser {

        @Override
        public AccountType type() {
            return AccountType.STAFF;
        }

        @Override
        public Optional<Long> staffId() {
            return Optional.of(staff);
        }

        @Override
        public Optional<Long> familyMemberId() {
            return Optional.empty();
        }

        @Override
        public Optional<Long> residentId() {
            return Optional.empty();
        }

        @Override
        public boolean isScheduler() {
            return false;
        }

        @Override
        public String deviceId() {
            throw new IllegalStateException("Người duyệt dựng từ tài khoản không có phiên");
        }

        @Override
        public AccessChannel channel() {
            throw new IllegalStateException("Người duyệt dựng từ tài khoản không có phiên");
        }
    }
}

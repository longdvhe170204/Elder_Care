package com.eldercare.backend.administration.account.internal.permission.service;

import com.eldercare.backend.administration.account.api.ApproverApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.AccessChannel;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.internal.persistence.AccountStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Fallback;
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

    private final AccountStore accounts;
    private final PermissionApi permissions;
    private final BusinessTime time;

    ApproverService(AccountStore accounts, PermissionApi permissions, BusinessTime time) {
        this.accounts = accounts;
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
        return accounts.staffWithRolesAt(time.now()).stream()
                .map(s -> (CurrentUser) new Candidate(s.accountId(), s.staffId(), s.roles()))
                .toList();
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

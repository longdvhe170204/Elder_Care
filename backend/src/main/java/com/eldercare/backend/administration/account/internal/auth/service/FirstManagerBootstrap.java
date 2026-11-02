package com.eldercare.backend.administration.account.internal.auth.service;

import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.spi.StaffProfileApi;
import com.eldercare.backend.administration.account.internal.auth.domain.AccountAuditActions;
import com.eldercare.backend.administration.account.internal.persistence.AccountStore;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.UnmetCondition;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khởi tạo tài khoản Quản lý viện đầu tiên (002 T016; BR-M15-22, FR-004b, Q-310; research A-03): chỉ khi chưa có tài
 * khoản nào đang có vai trò AC-01 (kiểm dưới khóa các dòng gán vai trò AC-01 ở {@code permission_assignment}); tạo hồ sơ
 * nhân viên tối thiểu qua 008, tài khoản Hoạt động buộc đổi mật khẩu, gán AC-01; nhật ký ghi người thực hiện "khởi tạo
 * hệ thống". Không có endpoint REST: chạy bằng
 * {@link com.eldercare.backend.administration.account.internal.auth.config.FirstManagerBootstrapRunner}.
 */
@Service
public class FirstManagerBootstrap {

    private static final Basis BASIS = new Basis("BR-M15-22", null, null, null);
    private static final String REASON = "Khởi tạo hệ thống (BR-M15-22)";

    private final AccountStore accounts;
    private final PasswordEncoder passwords;
    // ObjectProvider: 008 cài; trước đó chỉ test khai bản giả
    private final ObjectProvider<StaffProfileApi> staffProfiles;
    private final AuditLogApi auditLog;

    FirstManagerBootstrap(
            AccountStore accounts,
            PasswordEncoder passwords,
            ObjectProvider<StaffProfileApi> staffProfiles,
            AuditLogApi auditLog) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.staffProfiles = staffProfiles;
        this.auditLog = auditLog;
    }

    /** Tài khoản đã tạo và mật khẩu tạm (chỉ trả một lần, không lưu). */
    public record Result(long accountId, String username, String temporaryPassword) {}

    @Transactional
    public Result run(String username, String fullName) {
        // Khóa khoảng chỉ mục của AC-01: hai lần chạy đồng thời không cùng thấy "chưa có Quản lý viện"
        List<Long> managers = accounts.lockManagerAssignments();
        if (!managers.isEmpty()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("BR-M15-22", "Hệ thống đã có tài khoản Quản lý viện")));
        }
        long staffId = staffProfiles.getObject().createMinimal(fullName, Role.NURSING_HOME_MANAGER);
        String temporaryPassword = TemporaryPasswords.generate();
        long accountId = accounts.insertFirstManager(staffId, username, passwords.encode(temporaryPassword));
        accounts.history(accountId, AccountStatus.ACTIVE, null, null);
        accounts.addRole(accountId, Role.NURSING_HOME_MANAGER, null, REASON);

        auditLog.writeAsBootstrap(AuditLogEntry.builder()
                .action(AccountAuditActions.ACCOUNT_CREATED)
                .entity("M15.ACCOUNT", accountId)
                .after(Map.of("username", username, "type", "STAFF", "staffId", staffId, "status", "ACTIVE"))
                .basis(BASIS)
                .build());
        auditLog.writeAsBootstrap(AuditLogEntry.builder()
                .action(AccountAuditActions.ROLE_ASSIGNED)
                .entity("M15.ACCOUNT", accountId)
                .after(Map.of("role", Role.NURSING_HOME_MANAGER.code()))
                .reason(Reason.of(REASON))
                .basis(BASIS)
                .build());
        return new Result(accountId, username, temporaryPassword);
    }
}

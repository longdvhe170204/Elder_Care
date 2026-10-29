package com.eldercare.backend.administration.account.internal.auth.service;

import com.eldercare.backend.administration.account.api.spi.StaffProfileApi;
import com.eldercare.backend.administration.account.internal.auth.domain.AccountAuditActions;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khởi tạo tài khoản Quản lý viện đầu tiên (002 T016; BR-M15-22, FR-004b, Q-310; research A-03): chỉ khi chưa có tài
 * khoản nào đang có vai trò AC-01 (kiểm dưới khóa các dòng {@code role_assignment} của AC-01); tạo hồ sơ nhân viên tối
 * thiểu qua 008, tài khoản Hoạt động buộc đổi mật khẩu, gán AC-01; nhật ký ghi người thực hiện "khởi tạo hệ thống".
 * Không có endpoint REST: chạy bằng {@link com.eldercare.backend.administration.account.internal.auth.config.FirstManagerBootstrapRunner}.
 */
@Service
public class FirstManagerBootstrap {

    private static final Basis BASIS = new Basis("BR-M15-22", null, null, null);
    private static final String REASON = "Khởi tạo hệ thống (BR-M15-22)";

    private final JdbcTemplate jdbc;
    private final BusinessTime time;
    private final PasswordEncoder passwords;
    // ObjectProvider: 008 cài; trước đó chỉ test khai bản giả
    private final ObjectProvider<StaffProfileApi> staffProfiles;
    private final AuditLogApi auditLog;

    FirstManagerBootstrap(
            JdbcTemplate jdbc,
            BusinessTime time,
            PasswordEncoder passwords,
            ObjectProvider<StaffProfileApi> staffProfiles,
            AuditLogApi auditLog) {
        this.jdbc = jdbc;
        this.time = time;
        this.passwords = passwords;
        this.staffProfiles = staffProfiles;
        this.auditLog = auditLog;
    }

    /** Tài khoản đã tạo và mật khẩu tạm (chỉ trả một lần, không lưu). */
    public record Result(long accountId, String username, String temporaryPassword) {}

    @Transactional
    public Result run(String username, String fullName) {
        // Khóa khoảng chỉ mục của AC-01: hai lần chạy đồng thời không cùng thấy "chưa có Quản lý viện"
        List<Long> managers = jdbc.queryForList(
                "SELECT id FROM role_assignment WHERE role = 'AC-01' AND effective_to IS NULL FOR UPDATE", Long.class);
        if (!managers.isEmpty()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("BR-M15-22", "Hệ thống đã có tài khoản Quản lý viện")));
        }
        long staffId = staffProfiles.getObject().createMinimal(fullName, Role.NURSING_HOME_MANAGER);
        String temporaryPassword = TemporaryPasswords.generate();
        LocalDateTime now = LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC);

        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO account (username, type, staff_id, status, password_hash,"
                                    + " must_change_password, inactivity_reference_at, created_at, created_by)"
                                    + " VALUES (?, 'STAFF', ?, 'ACTIVE', ?, TRUE, ?, ?, NULL)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, username);
                    ps.setLong(2, staffId);
                    ps.setString(3, passwords.encode(temporaryPassword));
                    ps.setObject(4, now);
                    ps.setObject(5, now);
                    return ps;
                },
                key);
        long accountId = key.getKey().longValue();
        jdbc.update(
                "INSERT INTO account_status_history (account_id, status, effective_from, actor_id)"
                        + " VALUES (?, 'ACTIVE', ?, NULL)",
                accountId,
                now);
        jdbc.update(
                "INSERT INTO role_assignment (account_id, role, effective_from, assigned_by, reason)"
                        + " VALUES (?, 'AC-01', ?, NULL, ?)",
                accountId,
                now,
                REASON);

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

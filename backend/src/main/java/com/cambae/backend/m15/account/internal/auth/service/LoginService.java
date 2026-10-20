package com.cambae.backend.m15.account.internal.auth.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountStatus;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.internal.auth.domain.AccountAuditActions;
import com.cambae.backend.m15.account.internal.auth.domain.ActiveSession;
import com.cambae.backend.m15.account.internal.auth.domain.LoginRules;
import com.cambae.backend.m15.account.internal.auth.domain.LoginRules.Decision;
import com.cambae.backend.m15.account.internal.persistence.AccountStore;
import com.cambae.backend.m15.account.internal.persistence.AccountStore.AccountRow;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Đăng nhập, đăng xuất, tự đổi mật khẩu (002 T041, T042; research A-02, A-03; FR-006 → FR-008, FR-013, FR-014,
 * FR-014a, FR-015). Lần thất bại không ném lỗi trong transaction: bộ đếm sai, khóa tạm và nhật ký được lưu rồi
 * controller mới trả lỗi. Không phải lệnh nghiệp vụ ({@code @BusinessCommand}): chưa có người dùng khi đăng nhập, đổi
 * mật khẩu là việc của chính người dùng (plan 002 ghi chú Phase 5).
 */
@Service
public class LoginService {

    private static final Basis LOCK_BASIS = new Basis("CFG-M15-01", null, null, null);

    private final AccountStore store;
    private final PasswordEncoder passwords;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final AuditLogApi auditLog;
    // Băm giả để tên không tồn tại tốn cùng thời gian kiểm như tên có thật (research A-03)
    private volatile String dummyHash;

    LoginService(
            AccountStore store,
            PasswordEncoder passwords,
            BusinessTime time,
            ParameterApi parameters,
            AuditLogApi auditLog) {
        this.store = store;
        this.passwords = passwords;
        this.time = time;
        this.parameters = parameters;
        this.auditLog = auditLog;
    }

    /** Kết quả đăng nhập. */
    public sealed interface Result {}

    /** {@code token} giao cho client (cookie với web, thân phản hồi với app). */
    /** Đăng nhập thành công: token và thông tin người dùng cho phản hồi (loại, vai trò đang gán). */
    public record Success(String token, long accountId, AccountType type, Set<Role> roles, boolean mustChangePassword)
            implements Result {}

    public record Refused(String errorCode, int httpStatus, Instant unlocksAt) implements Result {}

    @Transactional
    public Result login(String username, String password, AccessChannel channel, String deviceId) {
        if (channel == AccessChannel.RESIDENT_APP) {
            throw new InvalidValueException("Đăng nhập kênh người cao tuổi làm ở 002 US8 (T092)");
        }
        Instant now = time.now();
        Optional<AccountRow> found = store.lockByUsername(username);
        if (found.isEmpty()) {
            passwords.matches(password, dummyHash());
            Decision refused = LoginRules.decide(null, null, false, now);
            auditLog.writeAnonymous(AuditLogEntry.builder()
                    .action(AccountAuditActions.LOGIN_FAILED)
                    .entity(AccountAuditActions.ENTITY, "")
                    .details(Map.of("username", username, "channel", channel.name()))
                    .build());
            return new Refused(refused.errorCode(), refused.httpStatus(), null);
        }
        AccountRow account = found.get();
        if (LoginRules.effective(account.status(), account.lockedUntil(), now) != account.status()) {
            endTemporaryLock(account);
        }
        boolean matches = passwords.matches(password, store.passwordHash(account.id()));
        Decision decision = LoginRules.decide(account.status(), account.lockedUntil(), matches, now);
        if (!decision.allowed()) {
            auditLog.writeAnonymous(AuditLogEntry.builder()
                    .action(AccountAuditActions.LOGIN_FAILED)
                    .entity(AccountAuditActions.ENTITY, account.id())
                    .details(Map.of("channel", channel.name(), "errorCode", decision.errorCode()))
                    .build());
            if (decision.countsFailure()) {
                countFailure(account, now);
            }
            return new Refused(decision.errorCode(), decision.httpStatus(), decision.unlocksAt());
        }
        String token = SessionTokens.generate();
        store.recordLogin(account.id());
        store.openSession(SessionTokens.hash(token), account.id(), deviceId, channel.name());
        ActiveSession session = new ActiveSession(
                account.id(),
                account.type(),
                Optional.ofNullable(account.staffId()),
                Optional.ofNullable(account.familyMemberId()),
                store.roles(account.id()),
                deviceId,
                channel);
        runAs(
                session,
                () -> auditLog.write(AuditLogEntry.builder()
                        .action(AccountAuditActions.LOGIN)
                        .entity(AccountAuditActions.ENTITY, account.id())
                        .details(Map.of("channel", channel.name()))
                        .build()));
        return new Success(
                token, account.id(), account.type(), store.roles(account.id()), store.mustChangePassword(account.id()));
    }

    /** Kết thúc phiên hiện tại ({@code LOGOUT}, FR-013). */
    @Transactional
    public void logout(String token) {
        store.endSession(SessionTokens.hash(token), "LOGOUT");
    }

    /**
     * Người dùng tự đổi mật khẩu (FR-006, FR-006a): kiểm mật khẩu hiện tại, độ mạnh theo CFG-M15-09 và khác mật khẩu
     * hiện tại; gỡ cờ buộc đổi; nhật ký không chứa mật khẩu.
     */
    @Transactional
    public void changePassword(CurrentUser user, String currentPassword, String newPassword) {
        long accountId = user.accountId();
        store.lock(accountId).orElseThrow();
        if (currentPassword == null || !passwords.matches(currentPassword, store.passwordHash(accountId))) {
            throw new InvalidValueException("Mật khẩu hiện tại không đúng");
        }
        List<UnmetCondition> unmet = policyViolations(newPassword);
        if (newPassword != null && newPassword.equals(currentPassword)) {
            unmet.add(new UnmetCondition("FR-006a", "Mật khẩu mới phải khác mật khẩu hiện tại"));
        }
        if (!unmet.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "PASSWORD_POLICY_NOT_MET",
                    "Mật khẩu mới chưa đạt yêu cầu",
                    Map.of("unmetConditions", unmet));
        }
        store.changePassword(accountId, passwords.encode(newPassword));
        auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.PASSWORD_CHANGED)
                .entity(AccountAuditActions.ENTITY, accountId)
                .after(Map.of("mustChangePassword", false))
                .build());
    }

    /** CFG-M15-09 dạng {@code {minLength, requireLetter, requireDigit}}; mỗi yêu cầu không đạt là một dòng. */
    private List<UnmetCondition> policyViolations(String password) {
        JsonNode policy = parameters.read(ParameterCode.CFG_M15_09).raw();
        int minLength = policy.get("minLength").intValue();
        boolean letter = policy.get("requireLetter").booleanValue();
        boolean digit = policy.get("requireDigit").booleanValue();
        String p = password == null ? "" : password;
        List<UnmetCondition> unmet = new ArrayList<>();
        if (p.length() < minLength
                || (letter && p.chars().noneMatch(Character::isLetter))
                || (digit && p.chars().noneMatch(Character::isDigit))) {
            StringBuilder rule = new StringBuilder("Tối thiểu " + minLength + " ký tự");
            if (letter && digit) {
                rule.append(", có cả chữ và số");
            } else if (letter) {
                rule.append(", có chữ");
            } else if (digit) {
                rule.append(", có số");
            }
            unmet.add(new UnmetCondition("CFG-M15-09", rule.toString()));
        }
        return unmet;
    }

    /** Hết thời gian khóa tạm: về Hoạt động, bộ đếm về 0; Bộ lập lịch thực hiện, căn cứ CFG-M15-01 (research A-03). */
    private void endTemporaryLock(AccountRow account) {
        store.changeStatus(account.id(), AccountStatus.ACTIVE, null, null);
        store.resetFailedLogins(account.id());
        SchedulerUserAspect.runAsScheduler(() -> auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.TEMPORARY_LOCK_EXPIRED)
                .entity(AccountAuditActions.ENTITY, account.id())
                .before(Map.of("status", AccountStatus.TEMPORARILY_LOCKED.name()))
                .after(Map.of("status", AccountStatus.ACTIVE.name()))
                .basis(LOCK_BASIS)
                .build()));
    }

    /** Đếm lần sai; đủ ngưỡng CFG-M15-01 thì Khóa tạm và kết thúc phiên (FR-008, FR-013). */
    private void countFailure(AccountRow account, Instant now) {
        JsonNode rule = parameters.read(ParameterCode.CFG_M15_01).raw();
        int failures = store.recordFailedLogin(account.id());
        if (failures < rule.get("maxFailedAttempts").intValue()) {
            return;
        }
        JsonNode duration = rule.get("lockDuration");
        Instant until = now.plus(Duration.of(
                duration.get("value").longValue(),
                ChronoUnit.valueOf(duration.get("unit").asString())));
        store.temporarilyLock(account.id(), until);
        store.endSessions(account.id(), "ACCOUNT_LEFT_ACTIVE");
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", AccountStatus.TEMPORARILY_LOCKED.name());
        after.put("lockedUntil", until.toString());
        SchedulerUserAspect.runAsScheduler(() -> auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.ACCOUNT_TEMPORARILY_LOCKED)
                .entity(AccountAuditActions.ENTITY, account.id())
                .before(Map.of("status", AccountStatus.ACTIVE.name()))
                .after(after)
                .basis(LOCK_BASIS)
                .build()));
    }

    private String dummyHash() {
        String hash = dummyHash;
        if (hash == null) {
            hash = passwords.encode(SessionTokens.generate());
            dummyHash = hash;
        }
        return hash;
    }

    /** Ghi nhật ký đăng nhập thành công với người thực hiện là chính tài khoản vừa đăng nhập. */
    private static void runAs(ActiveSession session, Runnable work) {
        SecurityContext outer = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new SessionAuthentication(session));
        SecurityContextHolder.setContext(context);
        try {
            work.run();
        } finally {
            SecurityContextHolder.setContext(outer);
        }
    }
}

package com.eldercare.backend.administration.account.internal.auth.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccessChannel;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.internal.auth.domain.AccountAuditActions;
import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.administration.account.internal.auth.domain.LoginRules;
import com.eldercare.backend.administration.account.internal.auth.domain.LoginRules.Decision;
import com.eldercare.backend.administration.account.internal.auth.domain.QrCode;
import com.eldercare.backend.administration.account.internal.auth.persistence.DeviceBindings;
import com.eldercare.backend.administration.account.internal.domain.Account;
import com.eldercare.backend.administration.account.internal.persistence.AccountStore;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.Basis;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.ParameterCode;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
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
    private final ResidentDevices devices;
    private final DeviceBindings bindings;
    // Băm giả để tên không tồn tại tốn cùng thời gian kiểm như tên có thật (research A-03)
    private volatile String dummyHash;

    LoginService(
            AccountStore store,
            PasswordEncoder passwords,
            BusinessTime time,
            ParameterApi parameters,
            AuditLogApi auditLog,
            ResidentDevices devices,
            DeviceBindings bindings) {
        this.store = store;
        this.devices = devices;
        this.bindings = bindings;
        this.passwords = passwords;
        this.time = time;
        this.parameters = parameters;
        this.auditLog = auditLog;
    }

    /** Kết quả đăng nhập. */
    public sealed interface Result {}

    /** {@code token} giao cho client (cookie với web, thân phản hồi với app). */
    /** Đăng nhập thành công: token và thông tin người dùng cho phản hồi (loại, vai trò đang gán). */
    public record Success(
            String token,
            String deviceToken,
            long accountId,
            AccountType type,
            Set<Role> roles,
            boolean mustChangePassword)
            implements Result {}

    public record Refused(String errorCode, int httpStatus, Instant unlocksAt) implements Result {}

    /**
     * Đăng nhập bằng tên, mật khẩu. {@code channel} rỗng là app chung ({@code MOBILE_APP}, đổi api-002 2026-10-28):
     * kênh phiên gán theo loại tài khoản sau khi đúng mật khẩu, nhân viên STAFF_APP, người thân FAMILY_APP, để thời gian
     * tự đăng xuất theo kênh (FR-013a, CFG-M15-10) giữ nguyên; người cao tuổi đăng nhập bằng mã QR (FR-069).
     */
    @Transactional
    public Result login(String username, String password, Optional<AccessChannel> requested, String deviceId) {
        Instant now = time.now();
        String requestedName = requested.map(AccessChannel::name).orElse("MOBILE_APP");
        Optional<Account> found = store.lockByUsername(username);
        if (found.isEmpty()) {
            passwords.matches(password, dummyHash());
            Decision refused = LoginRules.decide(null, null, false, now);
            auditLog.writeAnonymous(AuditLogEntry.builder()
                    .action(AccountAuditActions.LOGIN_FAILED)
                    .entity(AccountAuditActions.ENTITY, "")
                    .details(Map.of("username", username, "channel", requestedName))
                    .build());
            return new Refused(refused.errorCode(), refused.httpStatus(), null);
        }
        Account account = found.get();
        if (LoginRules.effective(account.status(), account.lockedUntil(), now) != account.status()) {
            endTemporaryLock(account);
        }
        boolean matches = passwords.matches(password, store.passwordHash(account.id()));
        Decision decision = LoginRules.decide(account.status(), account.lockedUntil(), matches, now);
        if (!decision.allowed()) {
            auditLog.writeAnonymous(AuditLogEntry.builder()
                    .action(AccountAuditActions.LOGIN_FAILED)
                    .entity(AccountAuditActions.ENTITY, account.id())
                    .details(Map.of("channel", requestedName, "errorCode", decision.errorCode()))
                    .build());
            if (decision.countsFailure()) {
                countFailure(account, now);
            }
            return new Refused(decision.errorCode(), decision.httpStatus(), decision.unlocksAt());
        }
        AccessChannel channel = requested.orElseGet(() -> switch (account.type()) {
            case STAFF -> AccessChannel.STAFF_APP;
            case FAMILY_MEMBER -> AccessChannel.FAMILY_APP;
            case RESIDENT -> throw new InvalidValueException("Người cao tuổi đăng nhập bằng mã QR");
        });
        // Kênh người cao tuổi chỉ cho tài khoản người cao tuổi: đăng nhập là gắn thiết bị (research A-15)
        boolean resident = channel == AccessChannel.RESIDENT_APP;
        if (resident && account.type() != AccountType.RESIDENT) {
            throw new InvalidValueException("Kênh người cao tuổi chỉ dùng cho tài khoản người cao tuổi");
        }
        if (devices.boundToOther(account.id(), deviceId)) {
            return new Refused("DEVICE_ALREADY_BOUND", 409, null);
        }
        return open(account, channel, deviceId, resident, null, Map.of("channel", channel.name()));
    }

    /**
     * Người cao tuổi đăng nhập bằng mã QR (002 T092; FR-069, FR-070, BR-M15-24; research A-15): trong một transaction
     * kiểm mã chưa dùng, chưa vô hiệu, chưa hết hạn (sai thì 401 {@code INVALID_QR_CODE}), tài khoản Hoạt động hoặc Khóa
     * tạm đã hết hạn (không thì 423 như đăng nhập), thiết bị không gắn với tài khoản khác (409); gắn thiết bị, mở phiên
     * kênh RESIDENT_APP, đánh dấu mã đã dùng.
     */
    @Transactional
    public Success qrLogin(String code, String deviceId) {
        Instant now = time.now();
        QrCode qr = (code == null ? Optional.<QrCode>empty() : bindings.lockCode(SessionTokens.hash(code)))
                .filter(c -> c.usableAt(now))
                .orElseThrow(LoginService::invalidQrCode);
        Account account = store.lock(qr.accountId())
                .filter(a -> a.type() == AccountType.RESIDENT)
                .orElseThrow(LoginService::invalidQrCode);
        if (LoginRules.effective(account.status(), account.lockedUntil(), now) != account.status()) {
            endTemporaryLock(account);
        }
        Decision decision = LoginRules.decide(account.status(), account.lockedUntil(), true, now);
        if (!decision.allowed()) {
            throw new BusinessException(
                    HttpStatus.valueOf(decision.httpStatus()),
                    decision.errorCode(),
                    "Tài khoản không đăng nhập được, cần liên hệ viện",
                    decision.unlocksAt() == null ? Map.of() : Map.of("unlocksAt", decision.unlocksAt()));
        }
        if (devices.boundToOther(account.id(), deviceId)) {
            throw ResidentDevices.deviceAlreadyBound();
        }
        bindings.markCodeUsed(qr.id(), deviceId);
        return open(account, AccessChannel.RESIDENT_APP, deviceId, true, qr.id(), Map.of("deviceId", deviceId));
    }

    /**
     * Mở phiên (đóng phiên đang mở khác trên thiết bị, BR-M15-15); kênh người cao tuổi thì gắn thiết bị trước để phiên
     * mới không bị kết thúc cùng thiết bị cũ. Nhật ký ghi người thực hiện là chính tài khoản vừa đăng nhập.
     */
    private Success open(
            Account account,
            AccessChannel channel,
            String deviceId,
            boolean bindDevice,
            Long qrCodeId,
            Map<String, Object> details) {
        String token = SessionTokens.generate();
        ActiveSession session = new ActiveSession(
                account.id(),
                account.type(),
                Optional.ofNullable(account.staffId()),
                Optional.ofNullable(account.familyMemberId()),
                store.roles(account.id()),
                deviceId,
                channel,
                Optional.ofNullable(account.residentId()));
        String[] deviceToken = new String[1];
        runAs(session, () -> {
            if (bindDevice) {
                deviceToken[0] = devices.bind(account.id(), deviceId, qrCodeId);
            }
            auditLog.write(AuditLogEntry.builder()
                    .action(qrCodeId == null ? AccountAuditActions.LOGIN : AccountAuditActions.LOGIN_QR_USED)
                    .entity(AccountAuditActions.ENTITY, account.id())
                    .details(details)
                    .build());
        });
        store.recordLogin(account.id());
        store.openSession(SessionTokens.hash(token), account.id(), deviceId, channel.name());
        return new Success(
                token,
                deviceToken[0],
                account.id(),
                account.type(),
                store.roles(account.id()),
                store.mustChangePassword(account.id()));
    }

    private static BusinessException invalidQrCode() {
        return new BusinessException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_QR_CODE",
                "Mã QR đã dùng, đã hết hạn hoặc không còn hiệu lực; nhờ nhân viên tạo mã mới");
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
        // Tài khoản người cao tuổi: đổi mật khẩu kết thúc mọi phiên (FR-013a, BR-M15-24); đăng nhập lại bằng mã QR
        if (user.type() == AccountType.RESIDENT) {
            store.endSessions(accountId, "PASSWORD_CHANGED");
        }
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
    private void endTemporaryLock(Account account) {
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
    private void countFailure(Account account, Instant now) {
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

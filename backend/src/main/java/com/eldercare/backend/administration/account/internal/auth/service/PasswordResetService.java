package com.eldercare.backend.administration.account.internal.auth.service;

import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.events.PasswordSelfReset;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.account.api.spi.PasswordResetSmsPort;
import com.eldercare.backend.administration.account.api.spi.StaffProfileApi;
import com.eldercare.backend.administration.account.internal.auth.domain.AccountAuditActions;
import com.eldercare.backend.administration.account.internal.auth.domain.LoginRules;
import com.eldercare.backend.administration.account.internal.auth.domain.PasswordResetCode;
import com.eldercare.backend.administration.account.internal.auth.domain.PasswordResetRules;
import com.eldercare.backend.administration.account.internal.auth.persistence.PasswordResetCodes;
import com.eldercare.backend.administration.account.internal.domain.Account;
import com.eldercare.backend.administration.account.internal.persistence.AccountStore;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Tự đặt lại mật khẩu bằng mã qua tin nhắn (002 T105; FR-052a, BR-M15-25, Q-399; CFG-M15-12 → 14). Xin mã không báo
 * lý do không gửi (tên không tồn tại, không có số, sai trạng thái, quá giới hạn đều như nhau). Lần nhập sai không ném lỗi
 * trong transaction để bộ đếm sai được lưu; controller trả lỗi từ kết quả (như đăng nhập). Không phải lệnh nghiệp vụ:
 * chưa có người dùng đăng nhập, nhật ký ghi người thực hiện ẩn danh.
 */
@Service
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountStore store;
    private final PasswordResetCodes codes;
    private final LoginService logins;
    private final org.springframework.security.crypto.password.PasswordEncoder passwords;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<PasswordResetSmsPort> sms;
    private final ObjectProvider<StaffProfileApi> staffProfiles;
    private final ObjectProvider<FamilyRelationshipApi> families;

    PasswordResetService(
            AccountStore store,
            PasswordResetCodes codes,
            LoginService logins,
            org.springframework.security.crypto.password.PasswordEncoder passwords,
            BusinessTime time,
            ParameterApi parameters,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            ObjectProvider<PasswordResetSmsPort> sms,
            ObjectProvider<StaffProfileApi> staffProfiles,
            ObjectProvider<FamilyRelationshipApi> families) {
        this.store = store;
        this.codes = codes;
        this.logins = logins;
        this.passwords = passwords;
        this.time = time;
        this.parameters = parameters;
        this.auditLog = auditLog;
        this.events = events;
        this.sms = sms;
        this.staffProfiles = staffProfiles;
        this.families = families;
    }

    /** Kết quả đặt mật khẩu mới. */
    public sealed interface Result {}

    public record Done() implements Result {}

    /** Mã sai, hết hạn, đã hủy, đã dùng, hoặc tài khoản không hợp lệ: cùng một lỗi. */
    public record InvalidCode() implements Result {}

    public record WeakPassword(List<UnmetCondition> unmet) implements Result {}

    /** Kênh tin nhắn đã cấu hình (Q-06). */
    public boolean available() {
        PasswordResetSmsPort port = sms.getIfAvailable();
        return port != null && port.available();
    }

    /** Xin mã (FR-052a): chỉ gửi khi đủ điều kiện; mọi trường hợp đều trả về như nhau. */
    @Transactional
    public void request(String username) {
        PasswordResetSmsPort port = requireAvailable();
        Instant now = time.now();
        Optional<Account> found = store.lockByUsername(username.strip());
        if (found.isEmpty()) {
            return;
        }
        Account account = found.get();
        Optional<String> phone = phone(account);
        AccountStatus status = LoginRules.effective(account.status(), account.lockedUntil(), now);
        if (!PasswordResetRules.eligible(account.type(), status, phone)) {
            return;
        }
        JsonNode limit = parameters.read(ParameterCode.CFG_M15_14).raw();
        JsonNode interval = limit.get("minInterval");
        boolean allowed = PasswordResetRules.canRequest(
                now,
                codes.sentSince(account.id(), time.startOfDay(time.today())),
                codes.lastSentAt(account.id()),
                limit.get("maxPerDay").intValue(),
                Duration.of(
                        interval.get("value").longValue(),
                        ChronoUnit.valueOf(interval.get("unit").asString())));
        if (!allowed) {
            return;
        }
        Duration validity = parameters.read(ParameterCode.CFG_M15_12).asDuration();
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codes.insert(account.id(), hash(account.id(), code), now.plus(validity));
        boolean sent = port.send(
                phone.orElseThrow(),
                "Ma dat lai mat khau: " + code + " (hieu luc " + validity.toMinutes()
                        + " phut). Khong dua ma cho nguoi khac.");
        auditLog.writeAnonymous(AuditLogEntry.builder()
                .action(AccountAuditActions.PASSWORD_RESET_CODE_SENT)
                .entity(AccountAuditActions.ENTITY, account.id())
                .details(Map.of("accepted", sent))
                .build());
    }

    /**
     * Đặt mật khẩu mới bằng mã (FR-052a, BR-M15-25): mã đúng và mật khẩu đạt CFG-M15-09 thì đổi mật khẩu (không gắn cờ
     * buộc đổi), kết thúc mọi phiên, bộ đếm sai về 0, Khóa tạm trở về Hoạt động, báo chủ tài khoản. Mã sai thì tăng bộ
     * đếm, sai đủ CFG-M15-13 lần thì hủy mã; mật khẩu chưa đạt thì mã vẫn dùng được.
     */
    @Transactional
    public Result confirm(String username, String code, String newPassword) {
        requireAvailable();
        Instant now = time.now();
        Optional<Account> found = store.lockByUsername(username.strip());
        if (found.isEmpty()) {
            return new InvalidCode();
        }
        Account account = found.get();
        AccountStatus status = LoginRules.effective(account.status(), account.lockedUntil(), now);
        if (account.type() == AccountType.RESIDENT
                || (status != AccountStatus.ACTIVE && status != AccountStatus.TEMPORARILY_LOCKED)) {
            return new InvalidCode();
        }
        Optional<PasswordResetCode> active = codes.lockActive(account.id());
        if (active.isEmpty()) {
            return new InvalidCode();
        }
        PasswordResetCode current = active.get();
        if (code == null || !MessageDigest.isEqual(current.codeHash(), hash(account.id(), code.strip()))) {
            int failures = current.failedAttempts() + 1;
            int max = parameters.read(ParameterCode.CFG_M15_13).asInt();
            codes.recordFailure(current.id(), PasswordResetRules.revokeAfter(failures, max));
            return new InvalidCode();
        }
        List<UnmetCondition> unmet = logins.policyViolations(newPassword);
        if (!unmet.isEmpty()) {
            return new WeakPassword(unmet);
        }
        codes.markUsed(current.id());
        store.changePassword(account.id(), passwords.encode(newPassword));
        store.endSessions(account.id(), "PASSWORD_CHANGED");
        store.resetFailedLogins(account.id());
        if (account.status() == AccountStatus.TEMPORARILY_LOCKED) {
            store.changeStatus(account.id(), AccountStatus.ACTIVE, null, account.id());
        }
        auditLog.writeAnonymous(AuditLogEntry.builder()
                .action(AccountAuditActions.PASSWORD_SELF_RESET)
                .entity(AccountAuditActions.ENTITY, account.id())
                .before(Map.of("status", account.status().name()))
                .after(Map.of("status", AccountStatus.ACTIVE.name(), "mustChangePassword", false))
                .build());
        events.publishEvent(new PasswordSelfReset(
                account.id(),
                account.staffId(),
                account.familyMemberId(),
                store.roles(account.id()).contains(Role.NURSING_HOME_MANAGER),
                now));
        return new Done();
    }

    private PasswordResetSmsPort requireAvailable() {
        PasswordResetSmsPort port = sms.getIfAvailable();
        if (port == null || !port.available()) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND, "PASSWORD_RESET_UNAVAILABLE", "Chưa có kênh tin nhắn để đặt lại mật khẩu");
        }
        return port;
    }

    /** Số trong hồ sơ của chủ tài khoản (008, 012), không phải số người yêu cầu nhập. */
    private Optional<String> phone(Account account) {
        return switch (account.type()) {
            case STAFF ->
                Optional.ofNullable(staffProfiles.getIfAvailable()).flatMap(api -> api.phoneNumber(account.staffId()));
            case FAMILY_MEMBER ->
                Optional.ofNullable(families.getIfAvailable())
                        .flatMap(api -> api.registeredPhoneNumber(account.familyMemberId()));
            case RESIDENT -> Optional.empty();
        };
    }

    private static byte[] hash(long accountId, String code) {
        return SessionTokens.hash(accountId + ":" + code);
    }
}

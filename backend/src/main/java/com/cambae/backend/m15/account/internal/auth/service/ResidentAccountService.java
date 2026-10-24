package com.cambae.backend.m15.account.internal.auth.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.AccountStatus;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.PermissionResult;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ResidentProfileApi;
import com.cambae.backend.m15.account.api.spi.ResidentProfileView;
import com.cambae.backend.m15.account.internal.auth.domain.AccountAuditActions;
import com.cambae.backend.m15.account.internal.auth.persistence.DeviceBindings;
import com.cambae.backend.m15.account.internal.auth.service.AccountService.AccountDetails;
import com.cambae.backend.m15.account.internal.domain.Account;
import com.cambae.backend.m15.account.internal.persistence.AccountStore;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh trên tài khoản người cao tuổi không cần hồ sơ người cao tuổi của 001 (002 T092, T094; FR-069, FR-070, FR-072,
 * FR-073, BR-M15-23, BR-M15-24; research A-15, A-16): tạo mã QR đăng nhập, gỡ thiết bị, khóa theo yêu cầu của chính
 * người cao tuổi hoặc có lý do, khóa theo đề nghị của người thân đã được Quản lý viện duyệt, mở khóa khi người cao tuổi
 * yêu cầu. Tạo tài khoản (T091) và khóa khi kết thúc lưu trú ở {@link AccountService}.
 */
@Service
public class ResidentAccountService {

    /** Mã QR chỉ trả một lần. */
    public record LoginCode(String code, Instant expiresAt) {}

    /** Người cao tuổi yêu cầu hoặc đồng ý trực tiếp với nhân viên nào (contracts {@code ResidentConsent}). */
    public record Consent(String method, Long withStaffId) {}

    static final Set<String> LOCK_BASES = Set.of("RESIDENT_REQUESTED", "WITH_REASON");
    private static final Set<String> CONSENT_METHODS = Set.of("REQUESTED", "AGREED");
    private static final PermissionKey CREATE = new PermissionKey(Function.RESIDENT_ACCOUNT, Operation.CREATE);

    private final AccountStore store;
    private final AccountService accounts;
    private final DeviceBindings bindings;
    private final ResidentDevices devices;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<PermissionApi> permissions;
    private final ObjectProvider<ResidentProfileApi> residentProfiles;
    private final ParameterApi parameters;
    private final AuditLogApi auditLog;
    private final BusinessTime time;

    ResidentAccountService(
            AccountStore store,
            AccountService accounts,
            DeviceBindings bindings,
            ResidentDevices devices,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<PermissionApi> permissions,
            ObjectProvider<ResidentProfileApi> residentProfiles,
            ParameterApi parameters,
            AuditLogApi auditLog,
            BusinessTime time) {
        this.store = store;
        this.accounts = accounts;
        this.bindings = bindings;
        this.devices = devices;
        this.currentUser = currentUser;
        this.permissions = permissions;
        this.residentProfiles = residentProfiles;
        this.parameters = parameters;
        this.auditLog = auditLog;
        this.time = time;
    }

    /**
     * Mã QR dùng một lần, hết hạn sau CFG-M15-11; vô hiệu mã chưa dùng trước đó (FR-069). Hành chính, Quản lý viện, hoặc
     * Điều dưỡng có phạm vi với người cao tuổi (Phụ lục 27 ⁴⁸): kiểm quyền {@code (RESIDENT_ACCOUNT, CREATE)} trên người
     * cao tuổi của tài khoản, đọc được sau khi tra tài khoản nên lệnh tự kiểm ({@code RequiresPermissionArchTest}).
     */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public LoginCode createLoginCode(long accountId) {
        CurrentUser user = currentUser.getObject();
        Account account = store.lock(accountId).orElseThrow(NotFoundException::new);
        PermissionResult result = permissions.getObject().hasPermission(user, CREATE, target(account));
        if (!result.allowed()) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED",
                    "Không có quyền tạo mã đăng nhập cho tài khoản này",
                    Map.of("failedLayer", result.failedLayer().map(Enum::name).orElse("ROLE")));
        }
        requireResident(account);
        if (account.status() == AccountStatus.LOCKED) {
            throw conflict("ACCOUNT_LOCKED", "Tài khoản đã khóa, không tạo mã đăng nhập");
        }
        bindings.revokeUnusedCodes(accountId);
        String code = SessionTokens.generateCode();
        Instant expiresAt =
                time.now().plus(parameters.read(ParameterCode.CFG_M15_11).asDuration());
        bindings.insertCode(accountId, SessionTokens.hash(code), user.accountId(), expiresAt);
        auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.LOGIN_QR_CREATED)
                .entity(AccountAuditActions.ENTITY, accountId)
                .after(Map.of("expiresAt", expiresAt.toString()))
                .build());
        return new LoginCode(code, expiresAt);
    }

    /** Gỡ thiết bị đang gắn kèm lý do, ví dụ người cao tuổi báo mất máy (FR-070); kết thúc phiên trên thiết bị. */
    @BusinessCommand
    @RequiresPermission(function = Function.RESIDENT_ACCOUNT, operation = Operation.UPDATE)
    @Transactional
    public AccountDetails unbindDevice(long accountId, String ifMatch, Reason reason) {
        CurrentUser user = currentUser.getObject();
        Account account = locked(accountId, ifMatch, user);
        if (devices.unbindActive(accountId, "MANUAL", user.accountId(), reason.value())
                .isEmpty()) {
            throw conflict("NO_BOUND_DEVICE", "Tài khoản không có thiết bị đang gắn");
        }
        store.bumpVersion(account.id());
        return accounts.detail(accountId);
    }

    /**
     * Khóa theo yêu cầu của chính người cao tuổi ({@code RESIDENT_REQUESTED}) hoặc có lý do ({@code WITH_REASON})
     * (FR-072, BR-M15-23): kết thúc phiên, gỡ thiết bị.
     */
    @BusinessCommand
    @RequiresPermission(function = Function.RESIDENT_ACCOUNT, operation = Operation.UPDATE)
    @Transactional
    public AccountDetails lockResident(long accountId, String ifMatch, String basis, Reason reason) {
        CurrentUser user = currentUser.getObject();
        Account account = locked(accountId, ifMatch, user);
        if (basis == null || !LOCK_BASES.contains(basis)) {
            throw new InvalidValueException("Căn cứ khóa là RESIDENT_REQUESTED hoặc WITH_REASON");
        }
        lock(account, basis, user.accountId(), reason, Map.of("basis", basis));
        return accounts.detail(accountId);
    }

    /**
     * Khóa theo đề nghị của người thân khi yêu cầu {@code M15.RESIDENT_ACCOUNT_LOCK} được Quản lý viện duyệt (A-16); gọi
     * trong transaction áp dụng của 000. Tài khoản không còn Hoạt động hoặc Khóa tạm thì không làm gì.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void lockOnFamilyRequest(long accountId, long requestId, Reason reason) {
        Account account = store.lock(accountId).orElseThrow(NotFoundException::new);
        if (account.status() != AccountStatus.ACTIVE && account.status() != AccountStatus.TEMPORARILY_LOCKED) {
            return;
        }
        lock(
                account,
                "FAMILY_REQUEST",
                currentUser.getObject().accountId(),
                reason,
                Map.of("approvalRequestId", requestId));
    }

    /**
     * Mở khóa khi chính người cao tuổi yêu cầu (FR-073): bắt buộc {@code consent}; khóa do kết thúc lưu trú thì 409
     * {@code LOCK_CAUSE_STILL_EXISTS}. Người cao tuổi đăng nhập lại bằng mã QR.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.RESIDENT_ACCOUNT, operation = Operation.UPDATE)
    @Transactional
    public AccountDetails unlockResident(long accountId, String ifMatch, Consent consent) {
        CurrentUser user = currentUser.getObject();
        Account account = locked(accountId, ifMatch, user);
        if (consent == null
                || consent.method() == null
                || !CONSENT_METHODS.contains(consent.method())
                || consent.withStaffId() == null) {
            throw new InvalidValueException(
                    "Cần xác nhận của chính người cao tuổi: cách và nhân viên đã nghe (FR-073)");
        }
        if (account.status() != AccountStatus.LOCKED) {
            throw conflict("INVALID_STATE", "Chỉ mở khóa tài khoản Đã khóa");
        }
        if (AccountService.RESIDENCY_ENDED.equals(account.lockCause()) || finalStatus(account.residentId())) {
            throw conflict("LOCK_CAUSE_STILL_EXISTS", "Tài khoản khóa do kết thúc lưu trú, không mở lại (FR-073)");
        }
        store.changeStatus(accountId, AccountStatus.ACTIVE, null, user.accountId());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("consentMethod", consent.method());
        details.put("consentWithStaffId", consent.withStaffId());
        auditLog.write(AuditLogEntry.builder()
                .action(RESIDENT_UNLOCKED)
                .entity(AccountAuditActions.ENTITY, accountId)
                .before(Map.of("status", AccountStatus.LOCKED.name(), "lockCause", account.lockCause()))
                .after(Map.of("status", AccountStatus.ACTIVE.name()))
                .details(details)
                .build());
        return accounts.detail(accountId);
    }

    /** {@code ACCOUNT_UNLOCKED} không bắt lý do: căn cứ là xác nhận của người cao tuổi ghi ở chi tiết (FR-073). */
    private static final AuditAction RESIDENT_UNLOCKED = new AuditAction("ACCOUNT_UNLOCKED", false);

    private void lock(Account account, String cause, long actorId, Reason reason, Map<String, Object> details) {
        if (account.status() != AccountStatus.ACTIVE && account.status() != AccountStatus.TEMPORARILY_LOCKED) {
            throw conflict("INVALID_STATE", "Chỉ khóa tài khoản Hoạt động hoặc Khóa tạm");
        }
        store.changeStatus(account.id(), AccountStatus.LOCKED, cause, actorId);
        store.endSessions(account.id(), "ACCOUNT_LEFT_ACTIVE");
        devices.unbindActive(account.id(), "ACCOUNT_LOCKED", actorId, null);
        auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.ACCOUNT_LOCKED)
                .entity(AccountAuditActions.ENTITY, account.id())
                .before(Map.of("status", account.status().name()))
                .after(Map.of("status", AccountStatus.LOCKED.name(), "lockCause", cause))
                .reason(reason)
                .details(details)
                .build());
    }

    /** Hành chính, Quản lý viện; Điều dưỡng chỉ tạo mã QR (Phụ lục 27 ⁴⁸). */
    private Account locked(long accountId, String ifMatch, CurrentUser user) {
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER) && !user.roles().contains(Role.OFFICE_STAFF)) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED",
                    "Chỉ Hành chính, Quản lý viện thao tác tài khoản người cao tuổi",
                    Map.of("failedLayer", "ROLE"));
        }
        Account account = store.lock(accountId).orElseThrow(NotFoundException::new);
        requireResident(account);
        VersionEtag.requireMatch(ifMatch, account.version(), accounts.detail(accountId));
        return account;
    }

    /** Người cao tuổi đã ở trạng thái cuối dù sự kiện chưa tới (FR-073: khóa theo (a) không mở được). */
    private boolean finalStatus(Long residentId) {
        ResidentProfileApi api = residentProfiles.getIfAvailable();
        return residentId != null
                && api != null
                && api.profile(residentId).map(ResidentProfileView::finalStatus).orElse(false);
    }

    static Target target(Account account) {
        return new Target(AccountAuditActions.ENTITY, String.valueOf(account.id()), account.residentId(), null);
    }

    private static void requireResident(Account account) {
        if (account.type() != AccountType.RESIDENT) {
            throw conflict("NOT_RESIDENT_ACCOUNT", "Chỉ áp dụng cho tài khoản người cao tuổi");
        }
    }

    private static BusinessException conflict(String errorCode, String message) {
        return new BusinessException(HttpStatus.CONFLICT, errorCode, message);
    }
}

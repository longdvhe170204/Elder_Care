package com.cambae.backend.m15.account.auth.internal;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.AccountStatus;
import com.cambae.backend.m15.account.api.AccountType;
import com.cambae.backend.m15.account.api.AccountView;
import com.cambae.backend.m15.account.api.AssignmentApi;
import com.cambae.backend.m15.account.api.AssignmentKind;
import com.cambae.backend.m15.account.api.AssignmentView;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.DutyApi;
import com.cambae.backend.m15.account.api.FamilyRelationshipApi;
import com.cambae.backend.m15.account.api.Function;
import com.cambae.backend.m15.account.api.Operation;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.ResidencyEndApi;
import com.cambae.backend.m15.account.api.StaffProfileApi;
import com.cambae.backend.m15.account.api.StaffStatus;
import com.cambae.backend.m15.account.auth.internal.AccountStore.AccountRow;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Fallback;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tài khoản nhân viên (002 T031, T032; data-model mục 2 bảng chuyển trạng thái; FR-001 → FR-013): tạo, khóa, mở khóa,
 * cấp lại mật khẩu, khóa kèm Nghỉ việc; mỗi chuyển thêm một dòng lịch sử, rời Hoạt động thì kết thúc mọi phiên trong
 * cùng transaction, ghi nhật ký; mật khẩu không vào nhật ký. Lệnh trên tài khoản nhân viên chỉ Quản lý viện (Hành chính
 * có quyền dòng "Tài khoản, phân quyền, tham số" chỉ với tài khoản người thân, chú thích ¹²; tài khoản người thân ở US6).
 * {@code @Fallback}: bản giả {@code AccountApi} của test 000 vẫn thắng.
 */
@Service
@Fallback
public class AccountService implements AccountApi {

    static final Set<Role> STAFF_ROLES = EnumSet.of(
            Role.NURSING_HOME_MANAGER,
            Role.FLOOR_MANAGER,
            Role.DOCTOR,
            Role.NURSING_STAFF,
            Role.CAREGIVER,
            Role.KITCHEN_STAFF,
            Role.OFFICE_STAFF,
            Role.ACCOUNTANT);
    private static final int PAGE_SIZE = 50;

    private final AccountStore store;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final PasswordEncoder passwords;
    private final AuditLogApi auditLog;
    // ObjectProvider: 008 cài; trước đó chỉ test khai bản giả
    private final ObjectProvider<StaffProfileApi> staffProfiles;
    private final ObjectProvider<DutyApi> duties;
    private final ObjectProvider<FamilyRelationshipApi> familyRelationships;
    private final ObjectProvider<AssignmentApi> assignments;
    private final ObjectProvider<ResidencyEndApi> residencyEnds;
    private final ParameterApi parameters;

    AccountService(
            AccountStore store,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            PasswordEncoder passwords,
            AuditLogApi auditLog,
            ObjectProvider<StaffProfileApi> staffProfiles,
            ObjectProvider<DutyApi> duties,
            ObjectProvider<FamilyRelationshipApi> familyRelationships,
            ObjectProvider<AssignmentApi> assignments,
            ObjectProvider<ResidencyEndApi> residencyEnds,
            ParameterApi parameters) {
        this.store = store;
        this.time = time;
        this.currentUser = currentUser;
        this.passwords = passwords;
        this.auditLog = auditLog;
        this.staffProfiles = staffProfiles;
        this.duties = duties;
        this.familyRelationships = familyRelationships;
        this.assignments = assignments;
        this.residencyEnds = residencyEnds;
        this.parameters = parameters;
    }

    /** Tài khoản kèm vai trò đang gán và lệnh người xem được phép (FR-042). */
    public record AccountDetails(AccountRow account, Set<Role> roles, List<String> allowedCommands) {}

    /** Tài khoản vừa tạo và mật khẩu tạm (chỉ trả một lần). */
    public record Created(AccountDetails details, String temporaryPassword) {}

    public record Page(List<AccountDetails> items, String nextCursor) {}

    /** Lệnh trên tài khoản nhân viên chỉ Quản lý viện (FR-004, FR-011, BR-M15-04). */
    public static void requireManager(CurrentUser user) {
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED", "Chỉ Quản lý viện thực hiện lệnh này", Map.of("failedLayer", "ROLE"));
        }
    }

    // --- Lệnh -------------------------------------------------------------------------------------------------

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.CREATE)
    @Transactional
    public Created createStaff(long staffId, String username, List<Role> roles) {
        CurrentUser user = currentUser.getObject();
        requireManager(user);
        if (roles.isEmpty() || !STAFF_ROLES.containsAll(roles)) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("FR-003", "Tài khoản nhân viên cần ít nhất một vai trò nhân viên")));
        }
        StaffProfileApi profiles = staffProfiles.getObject();
        if (store.byStaff(staffId).isPresent()) {
            throw conflict("PROFILE_ALREADY_HAS_ACCOUNT", "Hồ sơ nhân viên đã có tài khoản (FR-002)");
        }
        if (profiles.status(staffId, time.today()) == StaffStatus.RESIGNED) {
            throw conflict("STAFF_RESIGNED", "Nhân viên đã nghỉ việc (FR-004)");
        }
        if (profiles.positionRole(staffId).isEmpty()) {
            throw conflict("POSITION_NOT_SYSTEM_USER", "Chức danh không dùng hệ thống (FR-004a)");
        }
        String temporaryPassword = TemporaryPasswords.generate();
        long accountId;
        try {
            accountId = store.insertStaff(staffId, username, passwords.encode(temporaryPassword), user.accountId());
        } catch (DuplicateKeyException e) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-001", "Tên đăng nhập đã được dùng")));
        }
        store.history(accountId, AccountStatus.ACTIVE, null, user.accountId());
        for (Role role : roles) {
            store.addRole(accountId, role, user.accountId(), "Tạo tài khoản");
        }
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("username", username);
        after.put("type", AccountType.STAFF.name());
        after.put("staffId", staffId);
        after.put("status", AccountStatus.ACTIVE.name());
        after.put("roles", roles.stream().map(Role::code).sorted().toList());
        auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.ACCOUNT_CREATED)
                .entity(AccountAuditActions.ENTITY, accountId)
                .after(after)
                .build());
        return new Created(details(store.find(accountId).orElseThrow(), user), temporaryPassword);
    }

    /**
     * Tạo tài khoản người thân (002 T069; FR-050 → FR-052; US6 #8): Hành chính hoặc Quản lý viện; xác minh danh tính
     * bắt buộc; hồ sơ có ít nhất một quan hệ Hiệu lực với người cao tuổi chưa ở trạng thái cuối; vai trò duy nhất Người
     * thân; buộc đổi mật khẩu; nhật ký ghi cách xác minh, không ghi số.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.CREATE)
    @Transactional
    public Created createFamily(
            long familyMemberId,
            String username,
            String verificationMethod,
            String idDocumentNumber,
            String calledPhoneNumber) {
        CurrentUser user = currentUser.getObject();
        requireFamilyAccountOperator(user);
        requireVerification(familyMemberId, verificationMethod, idDocumentNumber, calledPhoneNumber);
        if (store.byFamilyMember(familyMemberId).isPresent()) {
            throw conflict("PROFILE_ALREADY_HAS_ACCOUNT", "Hồ sơ người thân đã có tài khoản");
        }
        ResidencyEndApi ends = residencyEnds.getIfAvailable();
        boolean related = familyRelationships.getObject().active(familyMemberId).stream()
                .anyMatch(
                        r -> ends == null || ends.finalStatusAt(r.residentId()).isEmpty());
        if (!related) {
            throw conflict(
                    "RELATIONSHIP_ENDED",
                    "Người thân không có quan hệ với người cao tuổi chưa ở trạng thái cuối (FR-050)");
        }
        String temporaryPassword = TemporaryPasswords.generate();
        long accountId;
        try {
            accountId =
                    store.insertFamily(familyMemberId, username, passwords.encode(temporaryPassword), user.accountId());
        } catch (DuplicateKeyException e) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-001", "Tên đăng nhập đã được dùng")));
        }
        store.history(accountId, AccountStatus.ACTIVE, null, user.accountId());
        store.addRole(accountId, Role.FAMILY_MEMBER, user.accountId(), "Tạo tài khoản");
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("username", username);
        after.put("type", AccountType.FAMILY_MEMBER.name());
        after.put("familyMemberId", familyMemberId);
        after.put("status", AccountStatus.ACTIVE.name());
        after.put("roles", List.of(Role.FAMILY_MEMBER.code()));
        auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.ACCOUNT_CREATED)
                .entity(AccountAuditActions.ENTITY, accountId)
                .after(after)
                .details(Map.of("verificationMethod", verificationMethod))
                .build());
        return new Created(details(store.find(accountId).orElseThrow(), user), temporaryPassword);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public AccountDetails lock(long accountId, String ifMatch, Reason reason, Long replacementDepartmentLead) {
        CurrentUser user = currentUser.getObject();
        requireManager(user);
        AccountRow account = store.lock(accountId).orElseThrow(NotFoundException::new);
        VersionEtag.requireMatch(ifMatch, account.version(), details(account, user));
        if (account.status() == AccountStatus.LOCKED) {
            throw conflict("INVALID_STATE", "Tài khoản đã khóa");
        }
        if (account.status() == AccountStatus.ACTIVE
                && store.roles(accountId).contains(Role.NURSING_HOME_MANAGER)
                && store.otherActiveManagers(accountId) == 0) {
            throw conflict("NO_MANAGER_LEFT", "Viện phải còn ít nhất một Quản lý viện Hoạt động (FR-012)");
        }
        if (account.staffId() != null && duties.getObject().isDepartmentLead(account.staffId())) {
            if (replacementDepartmentLead == null) {
                throw conflict(
                        "REPLACEMENT_LEAD_REQUIRED", "Người này đang là Phụ trách bộ phận, cần người thay (Q-292)");
            }
            duties.getObject().replaceDepartmentLead(account.staffId(), replacementDepartmentLead, reason);
        }
        store.changeStatus(accountId, AccountStatus.LOCKED, "MANUAL", user.accountId());
        store.endSessions(accountId, "ACCOUNT_LEFT_ACTIVE");
        auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.ACCOUNT_LOCKED)
                .entity(AccountAuditActions.ENTITY, accountId)
                .before(Map.of("status", account.status().name()))
                .after(Map.of("status", AccountStatus.LOCKED.name(), "lockCause", "MANUAL"))
                .reason(reason)
                .build());
        return details(store.find(accountId).orElseThrow(), user);
    }

    /**
     * Mở khóa tài khoản Đã khóa; từ chối khi nguyên nhân khóa tự động còn (FR-011): Nghỉ việc mà hồ sơ vẫn Nghỉ việc;
     * hết hạn sau kết thúc lưu trú, hết quan hệ mà người thân vẫn không còn người cao tuổi nào trong phạm vi (CHK025).
     */
    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public AccountDetails unlock(long accountId, String ifMatch, Reason reason) {
        CurrentUser user = currentUser.getObject();
        requireManager(user);
        AccountRow account = store.lock(accountId).orElseThrow(NotFoundException::new);
        VersionEtag.requireMatch(ifMatch, account.version(), details(account, user));
        if (account.status() != AccountStatus.LOCKED) {
            throw conflict("INVALID_STATE", "Chỉ mở khóa tài khoản Đã khóa");
        }
        boolean causeRemains = switch (account.lockCause()) {
            case "MANUAL" -> false;
            case "RESIGNATION" ->
                staffProfiles.getObject().status(account.staffId(), time.today()) == StaffStatus.RESIGNED;
            case "EXPIRED_AFTER_RESIDENCY_END", "RELATIONSHIP_ENDED" -> !stillRelated(account.familyMemberId());
            default -> true;
        };
        if (causeRemains) {
            throw conflict("LOCK_CAUSE_STILL_EXISTS", "Nguyên nhân khóa tự động vẫn còn (FR-011)");
        }
        store.changeStatus(accountId, AccountStatus.ACTIVE, null, user.accountId());
        auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.ACCOUNT_UNLOCKED)
                .entity(AccountAuditActions.ENTITY, accountId)
                .before(Map.of("status", AccountStatus.LOCKED.name(), "lockCause", account.lockCause()))
                .after(Map.of("status", AccountStatus.ACTIVE.name()))
                .reason(reason)
                .build());
        return details(store.find(accountId).orElseThrow(), user);
    }

    /**
     * Cấp lại mật khẩu (FR-006): tài khoản nhân viên chỉ Quản lý viện; tài khoản người thân Hành chính hoặc Quản lý
     * viện, kèm xác minh danh tính, nhật ký ghi cách xác minh (FR-051, FR-052). Hành chính với tài khoản khác: 404 chung.
     */
    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public String resetPassword(
            long accountId,
            Reason reason,
            String verificationMethod,
            String idDocumentNumber,
            String calledPhoneNumber) {
        CurrentUser user = currentUser.getObject();
        requireFamilyAccountOperator(user);
        AccountRow account = store.lock(accountId).orElseThrow(NotFoundException::new);
        boolean family = account.type() == AccountType.FAMILY_MEMBER;
        if (!family && !user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new NotFoundException();
        }
        if (family) {
            requireVerification(account.familyMemberId(), verificationMethod, idDocumentNumber, calledPhoneNumber);
        }
        String temporaryPassword = TemporaryPasswords.generate();
        store.resetPassword(accountId, passwords.encode(temporaryPassword));
        AuditLogEntry.Builder entry = AuditLogEntry.builder()
                .action(AccountAuditActions.PASSWORD_RESET)
                .entity(AccountAuditActions.ENTITY, accountId)
                .after(Map.of("mustChangePassword", true))
                .reason(reason);
        if (family) {
            entry.details(Map.of("verificationMethod", verificationMethod));
        }
        auditLog.write(entry.build());
        return temporaryPassword;
    }

    /**
     * Mở khóa sớm tài khoản Khóa tạm (FR-019c; bảng trạng thái FR-007): Quản lý viện; Trưởng tầng được giao tầng, hoặc
     * Người phụ trách ca của tầng trong ca, với nhân viên có phân công cùng tầng trong ca đang diễn ra. Lý do và xác
     * minh trực tiếp bắt buộc; bộ đếm sai về 0; nhật ký ghi căn cứ. Quyền tự kiểm vì phụ thuộc phân công lúc chạy
     * ({@code RequiresPermissionArchTest.SELF_CHECKED}).
     */
    @BusinessCommand
    @Transactional
    public AccountDetails earlyUnlock(long accountId, String ifMatch, Reason reason, Boolean verifiedInPerson) {
        CurrentUser user = currentUser.getObject();
        if (!Boolean.TRUE.equals(verifiedInPerson)) {
            throw new InvalidValueException("Cần xác minh trực tiếp người dùng trước khi mở khóa sớm (FR-019c)");
        }
        AccountRow account = store.lock(accountId).orElseThrow(NotFoundException::new);
        String authority = earlyUnlockAuthority(user, account);
        VersionEtag.requireMatch(ifMatch, account.version(), details(account, user));
        if (LoginRules.effective(account.status(), account.lockedUntil(), time.now())
                != AccountStatus.TEMPORARILY_LOCKED) {
            throw conflict("INVALID_STATE", "Chỉ mở khóa sớm tài khoản đang Khóa tạm");
        }
        store.changeStatus(accountId, AccountStatus.ACTIVE, null, user.accountId());
        store.resetFailedLogins(accountId);
        auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.ACCOUNT_EARLY_UNLOCKED)
                .entity(AccountAuditActions.ENTITY, accountId)
                .before(Map.of("status", AccountStatus.TEMPORARILY_LOCKED.name()))
                .after(Map.of("status", AccountStatus.ACTIVE.name()))
                .reason(reason)
                .details(Map.of("authority", authority, "verifiedInPerson", true))
                .build());
        return details(store.find(accountId).orElseThrow(), user);
    }

    /**
     * Kích hoạt lại tài khoản người thân Không hoạt động (FR-011, FR-051, FR-052; bảng trạng thái FR-007): Hành chính
     * hoặc Quản lý viện; cách xác minh bắt buộc, gọi lại phải đúng số đã đăng ký; người thân còn quan hệ Hiệu lực với
     * người cao tuổi chưa ở trạng thái cuối hoặc chưa quá CFG-M01-04; buộc đổi mật khẩu.
     */
    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public AccountDetails reactivate(
            long accountId,
            String ifMatch,
            Reason reason,
            String verificationMethod,
            String idDocumentNumber,
            String calledPhoneNumber) {
        CurrentUser user = currentUser.getObject();
        requireFamilyAccountOperator(user);
        if (verificationMethod == null) {
            throw new InvalidValueException("Cần ghi cách xác minh (FR-052)");
        }
        AccountRow account = store.lock(accountId).orElseThrow(NotFoundException::new);
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER) && account.type() != AccountType.FAMILY_MEMBER) {
            throw new NotFoundException();
        }
        VersionEtag.requireMatch(ifMatch, account.version(), details(account, user));
        if (account.type() != AccountType.FAMILY_MEMBER || account.status() != AccountStatus.INACTIVE) {
            throw conflict("INVALID_STATE", "Chỉ kích hoạt lại tài khoản người thân Không hoạt động");
        }
        requireVerification(account.familyMemberId(), verificationMethod, idDocumentNumber, calledPhoneNumber);
        if (!stillRelated(account.familyMemberId())) {
            throw conflict("RELATIONSHIP_ENDED", "Người thân không còn quan hệ Hiệu lực với người cao tuổi (FR-011)");
        }
        store.changeStatus(accountId, AccountStatus.ACTIVE, null, user.accountId());
        store.requirePasswordChange(accountId);
        auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.ACCOUNT_REACTIVATED)
                .entity(AccountAuditActions.ENTITY, accountId)
                .before(Map.of("status", AccountStatus.INACTIVE.name()))
                .after(Map.of("status", AccountStatus.ACTIVE.name(), "mustChangePassword", true))
                .reason(reason)
                .details(Map.of("verificationMethod", verificationMethod))
                .build());
        return details(store.find(accountId).orElseThrow(), user);
    }

    /** Căn cứ mở khóa sớm của người dùng với tài khoản (FR-019c); không đủ thì từ chối. */
    private String earlyUnlockAuthority(CurrentUser user, AccountRow account) {
        if (user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            return Role.NURSING_HOME_MANAGER.name();
        }
        AssignmentApi api = assignments.getIfAvailable();
        if (account.type() == AccountType.STAFF && user.staffId().isPresent() && api != null) {
            Instant now = time.now();
            List<AssignmentView> targetShifts = api.activeAt(account.staffId(), now).stream()
                    .filter(a -> a.floorAreaId() != null && a.shiftId() != null)
                    .toList();
            for (AssignmentView mine : api.activeAt(user.staffId().get(), now)) {
                for (AssignmentView theirs : targetShifts) {
                    if (!theirs.floorAreaId().equals(mine.floorAreaId())) {
                        continue;
                    }
                    if (mine.kind() == AssignmentKind.SHIFT_LEAD
                            && theirs.shiftId().equals(mine.shiftId())) {
                        return AssignmentKind.SHIFT_LEAD.name();
                    }
                    if (mine.kind() == AssignmentKind.FLOOR_MANAGER
                            && user.roles().contains(Role.FLOOR_MANAGER)) {
                        return AssignmentKind.FLOOR_MANAGER.name();
                    }
                }
            }
        }
        throw new PermissionDeniedException(
                "PERMISSION_DENIED",
                "Chỉ Quản lý viện, Trưởng tầng hoặc Người phụ trách ca cùng tầng và ca mở khóa sớm (FR-019c)",
                Map.of("failedLayer", "SCOPE"));
    }

    /** Lệnh trên tài khoản người thân: Hành chính hoặc Quản lý viện (FR-051, chú thích ¹²). */
    private static void requireFamilyAccountOperator(CurrentUser user) {
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER) && !user.roles().contains(Role.OFFICE_STAFF)) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED",
                    "Chỉ Hành chính, Quản lý viện thao tác tài khoản người thân",
                    Map.of("failedLayer", "ROLE"));
        }
    }

    /**
     * Xác minh danh tính trước khi tạo, cấp lại mật khẩu, kích hoạt lại tài khoản người thân (FR-052): thiếu thì 422;
     * gọi lại phải đúng số đã đăng ký trong hồ sơ người thân, có mặt phải ghi số giấy tờ, không thì 409.
     */
    private void requireVerification(
            long familyMemberId, String method, String idDocumentNumber, String calledPhoneNumber) {
        if (method == null) {
            throw new InvalidValueException("Cần ghi cách xác minh (FR-052)");
        }
        boolean verified = switch (method) {
            case "CALLBACK_REGISTERED_NUMBER" ->
                calledPhoneNumber != null
                        && familyRelationships
                                .getObject()
                                .registeredPhoneNumber(familyMemberId)
                                .map(n -> digits(n).equals(digits(calledPhoneNumber)))
                                .orElse(false);
            case "IN_PERSON_ID_DOCUMENT" -> idDocumentNumber != null && !idDocumentNumber.isBlank();
            default -> false;
        };
        if (!verified) {
            throw conflict("INVALID_VERIFICATION", "Xác minh không hợp lệ: phải gọi lại đúng số đã đăng ký (FR-052)");
        }
    }

    /** Điều kiện người thân của FR-011 (CHK025), dùng chung cho kích hoạt lại, mở khóa và khóa tự động (US6). */
    private boolean stillRelated(long familyMemberId) {
        ResidencyEndApi ends = residencyEnds.getIfAvailable();
        Instant now = time.now();
        Duration grace = parameters.read(ParameterCode.CFG_M01_04).asDuration();
        return familyRelationships.getObject().active(familyMemberId).stream()
                .anyMatch(r -> ends == null
                        || ends.finalStatusAt(r.residentId())
                                .map(at -> !at.plus(grace).isBefore(now))
                                .orElse(true));
    }

    private static String digits(String phone) {
        return phone.replaceAll("\\D", "");
    }

    // --- Đọc --------------------------------------------------------------------------------------------------

    /** Chi tiết; người không phải Quản lý viện chỉ xem tài khoản người thân (FR-051), ngoài ra 404 chung. */
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.VIEW)
    public AccountDetails detail(long accountId) {
        CurrentUser user = currentUser.getObject();
        AccountRow account = store.find(accountId).orElseThrow(NotFoundException::new);
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER) && account.type() != AccountType.FAMILY_MEMBER) {
            throw new NotFoundException();
        }
        return details(account, user);
    }

    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.VIEW)
    public Page page(AccountType type, AccountStatus status, String q, String cursor) {
        CurrentUser user = currentUser.getObject();
        AccountType visibleType = user.roles().contains(Role.NURSING_HOME_MANAGER) ? type : AccountType.FAMILY_MEMBER;
        if (type != null && type != visibleType) {
            return new Page(List.of(), null);
        }
        Long after;
        try {
            after = cursor == null || cursor.isBlank() ? null : Long.valueOf(cursor);
        } catch (NumberFormatException e) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "Con trỏ trang không hợp lệ");
        }
        List<AccountRow> rows = store.page(visibleType, status, q, after, PAGE_SIZE + 1);
        boolean more = rows.size() > PAGE_SIZE;
        List<AccountRow> items = more ? rows.subList(0, PAGE_SIZE) : rows;
        return new Page(
                items.stream().map(a -> details(a, user)).toList(),
                more ? String.valueOf(items.getLast().id()) : null);
    }

    private AccountDetails details(AccountRow stored, CurrentUser user) {
        AccountRow account = displayed(stored);
        boolean manager = user.roles().contains(Role.NURSING_HOME_MANAGER);
        List<String> commands = new ArrayList<>();
        if (manager && account.type() == AccountType.STAFF) {
            commands.addAll(
                    account.status() == AccountStatus.LOCKED
                            ? List.of("unlock", "reset-password", "assign-role", "remove-role")
                            : List.of("lock", "reset-password", "assign-role", "remove-role"));
            if (account.status() == AccountStatus.TEMPORARILY_LOCKED) {
                commands.add("early-unlock");
            }
        }
        if (account.type() == AccountType.FAMILY_MEMBER) {
            if (manager) {
                commands.add(account.status() == AccountStatus.LOCKED ? "unlock" : "lock");
            }
            if (manager || user.roles().contains(Role.OFFICE_STAFF)) {
                commands.add("reset-password");
                if (account.status() == AccountStatus.INACTIVE) {
                    commands.add("reactivate");
                }
            }
        }
        return new AccountDetails(account, store.roles(account.id()), List.copyOf(commands));
    }

    /** Khóa tạm đã quá thời điểm mở hiển thị là Hoạt động (research A-03: chuyển khi có lần đăng nhập kế tiếp). */
    private AccountRow displayed(AccountRow a) {
        if (LoginRules.effective(a.status(), a.lockedUntil(), time.now()) == a.status()) {
            return a;
        }
        return new AccountRow(
                a.id(),
                a.username(),
                a.type(),
                a.staffId(),
                a.familyMemberId(),
                AccountStatus.ACTIVE,
                null,
                null,
                a.lastLoginAt(),
                a.version(),
                a.permissionVersion());
    }

    private static BusinessException conflict(String errorCode, String message) {
        return new BusinessException(HttpStatus.CONFLICT, errorCode, message);
    }

    // --- AccountApi -------------------------------------------------------------------------------------------

    /**
     * Khóa kèm Nghỉ việc trong transaction lệnh của 008 (FR-009, BR-M09-04): Đã khóa nguyên nhân Nghỉ việc, kết thúc
     * phiên; nhật ký ghi người thực hiện Bộ lập lịch kèm căn cứ. Khóa tự động không bị chặn bởi FR-012 (research A-05).
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockOnResignation(long staffId, Basis basis) {
        Optional<AccountRow> found = store.byStaff(staffId);
        if (found.isEmpty()) {
            return;
        }
        AccountRow account = store.lock(found.get().id()).orElseThrow();
        if (account.status() == AccountStatus.LOCKED && "RESIGNATION".equals(account.lockCause())) {
            return;
        }
        store.changeStatus(account.id(), AccountStatus.LOCKED, "RESIGNATION", null);
        store.endSessions(account.id(), "ACCOUNT_LEFT_ACTIVE");
        SchedulerUserAspect.runAsScheduler(() -> auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.ACCOUNT_LOCKED)
                .entity(AccountAuditActions.ENTITY, account.id())
                .before(Map.of("status", account.status().name()))
                .after(Map.of("status", AccountStatus.LOCKED.name(), "lockCause", "RESIGNATION"))
                .basis(basis)
                .build()));
    }

    /**
     * Xét lại tài khoản người thân trong transaction lệnh chấm dứt quan hệ của 012 (002 T070; FR-049, BR-M15-14): không
     * còn quan hệ Hiệu lực thì Đã khóa nguyên nhân hết quan hệ; còn quan hệ mà mọi người cao tuổi đã quá CFG-M01-04 sau
     * trạng thái cuối thì nguyên nhân hết hạn. Nhật ký người thực hiện Bộ lập lịch kèm căn cứ của 012.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recheckFamilyMember(long familyMemberId, Basis basis) {
        Optional<AccountRow> found = store.byFamilyMember(familyMemberId);
        if (found.isEmpty() || stillRelated(familyMemberId)) {
            return;
        }
        String cause = familyRelationships.getObject().active(familyMemberId).isEmpty()
                ? "RELATIONSHIP_ENDED"
                : "EXPIRED_AFTER_RESIDENCY_END";
        lockFamily(found.get().id(), cause, basis);
    }

    /**
     * Job khóa sau kết thúc lưu trú (T070; FR-049, US6 #5): người thân còn quan hệ Hiệu lực mà mọi người cao tuổi đã
     * quá CFG-M01-04 sau trạng thái cuối thì Đã khóa nguyên nhân hết hạn. Không còn quan hệ nào thì để lệnh của 012
     * ({@link #recheckFamilyMember}) khóa với căn cứ lần chấm dứt. Trả {@code true} khi đã khóa.
     */
    @Transactional
    public boolean lockIfResidenciesExpired(long accountId, Basis basis) {
        AccountRow account = store.find(accountId).orElseThrow();
        if (account.type() != AccountType.FAMILY_MEMBER
                || familyRelationships
                        .getObject()
                        .active(account.familyMemberId())
                        .isEmpty()
                || stillRelated(account.familyMemberId())) {
            return false;
        }
        return lockFamily(accountId, "EXPIRED_AFTER_RESIDENCY_END", basis);
    }

    /** Khóa tự động tài khoản người thân dưới khóa dòng; đã Đã khóa thì giữ nguyên nguyên nhân cũ. */
    private boolean lockFamily(long accountId, String cause, Basis basis) {
        AccountRow account = store.lock(accountId).orElseThrow();
        if (account.status() == AccountStatus.LOCKED) {
            return false;
        }
        store.changeStatus(accountId, AccountStatus.LOCKED, cause, null);
        store.endSessions(accountId, "ACCOUNT_LEFT_ACTIVE");
        SchedulerUserAspect.runAsScheduler(() -> auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.ACCOUNT_LOCKED)
                .entity(AccountAuditActions.ENTITY, accountId)
                .before(Map.of("status", account.status().name()))
                .after(Map.of("status", AccountStatus.LOCKED.name(), "lockCause", cause))
                .basis(basis)
                .build()));
        return true;
    }

    @Override
    public Optional<AccountView> byStaff(long staffId) {
        return store.byStaff(staffId).map(this::view);
    }

    @Override
    public Optional<AccountView> byFamilyMember(long familyMemberId) {
        return store.byFamilyMember(familyMemberId).map(this::view);
    }

    /** Tài khoản người cao tuổi có từ US8 (T090). */
    @Override
    public Optional<AccountView> byResident(long residentId) {
        return Optional.empty();
    }

    /** Cờ buộc đổi mật khẩu của tài khoản, cho {@code GET /me} (FR-006). */
    public boolean mustChangePassword(long accountId) {
        return store.mustChangePassword(accountId);
    }

    @Override
    public boolean hasRole(long accountId, Role... roles) {
        Set<Role> assigned = store.roles(accountId);
        return Arrays.stream(roles).anyMatch(assigned::contains);
    }

    /** Module chủ hồ sơ (008, 012) chưa có bản cài thì rỗng: họ tên chỉ để hiển thị, không chặn {@code /me}. */
    @Override
    public Optional<String> fullName(long accountId) {
        return store.find(accountId).flatMap(a -> switch (a.type()) {
            case STAFF -> Optional.ofNullable(staffProfiles.getIfAvailable()).flatMap(p -> p.fullName(a.staffId()));
            case FAMILY_MEMBER ->
                Optional.ofNullable(familyRelationships.getIfAvailable())
                        .flatMap(r -> r.familyMemberName(a.familyMemberId()));
            // Người cao tuổi: họ tên qua NguoiCaoTuoiApi của 001 ở T091
            case RESIDENT -> Optional.<String>empty();
        });
    }

    private AccountView view(AccountRow account) {
        return new AccountView(account.id(), account.type(), account.status(), Set.copyOf(store.roles(account.id())));
    }
}

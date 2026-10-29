package com.eldercare.backend.staffing.credential.internal.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.events.CredentialExpiryWarningClosed;
import com.eldercare.backend.staffing.credential.internal.domain.*;
import com.eldercare.backend.staffing.credential.internal.persistence.CredentialStore;
import com.eldercare.backend.staffing.staff.internal.persistence.StaffStore;
import com.eldercare.backend.staffing.staff.internal.service.StaffService;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T006: danh mục và lệnh thêm; gia hạn/thu hồi/sửa sai thuộc T034. */
@Service
public class CredentialService {
    private final CredentialStore store;
    private final StaffService staff;
    private final StaffStore staffStore;
    private final JdbcTemplate jdbc;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;
    private final ObjectProvider<FileApi> files;
    private final CredentialInvalidityEffects effects;
    private final ApplicationEventPublisher events;

    public CredentialService(
            CredentialStore store,
            StaffService staff,
            StaffStore staffStore,
            JdbcTemplate jdbc,
            AuditLogApi audit,
            BusinessTime time,
            ObjectProvider<CurrentUser> users,
            ObjectProvider<FileApi> files,
            CredentialInvalidityEffects effects,
            ApplicationEventPublisher events) {
        this.store = store;
        this.staff = staff;
        this.staffStore = staffStore;
        this.jdbc = jdbc;
        this.audit = audit;
        this.time = time;
        this.users = users;
        this.files = files;
        this.effects = effects;
        this.events = events;
    }

    public List<Credential> list(long staffId) {
        staff.get(staffId);
        return store.staffCredentials(staffId);
    }

    public List<CredentialType> types() {
        return store.types();
    }

    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.CREATE)
    @Transactional
    public CredentialType addType(
            String name, String group, boolean expiry, Set<String> roles, boolean scopes, boolean active) {
        if (!Set.of("PROFESSIONAL_LICENSE", "TRAINING").contains(group)) throw invalid("Nhóm chứng chỉ không hợp lệ");
        for (String role : roles) {
            try {
                Role.valueOf(role);
            } catch (IllegalArgumentException e) {
                throw invalid("Vai trò không hợp lệ");
            }
        }
        long id = store.addType(name, group, expiry, roles, scopes, active, time.now());
        var result = store.type(id).orElseThrow();
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CREDENTIAL_TYPE_CREATED", false))
                .entity("M09.CREDENTIAL_TYPE", id)
                .after(Map.of(
                        "name",
                        name,
                        "group",
                        group,
                        "hasExpiry",
                        expiry,
                        "requiredForRoles",
                        roles,
                        "hasPracticeScopes",
                        scopes,
                        "active",
                        active))
                .build());
        return result;
    }

    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.CREATE)
    @Transactional
    public Credential add(long staffId, CredentialInput input) {
        staffStore.lock(staffId);
        validateInput(input);
        FileApi fileApi = null;
        if (input.scanFileId() != null) {
            fileApi = files.getIfAvailable();
            if (fileApi == null) throw invalid("Dịch vụ tệp chưa sẵn sàng");
        }
        long id = store.insert(staffId, input, users.getObject().accountId(), time.now());
        if (fileApi != null) fileApi.attach(input.scanFileId(), "M09.CREDENTIAL_SCAN", id);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("credentialTypeId", input.credentialTypeId());
        after.put("credentialNumber", input.credentialNumber());
        after.put("issuingAuthority", input.issuingAuthority());
        after.put("practiceScopes", input.practiceScopes());
        after.put("issueDate", input.issueDate());
        after.put("expiryDate", input.expiryDate());
        after.put("scanFileId", input.scanFileId());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CREDENTIAL_ADDED", false))
                .entity("M09.CREDENTIAL", id)
                .after(after)
                .details(Map.of("permissionBasis", "ROLE"))
                .build());
        return store.find(id).orElseThrow();
    }

    private void validateInput(CredentialInput input) {
        var type = store.type(input.credentialTypeId())
                .filter(CredentialType::active)
                .orElseThrow(() -> invalid("Loại chứng chỉ không còn hiệu lực"));
        if (input.issueDate() == null || (type.hasExpiry() && input.expiryDate() == null))
            throw invalid("Thiếu ngày cấp hoặc ngày hết hạn");
        if (input.expiryDate() != null && input.expiryDate().isBefore(input.issueDate()))
            throw invalid("Ngày hết hạn trước ngày cấp");
        if (!type.hasPracticeScopes() && !input.practiceScopes().isEmpty())
            throw invalid("Loại chứng chỉ không có phạm vi hành nghề");
        for (String code : input.practiceScopes())
            if (jdbc.queryForObject(
                            "SELECT COUNT(*) FROM practice_scope WHERE code=? AND status='ACTIVE'", Integer.class, code)
                    == 0) throw invalid("Phạm vi hành nghề không còn hiệu lực");
    }

    private Credential lock(long id) {
        var before = store.find(id).orElseThrow(NotFoundException::new);
        staffStore.lock(before.staffId());
        return store.find(id).orElseThrow(NotFoundException::new);
    }

    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.UPDATE)
    @Transactional
    public Credential renew(long id, CredentialInput input) {
        var before = lock(id);
        if (before.replaced())
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Chứng chỉ đã được thay thế");
        if (before.credentialTypeId() != input.credentialTypeId())
            throw invalid("Gia hạn phải giữ nguyên loại chứng chỉ");
        var added = add(before.staffId(), input);
        store.replaces(added.id(), id);
        jdbc.update("UPDATE credential SET expiry_warning_at=NULL WHERE id=?", id);
        events.publishEvent(new CredentialExpiryWarningClosed(id, time.now()));
        var result = store.find(added.id()).orElseThrow();
        auditChange("CREDENTIAL_RENEWED", before, result, null);
        return result;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.UPDATE)
    @Transactional
    public void revoke(long id, LocalDate effectiveFrom, Reason reason) {
        if (reason == null) throw new ReasonRequiredException();
        if (effectiveFrom == null) throw invalid("Cần ngày hiệu lực thu hồi");
        var before = lock(id);
        if (before.replaced())
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Chứng chỉ đã được thay thế");
        jdbc.update(
                "UPDATE credential SET revoked_from=?,revocation_reason=?,invalidity_applied_at=NULL WHERE id=?",
                effectiveFrom,
                reason.value(),
                id);
        var after = store.find(id).orElseThrow();
        var at = effects.invalidAt(after);
        if (at != null && !at.isAfter(time.now())) effects.apply(after, at);
        auditChange("CREDENTIAL_REVOKED", before, after, reason);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.UPDATE)
    @Transactional
    public void correct(long id, CredentialInput input, Reason reason) {
        if (reason == null) throw new ReasonRequiredException();
        var before = lock(id);
        validateInput(input);
        if (input.scanFileId() != null && !Objects.equals(input.scanFileId(), before.scanFileId()))
            attachFile(input.scanFileId(), id);
        store.correct(id, input);
        var after = store.find(id).orElseThrow();
        var at = effects.invalidAt(after);
        if (before.credentialTypeId() != after.credentialTypeId()) {
            effects.apply(before, time.now());
            // Cờ vừa áp thuộc loại cũ; loại đã đính chính vẫn phải được xử lý khi tới mốc riêng.
            jdbc.update("UPDATE credential SET invalidity_applied_at=NULL WHERE id=?", id);
        }
        if (at != null && !at.isAfter(time.now())) effects.apply(after, at);
        auditChange("CREDENTIAL_CORRECTED", before, after, reason);
    }

    private void attachFile(long file, long id) {
        var api = files.getIfAvailable();
        if (api == null) throw invalid("Dịch vụ tệp chưa sẵn sàng");
        api.attach(file, "M09.CREDENTIAL_SCAN", id);
    }

    private Map<String, Object> snapshot(Credential c) {
        var m = new LinkedHashMap<String, Object>();
        m.put("credentialTypeId", c.credentialTypeId());
        m.put("credentialNumber", c.credentialNumber());
        m.put("issuingAuthority", c.issuingAuthority());
        m.put("practiceScopes", c.practiceScopes());
        m.put("issueDate", c.issueDate());
        m.put("expiryDate", c.expiryDate());
        m.put("scanFileId", c.scanFileId());
        m.put("revokedFrom", c.revokedFrom());
        m.put("replacesId", c.replacesId());
        return m;
    }

    private void auditChange(String action, Credential before, Credential after, Reason reason) {
        var entry = AuditLogEntry.builder()
                .action(new AuditAction(action, false))
                .entity("M09.CREDENTIAL", after.id())
                .before(snapshot(before))
                .after(snapshot(after));
        if (reason != null) entry.reason(reason);
        audit.write(entry.build());
    }

    public List<PracticeScope> practiceScopes() {
        return jdbc.query(
                "SELECT id,name,status,code FROM practice_scope ORDER BY id",
                (r, i) -> new PracticeScope(
                        r.getLong(1), r.getString(2), "ACTIVE".equals(r.getString(3)), r.getString(4)));
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", message);
    }
}

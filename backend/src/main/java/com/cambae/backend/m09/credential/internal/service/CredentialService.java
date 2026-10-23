package com.cambae.backend.m09.credential.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.*;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.credential.internal.domain.*;
import com.cambae.backend.m09.credential.internal.persistence.CredentialStore;
import com.cambae.backend.m09.staff.internal.persistence.StaffStore;
import com.cambae.backend.m09.staff.internal.service.StaffService;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.*;
import com.cambae.backend.m15.auditlog.api.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
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

    public CredentialService(
            CredentialStore store,
            StaffService staff,
            StaffStore staffStore,
            JdbcTemplate jdbc,
            AuditLogApi audit,
            BusinessTime time,
            ObjectProvider<CurrentUser> users,
            ObjectProvider<FileApi> files) {
        this.store = store;
        this.staff = staff;
        this.staffStore = staffStore;
        this.jdbc = jdbc;
        this.audit = audit;
        this.time = time;
        this.users = users;
        this.files = files;
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

    public List<com.cambae.backend.m15.account.api.spi.PracticeScopeView> practiceScopes() {
        return jdbc.query(
                "SELECT id,name,status FROM practice_scope ORDER BY id",
                (r, i) -> new com.cambae.backend.m15.account.api.spi.PracticeScopeView(
                        r.getLong(1), r.getString(2), "ACTIVE".equals(r.getString(3))));
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", message);
    }
}

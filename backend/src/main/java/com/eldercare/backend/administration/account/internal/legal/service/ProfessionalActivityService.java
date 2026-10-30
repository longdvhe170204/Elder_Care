package com.eldercare.backend.administration.account.internal.legal.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.spi.PracticeScopeCatalogApi;
import com.eldercare.backend.administration.account.api.spi.PracticeScopeView;
import com.eldercare.backend.administration.account.internal.auth.service.AccountService;
import com.eldercare.backend.administration.account.internal.legal.domain.ProfessionalActivity;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.web.VersionEtag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục nghiệp vụ chuyên môn (002 T065; data-model mục 4; FR-036, FR-037): chỉ Quản lý viện thêm, sửa, ngừng hiệu
 * lực, bắt buộc lý do, nhật ký {@code PROFESSIONAL_ACTIVITY_*} (tên logic {@code NVCM_*}) trước/sau. Mục phạm vi hành
 * nghề yêu cầu phải là mục đang hiệu lực của danh mục 008 (BR-M15-19). Mục mặc định không ngừng hiệu lực được và không
 * đổi chức năng ({@code DEFAULT_ITEM}); vẫn sửa được loại giấy phép, phạm vi để Quản lý viện cấu hình. Cài
 * {@link ProfessionalActivityApi} cho 008 (FR-030).
 */
@Service
public class ProfessionalActivityService implements ProfessionalActivityApi {

    static final AuditAction CREATED = new AuditAction("PROFESSIONAL_ACTIVITY_CREATED", true);
    static final AuditAction UPDATED = new AuditAction("PROFESSIONAL_ACTIVITY_UPDATED", true);
    static final AuditAction DEACTIVATED = new AuditAction("PROFESSIONAL_ACTIVITY_DEACTIVATED", true);
    static final String ENTITY = "M15.PROFESSIONAL_ACTIVITY";

    private final ProfessionalActivities activities;
    private final ObjectProvider<CurrentUser> currentUser;
    private final AuditLogApi auditLog;
    // ObjectProvider: 008 cài; trước đó chỉ test khai bản giả
    private final ObjectProvider<PracticeScopeCatalogApi> practiceScopes;

    ProfessionalActivityService(
            ProfessionalActivities activities,
            ObjectProvider<CurrentUser> currentUser,
            AuditLogApi auditLog,
            ObjectProvider<PracticeScopeCatalogApi> practiceScopes) {
        this.activities = activities;
        this.currentUser = currentUser;
        this.auditLog = auditLog;
        this.practiceScopes = practiceScopes;
    }

    /** Nội dung của lệnh thêm, sửa. */
    public record Input(
            String name, String function, String requiredLicenseType, Set<Long> practiceScopes, boolean facility) {}

    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.VIEW)
    public List<ProfessionalActivity> list() {
        return activities.all();
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.CREATE)
    @Transactional
    public ProfessionalActivity create(Input input, Reason reason) {
        AccountService.requireManager(currentUser.getObject());
        Function function = function(input.function());
        Set<Long> scopes = practiceScopes(input.practiceScopes());
        if (activities.nameTaken(input.name().strip(), 0)) {
            throw duplicateName();
        }
        long id = activities.insert(
                input.name().strip(), function, input.requiredLicenseType(), input.facility(), scopes);
        ProfessionalActivity created = activities.find(id).orElseThrow();
        audit(CREATED, id, null, snapshot(created), reason);
        return created;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public ProfessionalActivity update(long id, String ifMatch, Input input, Reason reason) {
        AccountService.requireManager(currentUser.getObject());
        ProfessionalActivity before = activities.lock(id).orElseThrow(NotFoundException::new);
        VersionEtag.requireMatch(ifMatch, before.version(), before);
        Function function = function(input.function());
        if (before.isDefault() && function != before.function()) {
            throw new BusinessException(HttpStatus.CONFLICT, "DEFAULT_ITEM", "Không đổi chức năng của mục mặc định");
        }
        Set<Long> scopes = practiceScopes(input.practiceScopes());
        if (before.active() && activities.nameTaken(input.name().strip(), id)) {
            throw duplicateName();
        }
        activities.update(id, input.name().strip(), function, input.requiredLicenseType(), input.facility(), scopes);
        ProfessionalActivity after = activities.find(id).orElseThrow();
        audit(UPDATED, id, snapshot(before), snapshot(after), reason);
        return after;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public ProfessionalActivity deactivate(long id, String ifMatch, Reason reason) {
        AccountService.requireManager(currentUser.getObject());
        ProfessionalActivity before = activities.lock(id).orElseThrow(NotFoundException::new);
        if (before.isDefault()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "DEFAULT_ITEM", "Mục mặc định không ngừng hiệu lực được (FR-036)");
        }
        VersionEtag.requireMatch(ifMatch, before.version(), before);
        if (!before.active()) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STATE", "Mục đã ngừng hiệu lực");
        }
        activities.deactivate(id);
        ProfessionalActivity after = activities.find(id).orElseThrow();
        audit(DEACTIVATED, id, snapshot(before), snapshot(after), reason);
        return after;
    }

    @Override
    public Optional<String> requiredLicenseType(String activityCode) {
        return activities
                .byCode(activityCode)
                .filter(ProfessionalActivity::active)
                .map(ProfessionalActivity::requiredLicenseType);
    }

    private static Function function(String code) {
        try {
            return Function.valueOf(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidValueException("Không có chức năng " + code);
        }
    }

    @Override
    public boolean usesCredentialType(String code) {
        return activities.all().stream().anyMatch(a -> code.equals(a.requiredLicenseType()));
    }

    @Override
    public boolean usesPracticeScope(long id) {
        return activities.all().stream().anyMatch(a -> a.practiceScopes().contains(id));
    }

    private Set<Long> practiceScopes(Set<Long> ids) {
        Set<Long> scopes = ids == null ? Set.of() : Set.copyOf(ids);
        PracticeScopeCatalogApi catalog = practiceScopes.getIfAvailable();
        for (long id : scopes) {
            boolean active = catalog != null
                    && catalog.find(id).map(PracticeScopeView::active).orElse(false);
            if (!active) {
                throw new InvalidValueException("Phạm vi hành nghề " + id + " không có hoặc đã ngừng hiệu lực");
            }
        }
        return scopes;
    }

    private static BusinessException duplicateName() {
        return new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_NAME", "Đã có mục đang hiệu lực cùng tên");
    }

    private static Map<String, Object> snapshot(ProfessionalActivity a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", a.name());
        m.put("function", a.function().name());
        m.put("requiredLicenseType", a.requiredLicenseType());
        m.put("requiredPracticeScopes", a.practiceScopes().stream().sorted().toList());
        m.put("requiresFacilityScope", a.requiresFacilityScope());
        m.put("status", a.active() ? "ACTIVE" : "INACTIVE");
        return m;
    }

    private void audit(
            AuditAction action, long id, Map<String, Object> before, Map<String, Object> after, Reason reason) {
        AuditLogEntry.Builder entry = AuditLogEntry.builder()
                .action(action)
                .entity(ENTITY, id)
                .after(after)
                .reason(reason);
        if (before != null) {
            entry.before(before);
        }
        auditLog.write(entry.build());
    }
}

package com.eldercare.backend.resident.internal.health.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.resident.api.AllergenApi;
import com.eldercare.backend.resident.api.dto.AllergenGroup;
import com.eldercare.backend.resident.api.dto.AllergenView;
import com.eldercare.backend.resident.api.dto.CreateAllergen;
import com.eldercare.backend.resident.internal.health.domain.Allergen;
import com.eldercare.backend.resident.internal.health.persistence.AllergenRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục dị nguyên cho 006, 011 (001 T032; data-model mục 5 "Danh mục"). Tra cứu chỉ đọc; {@link #addAllergen} chỉ cho
 * nhóm thực phẩm và chỉ Bác sĩ (011 FR-003); nhóm hoạt chất và nhóm khác do Quản lý viện cấu hình qua {@code
 * CatalogService}.
 */
@Service
class AllergenApiImpl implements AllergenApi {

    private static final AuditAction CREATED = new AuditAction("CATALOG_CREATED", false);

    private final AllergenRepository allergens;
    private final AuditLogApi auditLog;
    private final ObjectProvider<CurrentUser> currentUser;

    AllergenApiImpl(AllergenRepository allergens, AuditLogApi auditLog, ObjectProvider<CurrentUser> currentUser) {
        this.allergens = allergens;
        this.auditLog = auditLog;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AllergenView> allergen(long id) {
        return allergens.findById(id).map(AllergenApiImpl::view);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllergenView> activeAllergens(AllergenGroup group) {
        return allergens.findByStatusOrderByIdAsc(CatalogStatus.ACTIVE).stream()
                .filter(a -> group == null || a.getGroup() == group)
                .map(AllergenApiImpl::view)
                .toList();
    }

    @Override
    @Transactional
    public long addAllergen(CreateAllergen command) {
        if (!currentUser.getObject().roles().contains(Role.DOCTOR)) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED", "Chỉ Bác sĩ thêm thành phần thực phẩm", Map.of("failedLayer", "ROLE"));
        }
        if (command == null || command.group() != AllergenGroup.FOOD) {
            throw new InvalidValueException("Chỉ thêm được dị nguyên thuộc nhóm thực phẩm");
        }
        if (command.name() == null || command.name().isBlank()) {
            throw new InvalidValueException("Tên dị nguyên là bắt buộc");
        }
        String name = command.name().strip();
        if (allergens.findByNameAndStatus(name, CatalogStatus.ACTIVE).isPresent()) {
            throw duplicateName();
        }
        Allergen created;
        try {
            created = allergens.saveAndFlush(new Allergen(name, AllergenGroup.FOOD));
        } catch (DataIntegrityViolationException e) {
            // UNIQUE (active_name): hai lệnh thêm cùng tên chạy cùng lúc
            throw duplicateName();
        }
        auditLog.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity("M01.ALLERGEN", created.getId())
                .after(Map.of("name", name, "group", AllergenGroup.FOOD.name(), "status", "ACTIVE"))
                .build());
        return created.getId();
    }

    private static AllergenView view(Allergen a) {
        return new AllergenView(a.getId(), a.getName(), a.getGroup());
    }

    private static BusinessException duplicateName() {
        return new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_NAME", "Đã có mục đang hiệu lực cùng tên");
    }
}

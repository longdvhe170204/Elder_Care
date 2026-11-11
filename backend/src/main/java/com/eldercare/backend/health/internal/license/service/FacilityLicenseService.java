package com.eldercare.backend.health.internal.license.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.health.api.dto.FacilityContactView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giấy phép cơ sở có lịch sử và số liên hệ của viện (FR-070, FR-071, FR-075; BR-M06-05; research S-16). Giấy phép không
 * sửa, không xóa: gia hạn, đổi phạm vi, sửa sai đều là bản mới; bản bị thay do sai sót không dùng để xét hiệu lực
 * (đọc ở {@link FacilitySources}). Chỉ Quản lý viện ghi; Quản lý viện và Bác sĩ xem giấy phép.
 */
@Service
public class FacilityLicenseService {
    public static final AuditAction LICENSE_SAVED = new AuditAction("FACILITY_LICENSE_SAVED", false);
    public static final AuditAction LICENSE_REPLACED_FOR_ERROR =
            new AuditAction("FACILITY_LICENSE_REPLACED_FOR_ERROR", true);
    public static final AuditAction CONTACT_CHANGED = new AuditAction("FACILITY_CONTACT_CHANGED", false);

    private static final String CONTACT_CODE = "CURRENT";

    private final CatalogApi catalogs;
    private final AuditLogApi audit;
    private final ObjectProvider<CurrentUser> users;

    public FacilityLicenseService(CatalogApi catalogs, AuditLogApi audit, ObjectProvider<CurrentUser> users) {
        this.catalogs = catalogs;
        this.audit = audit;
        this.users = users;
    }

    /** Một giấy phép khi lưu: trường theo contracts/api-007 {@code FacilityLicense}. */
    public record LicenseInput(
            String number,
            String issuer,
            LocalDate issuedOn,
            LocalDate expiresOn,
            boolean coversMedicalPractice,
            List<String> scopes,
            Long scanFileId,
            Long replaces,
            boolean replacesDueToError,
            String reason) {}

    @Schema(
            name = "FacilityLicenseView",
            requiredProperties = {"number", "issuer", "issuedOn", "expiresOn", "coversMedicalPractice"})
    public record LicenseView(
            long id,
            String number,
            String issuer,
            LocalDate issuedOn,
            LocalDate expiresOn,
            boolean coversMedicalPractice,
            List<String> scopes,
            Long scanFileId,
            Long replaces,
            boolean replacesDueToError,
            String reason,
            boolean active) {}

    @Transactional(readOnly = true)
    public List<LicenseView> list(LocalDate today) {
        var user = users.getObject();
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER) && !user.roles().contains(Role.DOCTOR)) {
            throw new PermissionDeniedException("Chỉ Quản lý viện hoặc Bác sĩ xem giấy phép cơ sở");
        }
        var rows = catalogs.list(FacilitySources.LICENSE, null, false);
        var replacedForError = new java.util.HashSet<Long>();
        for (var row : rows) {
            if (Boolean.TRUE.equals(row.attributes().get("replacementDueToError"))
                    && row.attributes().get("replacesId") instanceof Number id) {
                replacedForError.add(id.longValue());
            }
        }
        return rows.stream().map(row -> view(row, today, replacedForError)).toList();
    }

    @Transactional
    public LicenseView save(LicenseInput input, LocalDate today) {
        manager();
        validate(input);
        if (input.replaces() != null) {
            catalogs.require(FacilitySources.LICENSE, input.replaces());
        }
        var attributes = new LinkedHashMap<String, Object>();
        attributes.put("number", input.number().strip());
        attributes.put("issuer", input.issuer().strip());
        attributes.put("issueDate", input.issuedOn().toString());
        attributes.put("expiryDate", input.expiresOn().toString());
        attributes.put("medicalCare", input.coversMedicalPractice());
        attributes.put("operationScopes", input.scopes() == null ? List.of() : List.copyOf(input.scopes()));
        attributes.put("scanFileId", input.scanFileId());
        attributes.put("replacesId", input.replaces());
        attributes.put("replacementDueToError", input.replacesDueToError());
        attributes.put("reason", input.reason() == null ? null : input.reason().strip());
        String code = input.number().strip() + "@" + input.issuedOn();
        var created = catalogs.create(
                FacilitySources.LICENSE,
                new NewCatalogItem<>(code, "Giấy phép " + input.number().strip(), null, 0, attributes));
        var entry = AuditLogEntry.builder()
                .action(input.replacesDueToError() ? LICENSE_REPLACED_FOR_ERROR : LICENSE_SAVED)
                .entity(FacilitySources.LICENSE.code(), created.id())
                .after(attributes);
        if (input.replacesDueToError()) entry.reason(new Reason(input.reason().strip()));
        audit.write(entry.build());
        return view(created, today, Set.of());
    }

    @Transactional
    public FacilityContactView changeContact(String phoneNumber, String note, int version) {
        manager();
        if (phoneNumber == null || phoneNumber.isBlank()) throw new InvalidValueException("Thiếu số liên hệ");
        var attributes = new LinkedHashMap<String, Object>();
        attributes.put("phoneNumber", phoneNumber.strip());
        attributes.put("note", note == null || note.isBlank() ? null : note.strip());
        var existing = catalogs.findByCode(FacilitySources.CONTACT, null, CONTACT_CODE);
        if (existing.isEmpty()) {
            var created = catalogs.create(
                    FacilitySources.CONTACT,
                    new NewCatalogItem<>(CONTACT_CODE, "Số liên hệ của viện", null, 0, attributes));
            write(created.id(), Map.of(), attributes);
            return new FacilityContactView(phoneNumber.strip(), (String) attributes.get("note"), created.version());
        }
        var change = catalogs.update(
                FacilitySources.CONTACT,
                existing.get().id(),
                version,
                existing.get().name(),
                0,
                attributes);
        write(existing.get().id(), existing.get().attributes(), attributes);
        return new FacilityContactView(
                phoneNumber.strip(),
                (String) attributes.get("note"),
                change.after().version());
    }

    @SuppressWarnings("unchecked")
    private void write(long id, Map before, Map<String, Object> after) {
        audit.write(AuditLogEntry.builder()
                .action(CONTACT_CHANGED)
                .entity(FacilitySources.CONTACT.code(), id)
                .before(before)
                .after(after)
                .build());
    }

    private void manager() {
        if (!users.getObject().roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException("Chỉ Quản lý viện cập nhật giấy phép và số liên hệ của viện (FR-070)");
        }
    }

    private static void validate(LicenseInput input) {
        if (input == null
                || blank(input.number())
                || blank(input.issuer())
                || input.issuedOn() == null
                || input.expiresOn() == null
                || input.expiresOn().isBefore(input.issuedOn())) {
            throw new InvalidValueException(
                    "Giấy phép cần số, cơ quan cấp, ngày cấp và ngày hết hạn không sớm hơn ngày cấp");
        }
        if (input.replacesDueToError() && (input.replaces() == null || blank(input.reason()))) {
            throw new InvalidValueException("Thay thế do sai sót cần chỉ rõ giấy phép bị thay và lý do");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    @SuppressWarnings("unchecked")
    private static LicenseView view(CatalogItem<Map> row, LocalDate today, Set<Long> replacedForError) {
        var a = row.attributes();
        LocalDate issued = LocalDate.parse((String) a.get("issueDate"));
        LocalDate expires = LocalDate.parse((String) a.get("expiryDate"));
        boolean active = !replacedForError.contains(row.id()) && !today.isBefore(issued) && !today.isAfter(expires);
        return new LicenseView(
                row.id(),
                (String) a.get("number"),
                (String) a.get("issuer"),
                issued,
                expires,
                Boolean.TRUE.equals(a.get("medicalCare")),
                a.get("operationScopes") instanceof List<?> scopes ? new ArrayList<>((List<String>) scopes) : List.of(),
                a.get("scanFileId") instanceof Number n ? n.longValue() : null,
                a.get("replacesId") instanceof Number n ? n.longValue() : null,
                Boolean.TRUE.equals(a.get("replacementDueToError")),
                (String) a.get("reason"),
                active);
    }
}

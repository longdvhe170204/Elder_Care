package com.cambae.backend.m02.internal.servicecatalog.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.data.EffectiveVersioning;
import com.cambae.backend.common.data.EffectiveVersioning.VersionedTable;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.VersionMismatchException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.api.dto.BillingPeriod;
import com.cambae.backend.m02.api.dto.ServiceType;
import com.cambae.backend.m02.api.events.PriceVersionCreated;
import com.cambae.backend.m02.internal.servicecatalog.domain.MismatchedContract;
import com.cambae.backend.m02.internal.servicecatalog.domain.PriceVersion;
import com.cambae.backend.m02.internal.servicecatalog.domain.ServiceRecord;
import com.cambae.backend.m02.internal.servicecatalog.persistence.ServiceCatalogStore;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh của Quản lý viện trên danh mục dịch vụ và phiên bản đơn giá (004 US9; research T-05; FR-034, FR-035, FR-036,
 * DBR-08, Q-297, BR-M15-06). Quyền {@code SERVICE_PRICE} chỉ AC-01 được tạo, sửa; AC-09 chỉ xem. Mọi lệnh sửa chạy dưới
 * khóa dòng {@code service}.
 *
 * <p>Loại, mức chăm sóc, loại phòng, thuốc không đổi sau khi tạo (hợp đồng và khoản chi phí đã tham chiếu); kỳ tính không
 * đổi khi đã có phiên bản đơn giá được tham chiếu. Dịch vụ đã ngừng hiệu lực không sửa.
 */
@Service
public class ServiceCatalogService {

    public static final String SERVICE_ENTITY = "M02.SERVICE";
    public static final String PRICE_VERSION_ENTITY = "M02.PRICE_VERSION";

    static final AuditAction CREATED = new AuditAction("M02_SERVICE_CREATED", false);
    static final AuditAction UPDATED = new AuditAction("M02_SERVICE_UPDATED", false);
    static final AuditAction DEACTIVATED = new AuditAction("M02_SERVICE_DEACTIVATED", true);
    static final AuditAction PRICE_VERSION_CREATED = new AuditAction("M02_SERVICE_PRICE_VERSION_CREATED", false);

    private static final VersionedTable PRICE_VERSIONS = new VersionedTable("service", "price_version", "service_id");

    /** Nội dung dịch vụ khi tạo, sửa (contracts/api-004.yaml, {@code ServiceInput}). */
    public record Input(
            ServiceType type,
            String name,
            String unit,
            BillingPeriod billingPeriod,
            CareLevel careLevel,
            Long roomTypeId,
            Long medicationId) {}

    private final ServiceCatalogStore store;
    private final EffectiveVersioning versioning;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    ServiceCatalogService(
            ServiceCatalogStore store,
            EffectiveVersioning versioning,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.versioning = versioning;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.currentUser = currentUser;
    }

    public static Target target(Long serviceId) {
        return new Target(SERVICE_ENTITY, serviceId == null ? null : String.valueOf(serviceId), null, null);
    }

    // ---- Dịch vụ ---------------------------------------------------------------------------------------------------

    @RequiresPermission(function = Function.SERVICE_PRICE, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<ServiceRecord> list(Target target, ServiceType type) {
        return store.list(type);
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.SERVICE_PRICE, operation = Operation.CREATE)
    @Transactional
    public ServiceRecord create(Target target, Input input) {
        requireConsistent(input);
        long id;
        try {
            id = store.insert(new ServiceRecord(
                    0,
                    input.type(),
                    input.name().strip(),
                    input.unit().strip(),
                    input.billingPeriod(),
                    input.careLevel(),
                    input.roomTypeId(),
                    input.medicationId(),
                    true,
                    0));
        } catch (DuplicateKeyException e) {
            throw duplicate(input.type(), input.careLevel());
        }
        ServiceRecord created = store.find(id).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(SERVICE_ENTITY, id)
                .after(snapshot(created))
                .build());
        return created;
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.SERVICE_PRICE, operation = Operation.UPDATE)
    @Transactional
    public ServiceRecord update(Target target, long id, String ifMatch, Input input) {
        requireConsistent(input);
        ServiceRecord current = store.lock(id).orElseThrow(NotFoundException::new);
        VersionEtag.requireMatch(ifMatch, current.version(), current);
        if (!current.active()) {
            throw conflict("SERVICE_INACTIVE", "Dịch vụ đã ngừng hiệu lực, không sửa được");
        }
        requireUnchanged(current, input);
        try {
            if (store.update(
                            id,
                            current.version(),
                            input.name().strip(),
                            input.unit().strip(),
                            input.billingPeriod())
                    == 0) {
                throw new VersionMismatchException(current);
            }
        } catch (DuplicateKeyException e) {
            throw duplicate(current.type(), current.careLevel());
        }
        ServiceRecord updated = store.find(id).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(UPDATED)
                .entity(SERVICE_ENTITY, id)
                .before(snapshot(current))
                .after(snapshot(updated))
                .build());
        return updated;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.SERVICE_PRICE, operation = Operation.UPDATE)
    @Transactional
    public ServiceRecord deactivate(Target target, long id, String ifMatch, Reason reason) {
        ServiceRecord current = store.lock(id).orElseThrow(NotFoundException::new);
        VersionEtag.requireMatch(ifMatch, current.version(), current);
        if (!current.active()) {
            throw conflict("SERVICE_ALREADY_INACTIVE", "Dịch vụ đã ngừng hiệu lực");
        }
        store.deactivate(id, current.version());
        ServiceRecord updated = store.find(id).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(DEACTIVATED)
                .entity(SERVICE_ENTITY, id)
                .before(snapshot(current))
                .after(snapshot(updated))
                .reason(reason)
                .build());
        return updated;
    }

    // ---- Phiên bản đơn giá -----------------------------------------------------------------------------------------

    @RequiresPermission(function = Function.SERVICE_PRICE, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<PriceVersion> priceVersions(Target target, long serviceId) {
        store.find(serviceId).orElseThrow(NotFoundException::new);
        return store.versions(serviceId);
    }

    /**
     * Thêm phiên bản đơn giá (FR-035, DBR-08). Không có {@code to} thì còn mở: nếu dịch vụ đang có phiên bản mở bắt đầu
     * trước {@code from}, phiên bản đó đóng ở ngày liền trước và {@code from} phải sau hôm nay (không rút ngắn phần đã áp
     * dụng); nếu không thì đây là hiệu lực lùi vào khoảng chưa có phiên bản và bị cắt ở ngày liền trước phiên bản kế
     * tiếp. Có {@code to} thì khoảng đó không được giao phiên bản nào.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.SERVICE_PRICE, operation = Operation.CREATE)
    @Transactional
    public PriceVersion createPriceVersion(
            Target target, long serviceId, BigDecimal unitPrice, LocalDate from, LocalDate to) {
        if (unitPrice.signum() < 0 || unitPrice.stripTrailingZeros().scale() > 0) {
            throw new InvalidValueException("unitPrice phải là số tiền nguyên không âm");
        }
        if (to != null && to.isBefore(from)) {
            throw new InvalidValueException("effectiveTo không được trước effectiveFrom");
        }
        store.lock(serviceId).orElseThrow(NotFoundException::new);
        List<PriceVersion> existing = store.versions(serviceId);
        PriceVersion open = existing.stream()
                .filter(version -> version.effectiveTo() == null)
                .findFirst()
                .orElse(null);
        long[] created = new long[1];
        LocalDate storedTo = to;
        boolean closesOpen = to == null && open != null && open.effectiveFrom().isBefore(from);
        if (to == null && !closesOpen) {
            storedTo = existing.stream()
                    .map(PriceVersion::effectiveFrom)
                    .filter(start -> start.isAfter(from))
                    .min(Comparator.naturalOrder())
                    .map(start -> start.minusDays(1))
                    .orElse(null);
        }
        LocalDate insertedTo = storedTo;
        Runnable insert = () -> created[0] = store.insertVersion(serviceId, unitPrice, from, insertedTo);
        if (closesOpen) {
            if (!from.isAfter(time.today())) {
                throw conflict(
                        "PRICE_VERSION_OVERLAPS_APPLIED",
                        "Phiên bản mở đã áp dụng tới hôm nay; phiên bản mới phải có hiệu lực sau hôm nay (DBR-08)");
            }
            versioning.addVersionFrom(PRICE_VERSIONS, serviceId, from, insert);
        } else {
            versioning.addVersion(PRICE_VERSIONS, serviceId, from, storedTo, insert);
        }
        PriceVersion version = store.findVersion(created[0]).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(PRICE_VERSION_CREATED)
                .entity(PRICE_VERSION_ENTITY, version.id())
                .after(snapshot(version))
                .build());
        events.publishEvent(new PriceVersionCreated(serviceId, version.effectiveFrom(), version.effectiveTo()));
        return version;
    }

    /** Hợp đồng Hiệu lực có đơn giá đã ghi khác phiên bản mới nhất (FR-036); chỉ để xem, không đổi hợp đồng. */
    @RequiresPermission(function = Function.SERVICE_PRICE, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<MismatchedContract> mismatchedContracts(Target target, long serviceId) {
        if (!currentUser.getObject().roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException("Chỉ Quản lý viện xem hợp đồng lệch đơn giá (FR-036)");
        }
        store.find(serviceId).orElseThrow(NotFoundException::new);
        return store.versions(serviceId).stream()
                .max(Comparator.comparing(PriceVersion::effectiveFrom))
                .map(latest -> store.mismatchedContracts(serviceId, latest.unitPrice()))
                .orElse(List.of());
    }

    // ---- Kiểm tra --------------------------------------------------------------------------------------------------

    /** Q-297: dịch vụ chăm sóc gắn đúng một mức; phí lưu trú gắn loại phòng, thuốc nguồn viện gắn thuốc; loại khác không gắn. */
    private static void requireConsistent(Input input) {
        boolean care = input.type() == ServiceType.CARE;
        if (care != (input.careLevel() != null)) {
            throw new InvalidValueException(
                    care
                            ? "Dịch vụ chăm sóc phải gắn đúng một mức (careLevelCode, Q-297)"
                            : "Chỉ dịch vụ chăm sóc mới có mức (careLevelCode)");
        }
        boolean fee = input.type() == ServiceType.RESIDENCY_FEE;
        if (fee != (input.roomTypeId() != null)) {
            throw new InvalidValueException(
                    fee
                            ? "Phí lưu trú phải gắn loại phòng (roomTypeId)"
                            : "Chỉ phí lưu trú mới có loại phòng (roomTypeId)");
        }
        boolean medication = input.type() == ServiceType.FACILITY_SUPPLIED_MEDICATION;
        if (medication != (input.medicationId() != null)) {
            throw new InvalidValueException(
                    medication
                            ? "Thuốc nguồn viện phải gắn thuốc (medicationId)"
                            : "Chỉ thuốc nguồn viện mới có thuốc (medicationId)");
        }
    }

    private void requireUnchanged(ServiceRecord current, Input input) {
        boolean sameLinks = current.type() == input.type()
                && Objects.equals(current.careLevel(), input.careLevel())
                && Objects.equals(current.roomTypeId(), input.roomTypeId())
                && Objects.equals(current.medicationId(), input.medicationId());
        if (!sameLinks) {
            throw conflict(
                    "SERVICE_FIELD_LOCKED",
                    "Loại dịch vụ, mức chăm sóc, loại phòng, thuốc không đổi sau khi tạo; ngừng hiệu lực rồi tạo dịch vụ mới");
        }
        if (current.billingPeriod() != input.billingPeriod() && store.hasReferencedVersion(current.id())) {
            throw conflict("SERVICE_FIELD_LOCKED", "Kỳ tính không đổi khi đơn giá của dịch vụ đã được tham chiếu");
        }
    }

    private static BusinessException duplicate(ServiceType type, CareLevel level) {
        return type == ServiceType.CARE
                ? conflict(
                        "CARE_SERVICE_LEVEL_EXISTS",
                        "Mức " + level + " đã có một dịch vụ chăm sóc Hiệu lực; ngừng dịch vụ đó trước (Q-297)")
                : conflict("SERVICE_NAME_EXISTS", "Đã có dịch vụ Hiệu lực trùng tên (BR-M15-06)");
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    private static Map<String, Object> snapshot(ServiceRecord service) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", service.type().name());
        map.put("name", service.name());
        map.put("unit", service.unit());
        map.put("billingPeriod", service.billingPeriod().name());
        map.put(
                "careLevelCode",
                service.careLevel() == null ? null : service.careLevel().name());
        map.put("roomTypeId", service.roomTypeId());
        map.put("medicationId", service.medicationId());
        map.put("status", service.active() ? "ACTIVE" : "INACTIVE");
        return map;
    }

    private static Map<String, Object> snapshot(PriceVersion version) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("serviceId", version.serviceId());
        map.put("unitPrice", version.unitPrice().toPlainString());
        map.put("effectiveFrom", version.effectiveFrom().toString());
        map.put(
                "effectiveTo",
                version.effectiveTo() == null ? null : version.effectiveTo().toString());
        return map;
    }
}

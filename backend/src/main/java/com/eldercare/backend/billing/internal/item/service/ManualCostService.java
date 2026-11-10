package com.eldercare.backend.billing.internal.item.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.billing.api.events.CostItemChanged;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.item.domain.ManualCost;
import com.eldercare.backend.billing.internal.item.persistence.ManualCostStore;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.period.service.CostLock;
import com.eldercare.backend.billing.internal.pricing.domain.DongRounding;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-021, BR-M11-04: khoản nhập tay chỉ cho mục ngoài danh mục dịch vụ, vật phẩm (kể cả mục Ngừng hiệu lực). Tên trùng mục
 * danh mục sau khi bỏ khoảng trắng thừa, không phân biệt hoa thường, giữ dấu thì bị chặn (Q4, A duyệt 2026-11-10).
 */
@Service
@Transactional
public class ManualCostService {
    public static final String EVIDENCE_OWNER = "M11.COST_ITEM_EVIDENCE";

    private final CostAccess access;
    private final CostLock locks;
    private final CostDestination destination;
    private final CostStatementStore statements;
    private final ManualCostStore store;
    private final UnitPriceApi catalog;
    private final FileApi files;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public ManualCostService(
            CostAccess access,
            CostLock locks,
            CostDestination destination,
            CostStatementStore statements,
            ManualCostStore store,
            UnitPriceApi catalog,
            FileApi files,
            AuditLogApi audit,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.access = access;
        this.locks = locks;
        this.destination = destination;
        this.statements = statements;
        this.store = store;
        this.catalog = catalog;
        this.files = files;
        this.audit = audit;
        this.time = time;
        this.events = events;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.COST_ADJUSTMENT, operation = Operation.CREATE)
    public long create(
            long residentId,
            String description,
            LocalDate incurredOn,
            BigDecimal quantity,
            String unit,
            long unitPrice,
            Reason reason,
            List<Long> fileIds) {
        long actor = access.require(
                Role.OFFICE_STAFF, Function.COST_ADJUSTMENT, Operation.CREATE, "M01.RESIDENT", residentId, residentId);
        List<Long> evidence = fileIds == null ? List.of() : List.copyOf(fileIds);
        if (residentId <= 0
                || description == null
                || description.isBlank()
                || description.length() > 2000
                || incurredOn == null
                || incurredOn.isAfter(time.today())
                || quantity == null
                || quantity.signum() <= 0
                || quantity.stripTrailingZeros().scale() > 3
                || quantity.compareTo(new BigDecimal("999999999.999")) > 0
                || unit == null
                || unit.isBlank()
                || unit.length() > 40
                || unitPrice < 0
                || reason == null
                || evidence.stream().anyMatch(id -> id == null || id <= 0)
                || evidence.stream().distinct().count() != evidence.size())
            throw new InvalidValueException("Thông tin khoản nhập tay chưa hợp lệ");
        var same = catalog.servicesNamed(description);
        if (!same.isEmpty())
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CONDITION_NOT_MET",
                    "Mục có trong danh mục: " + same.getFirst().name()
                            + "; lập khoản theo bản ghi nguồn hoặc đề nghị mua hộ");
        locks.resident(residentId);
        var target = destination.finalizedFinalPeriod(residentId).isPresent()
                ? destination.adjustment(residentId, actor)
                : destination.regular(residentId, YearMonth.from(incurredOn), actor);
        if (target.status().equals("FINALIZED")) target = destination.adjustment(residentId, actor);
        var amount = DongRounding.round(BigDecimal.valueOf(unitPrice).multiply(quantity));
        long id = store.create(
                new ManualCost(
                        residentId,
                        target.id(),
                        target.periodId(),
                        description.strip(),
                        incurredOn,
                        quantity,
                        unit.strip(),
                        unitPrice,
                        amount,
                        reason.value(),
                        evidence),
                actor,
                time.now());
        evidence.forEach(file -> files.attach(file, EVIDENCE_OWNER, id));
        statements.touch(target.id(), true);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("COST_ITEM_MANUAL_CREATE", false))
                .entity("M11.COST_ITEM", id)
                .resident(residentId)
                .after(Map.of("description", description.strip(), "amount", amount, "fileIds", evidence))
                .build());
        events.publishEvent(new CostItemChanged(residentId, target.periodId()));
        return id;
    }
}

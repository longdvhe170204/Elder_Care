package com.eldercare.backend.billing.internal.purchase.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.billing.api.dto.PurchaseAssistanceStatus;
import com.eldercare.backend.billing.api.events.*;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.item.service.CostDestination;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.period.service.CostLock;
import com.eldercare.backend.billing.internal.purchase.domain.*;
import com.eldercare.backend.billing.internal.purchase.persistence.PurchaseCostStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PurchaseRecordingService {
    private final SimpleRecordApi records;
    private final com.eldercare.backend.administration.shared.api.RecordEventApi recordEvents;
    private final CostAccess access;
    private final CostLock locks;
    private final CostDestination destination;
    private final CostStatementStore statements;
    private final PurchaseCostStore costs;
    private final ParameterApi parameters;
    private final FileApi files;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public PurchaseRecordingService(
            SimpleRecordApi records,
            com.eldercare.backend.administration.shared.api.RecordEventApi recordEvents,
            CostAccess access,
            CostLock locks,
            CostDestination destination,
            CostStatementStore statements,
            PurchaseCostStore costs,
            ParameterApi parameters,
            FileApi files,
            AuditLogApi audit,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.records = records;
        this.recordEvents = recordEvents;
        this.access = access;
        this.locks = locks;
        this.destination = destination;
        this.statements = statements;
        this.costs = costs;
        this.parameters = parameters;
        this.files = files;
        this.audit = audit;
        this.time = time;
        this.events = events;
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.PURCHASE_ON_BEHALF, operation = Operation.UPDATE)
    public void record(long id, long actual, LocalDate date, long file, boolean urgent, Long requester) {
        var initial = records.require(PurchaseDetails.KIND, id);
        long actor = access.require(
                Role.OFFICE_STAFF,
                Function.PURCHASE_ON_BEHALF,
                Operation.UPDATE,
                PurchaseDetails.KIND.code(),
                id,
                initial.residentId());
        if (actual < 0 || actual > 999999999999999L || date == null || date.isAfter(time.today()) || file <= 0)
            throw new InvalidValueException("Số tiền, ngày mua hoặc chứng từ không hợp lệ");
        locks.resident(initial.residentId());
        var row = records.lockForUpdate(PurchaseDetails.KIND, id);
        if (!row.status().equals("PURCHASE_ALLOWED"))
            throw new BusinessException(
                    HttpStatus.CONFLICT, "ALREADY_PROCESSED", "Đề nghị chưa được phép mua hoặc đã kết thúc");
        if (urgent || requester != null || row.content().catalogItemId() != null)
            throw PurchaseAssistanceService.unavailableCatalog();
        var result = PurchaseExcessCheck.evaluate(
                actual,
                row.content().consent().allowedAmount(),
                row.content().consent().threshold(),
                parameters.read(ParameterCode.CFG_M11_06).asPercent());
        if (result == PurchaseExcessCheck.Result.ADDITIONAL_CONSENT_REQUIRED)
            throw new BusinessException(
                    HttpStatus.CONFLICT, "CONDITION_NOT_MET", "Cần xin đồng ý bổ sung trước khi ghi đã mua");
        var target = destination.finalizedFinalPeriod(row.residentId()).isPresent()
                ? destination.adjustment(row.residentId(), actor)
                : destination.regular(row.residentId(), YearMonth.from(date), actor);
        if (target.status().equals("FINALIZED")) target = destination.adjustment(row.residentId(), actor);
        var content = row.content()
                .purchased(actual, date, file, access.user().staffId().orElseThrow());
        long item = costs.create(
                id,
                row.residentId(),
                target.id(),
                target.periodId(),
                content,
                actor,
                time.now(),
                result == PurchaseExcessCheck.Result.EXCESS);
        var receipt = recordEvents.add(
                PurchaseReceipt.KIND,
                new com.eldercare.backend.administration.shared.api.dto.EntityRef(PurchaseDetails.KIND.code(), id),
                new com.eldercare.backend.administration.shared.api.dto.NewRecordEvent<>(
                        new PurchaseReceipt(file),
                        null,
                        actor,
                        false,
                        time.now(),
                        null,
                        time.now(),
                        null,
                        "M11.PURCHASE_RECEIPT:" + id));
        files.attach(file, PurchaseReceipt.KIND.code(), receipt.id());
        records.change(
                PurchaseDetails.KIND, id, row.version(), new RecordChange<>("PURCHASED", content, null, null, true));
        statements.touch(target.id(), true);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("PURCHASE_ASSISTANCE_RECORD_PURCHASED", false))
                .entity(PurchaseDetails.KIND.code(), id)
                .resident(row.residentId())
                .before(Map.of("status", row.status()))
                .after(Map.of("status", "PURCHASED", "actualAmount", actual, "receiptFileId", file, "costItemId", item))
                .build());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("COST_ITEM_PURCHASE_ASSISTANCE", false))
                .entity("M11.COST_ITEM", item)
                .resident(row.residentId())
                .after(Map.of("purchaseAssistanceId", id, "amount", actual))
                .build());
        events.publishEvent(new CostItemChanged(row.residentId(), target.periodId()));
        events.publishEvent(new PurchaseAssistancePurchased(
                id,
                row.residentId(),
                row.content().consent().estimatedAmount(),
                PurchaseAssistanceStatus.PURCHASED,
                List.of()));
    }
}

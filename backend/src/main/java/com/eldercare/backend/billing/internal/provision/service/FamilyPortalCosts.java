package com.eldercare.backend.billing.internal.provision.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.billing.api.FamilyPortalCostApi;
import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.provision.persistence.CostProvisionStore;
import com.eldercare.backend.billing.internal.purchase.service.PurchaseAssistanceService;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.PrescriptionApi;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FamilyPortalCosts implements FamilyPortalCostApi {
    private final CostStatementStore statements;
    private final FinalizedCostViews views;
    private final CostSummaries summaries;
    private final PermissionApi permissions;
    private final CostAccess access;
    private final BusinessTime time;
    private final PurchaseAssistanceService purchases;
    private final FieldVisibilityApi fields;
    private final PrescriptionApi prescriptions;
    private final CostProvisionStore source;

    public FamilyPortalCosts(
            CostStatementStore statements,
            FinalizedCostViews views,
            CostSummaries summaries,
            PermissionApi permissions,
            CostAccess access,
            BusinessTime time,
            PurchaseAssistanceService purchases,
            FieldVisibilityApi fields,
            PrescriptionApi prescriptions,
            CostProvisionStore source) {
        this.statements = statements;
        this.views = views;
        this.summaries = summaries;
        this.permissions = permissions;
        this.access = access;
        this.time = time;
        this.purchases = purchases;
        this.fields = fields;
        this.prescriptions = prescriptions;
        this.source = source;
    }

    private void require(long resident, CurrentUser user) {
        if (user.accountId() != access.user().accountId()
                || user.type() != AccountType.FAMILY_MEMBER
                || !permissions
                        .hasPermission(
                                user,
                                new PermissionKey(Function.COST_ADJUSTMENT, Operation.VIEW),
                                new Target("M01.RESIDENT", Long.toString(resident), resident, null))
                        .allowed())
            throw new PermissionDeniedException("Không có quyền xem chi phí của người cao tuổi này");
    }

    public List<CostStatementView> finalizedStatements(long resident, CurrentUser user) {
        require(resident, user);
        var result = statements.list(null, resident, "FINALIZED").stream()
                .map(views::statement)
                .toList();
        if (fields.canView(
                        user, InfoGroup.MEDICATION, new Target("M01.RESIDENT", Long.toString(resident), resident, null))
                != Visibility.FULL) return result;
        Map<Long, String> names = new HashMap<>();
        prescriptions.history(resident).forEach(p -> {
            if (p.medication() != null)
                names.put(p.medication().medicationId(), p.medication().name());
        });
        return result.stream()
                .map(s -> new CostStatementView(
                        s.id(),
                        s.period(),
                        s.residentId(),
                        s.statementType(),
                        s.finalPeriod(),
                        s.throughDate(),
                        s.status(),
                        s.totalsByType(),
                        s.total(),
                        s.items().stream()
                                .map(i -> withMedicationName(i, names))
                                .toList()))
                .toList();
    }

    private CostItemView withMedicationName(CostItemView i, Map<Long, String> names) {
        if (i.costType() != CostType.MEDICATION) return i;
        String name = source.medicationId(i.id()).map(names::get).orElse(null);
        if (name == null) return i;
        return new CostItemView(
                i.id(),
                i.costType(),
                i.origin(),
                i.billingSource(),
                i.description(),
                i.itemCode(),
                name,
                i.incurredOn(),
                i.quantity(),
                i.unit(),
                i.unitPrice(),
                i.unitPriceBasis(),
                i.factor(),
                i.amount(),
                i.flags(),
                i.status(),
                i.originalItemId(),
                i.originalPeriod(),
                i.recalculationHistory(),
                i.commandHistory(),
                i.adjustments(),
                i.currentTotal(),
                List.of());
    }

    public ProvisionalCostSummaryView provisional(long resident, LocalDate from, LocalDate to, CurrentUser user) {
        require(resident, user);
        var month = YearMonth.from(time.today());
        var start = from == null ? month.atDay(1) : from;
        var end = to == null ? time.today() : to;
        if (end.isBefore(start)
                || !YearMonth.from(start).equals(month)
                || !YearMonth.from(end).equals(month))
            throw new InvalidValueException("Khoảng ngày phải nằm trong kỳ đang mở");
        return summaries.provisional(resident, start, end);
    }

    public List<PurchaseAssistanceView> awaitingConsent(long resident, long member) {
        if (access.user().familyMemberId().orElse(-1L) != member)
            throw new PermissionDeniedException("Sai người quyết định");
        return purchases.list(resident, "AWAITING_CONSENT").stream()
                .map(r -> {
                    var c = r.content();
                    return new PurchaseAssistanceView(
                            r.id(),
                            r.residentId(),
                            c.catalogItemId(),
                            c.description(),
                            c.quantity(),
                            c.consent().estimatedAmount(),
                            c.reason(),
                            new PurchaseRequesterView(c.requesterType(), c.requesterId()),
                            PurchaseAssistanceStatus.valueOf(r.status()),
                            c.consent().allowedAmount(),
                            c.consent().additionalPending(),
                            c.insufficientBalance(),
                            List.of());
                })
                .toList();
    }
}

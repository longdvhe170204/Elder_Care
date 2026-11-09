package com.eldercare.backend.billing.internal.purchase.service;

import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.purchase.domain.PurchaseDetails;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.file.FilePermissionProvider;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PurchaseReceiptPermission implements FilePermissionProvider {
    private final SimpleRecordApi records;
    private final com.eldercare.backend.administration.shared.api.RecordEventApi events;
    private final CostAccess access;
    private final RepresentativePort representatives;
    private final BusinessTime time;

    public PurchaseReceiptPermission(
            SimpleRecordApi records,
            com.eldercare.backend.administration.shared.api.RecordEventApi events,
            CostAccess access,
            RepresentativePort representatives,
            BusinessTime time) {
        this.records = records;
        this.events = events;
        this.access = access;
        this.representatives = representatives;
        this.time = time;
    }

    public String ownerType() {
        return "M11.PURCHASE_ASSISTANCE_RECEIPT";
    }

    public boolean canDownload(long id) {
        try {
            var receipt =
                    events.require(com.eldercare.backend.billing.internal.purchase.domain.PurchaseReceipt.KIND, id);
            var row = records.require(PurchaseDetails.KIND, receipt.parent().entityId());
            var user = access.user();
            if (user.type() == AccountType.FAMILY_MEMBER) {
                if (user.familyMemberId().isEmpty()
                        || !representatives.isActiveRepresentative(
                                user.familyMemberId().orElseThrow(), row.residentId(), time.today())) return false;
                access.viewOrCommand(
                        Function.PURCHASE_ON_BEHALF,
                        Operation.VIEW,
                        PurchaseDetails.KIND.code(),
                        row.id(),
                        row.residentId());
            } else {
                if (user.roles().stream()
                        .noneMatch(Set.of(Role.OFFICE_STAFF, Role.NURSING_HOME_MANAGER, Role.ACCOUNTANT)::contains))
                    return false;
                access.viewOrCommand(
                        Function.COST_ADJUSTMENT,
                        Operation.VIEW,
                        PurchaseDetails.KIND.code(),
                        row.id(),
                        row.residentId());
            }
            return true;
        } catch (PermissionDeniedException | NotFoundException ex) {
            return false;
        }
    }
}

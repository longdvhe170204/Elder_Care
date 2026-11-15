package com.eldercare.backend.billing.internal.item.service;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.item.persistence.CostItemStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FilePermissionProvider;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Bằng chứng của khoản nhập tay (FR-021): người xem được khoản chi phí của người cao tuổi đó (Hành chính, Quản lý viện, Kế toán). */
@Component
class CostItemEvidencePermission implements FilePermissionProvider {
    private final CostItemStore items;
    private final CostAccess access;

    CostItemEvidencePermission(CostItemStore items, CostAccess access) {
        this.items = items;
        this.access = access;
    }

    @Override
    public String ownerType() {
        return ManualCostService.EVIDENCE_OWNER;
    }

    @Override
    public boolean canDownload(long ownerId) {
        if (access.user().roles().stream()
                .noneMatch(Set.of(Role.OFFICE_STAFF, Role.NURSING_HOME_MANAGER, Role.ACCOUNTANT)::contains))
            return false;
        try {
            long resident = items.item(ownerId).residentId();
            access.viewOrCommand(Function.COST_ADJUSTMENT, Operation.VIEW, "M11.COST_ITEM", ownerId, resident);
            return true;
        } catch (RuntimeException denied) {
            return false;
        }
    }
}

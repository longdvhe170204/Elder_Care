package com.eldercare.backend.billing.balance.internal.settlement.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementRecords;
import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * {@code QuyenTepProvider} của bản scan có chữ ký ({@code M11.SETTLEMENT_SIGNED_COPY}; 017 T007 phần B, FR-034; research
 * E-12): cùng quyền với bảng quyết toán. Chủ sở hữu tệp là dòng {@code record_event} của bản ký.
 */
@Component
class SettlementSignedCopyFiles implements FilePermissionProvider {

    private final RecordEventApi records;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> user;

    SettlementSignedCopyFiles(RecordEventApi records, PermissionApi permissions, ObjectProvider<CurrentUser> user) {
        this.records = records;
        this.permissions = permissions;
        this.user = user;
    }

    @Override
    public String ownerType() {
        return SettlementRecords.SIGNED_COPY.code();
    }

    @Override
    public boolean canDownload(long ownerId) {
        long residentId =
                records.require(SettlementRecords.SIGNED_COPY, ownerId).parent().entityId();
        return permissions
                .hasPermission(user.getObject(), SettlementStatementFiles.VIEW, BalanceQueries.target(residentId))
                .allowed();
    }
}

package com.eldercare.backend.billing.balance.internal.settlement.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementRecords;
import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * {@code QuyenTepProvider} của PDF bảng quyết toán ({@code M11.SETTLEMENT_STATEMENT}; 017 T007 phần B, FR-028, FR-037;
 * research E-12): tải được khi người xem có quyền xem {@code SETTLEMENT_RECORD} của người cao tuổi đó (Kế toán, Quản lý
 * viện, người thân có quyền xem chi phí). Chủ sở hữu tệp là dòng {@code record_event} của phiên bản.
 */
@Component
class SettlementStatementFiles implements FilePermissionProvider {

    static final PermissionKey VIEW = new PermissionKey(Function.SETTLEMENT_RECORD, Operation.VIEW);

    private final RecordEventApi records;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> user;

    SettlementStatementFiles(RecordEventApi records, PermissionApi permissions, ObjectProvider<CurrentUser> user) {
        this.records = records;
        this.permissions = permissions;
        this.user = user;
    }

    @Override
    public String ownerType() {
        return SettlementRecords.STATEMENT.code();
    }

    @Override
    public boolean canDownload(long ownerId) {
        long residentId =
                records.require(SettlementRecords.STATEMENT, ownerId).parent().entityId();
        return permissions
                .hasPermission(user.getObject(), VIEW, BalanceQueries.target(residentId))
                .allowed();
    }
}

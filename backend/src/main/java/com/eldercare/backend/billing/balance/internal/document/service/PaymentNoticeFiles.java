package com.eldercare.backend.billing.balance.internal.document.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeRecords;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * {@code QuyenTepProvider} của hai tệp phiếu báo phí ({@code M11.PAYMENT_NOTICE}; 017 T007 phần B, FR-037; research
 * E-12): tải được khi người xem có quyền xem {@code PAYMENT_NOTICE} của người cao tuổi đó (Quản lý viện, Kế toán, người
 * thân có quyền xem chi phí). Chủ sở hữu tệp là dòng {@code record_event} của phiếu.
 */
@Component
class PaymentNoticeFiles implements FilePermissionProvider {

    static final PermissionKey VIEW = new PermissionKey(Function.PAYMENT_NOTICE, Operation.VIEW);

    private final RecordEventApi records;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> user;
    private final FamilyCostsRight familyRight;

    PaymentNoticeFiles(
            RecordEventApi records,
            PermissionApi permissions,
            ObjectProvider<CurrentUser> user,
            FamilyCostsRight familyRight) {
        this.familyRight = familyRight;
        this.records = records;
        this.permissions = permissions;
        this.user = user;
    }

    @Override
    public String ownerType() {
        return PaymentNoticeRecords.KIND.code();
    }

    @Override
    public boolean canDownload(long ownerId) {
        long residentId =
                records.require(PaymentNoticeRecords.KIND, ownerId).parent().entityId();
        return familyRight.allowed(residentId)
                && permissions
                        .hasPermission(user.getObject(), VIEW, BalanceQueries.target(residentId))
                        .allowed();
    }
}

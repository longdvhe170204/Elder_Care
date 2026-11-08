package com.eldercare.backend.billing.balance.internal.export.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.billing.balance.internal.export.domain.BalanceExportRecords;
import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * {@code QuyenTepProvider} của file xuất ({@code M11.BALANCE_EXPORT}; 017 T007 phần B, FR-027; research E-12): tải được
 * khi người xem có quyền xem {@code STATEMENT_REPORT} toàn viện (Kế toán, Quản lý viện). Chủ sở hữu tệp là dòng {@code
 * record_event} của lần xuất.
 */
@Component
class BalanceExportFiles implements FilePermissionProvider {

    static final PermissionKey VIEW = new PermissionKey(Function.STATEMENT_REPORT, Operation.VIEW);

    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> user;

    BalanceExportFiles(PermissionApi permissions, ObjectProvider<CurrentUser> user) {
        this.permissions = permissions;
        this.user = user;
    }

    @Override
    public String ownerType() {
        return BalanceExportRecords.KIND.code();
    }

    @Override
    public boolean canDownload(long ownerId) {
        return permissions
                .hasPermission(user.getObject(), VIEW, Target.UNSPECIFIED)
                .allowed();
    }
}

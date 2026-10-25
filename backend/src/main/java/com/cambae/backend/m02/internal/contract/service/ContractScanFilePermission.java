package com.cambae.backend.m02.internal.contract.service;

import com.cambae.backend.common.file.FilePermissionProvider;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền tải bản scan hợp đồng đã ký (004 T011 phần bản scan; research H-13 của 001, NFR-08): chủ sở hữu {@code
 * M02.CONTRACT_SCAN} là hợp đồng, tải được khi người xem có quyền xem hợp đồng của người cao tuổi đó (Phụ lục 27, dòng
 * "Tiếp nhận, hợp đồng, đặt cọc").
 */
@Component
class ContractScanFilePermission implements FilePermissionProvider {

    private static final PermissionKey VIEW = new PermissionKey(Function.ADMISSION_CONTRACT_DEPOSIT, Operation.VIEW);

    private final ContractService contracts;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> currentUser;

    ContractScanFilePermission(
            ContractService contracts, PermissionApi permissions, ObjectProvider<CurrentUser> currentUser) {
        this.contracts = contracts;
        this.permissions = permissions;
        this.currentUser = currentUser;
    }

    @Override
    public String ownerType() {
        return ContractService.SCAN_OWNER_TYPE;
    }

    @Override
    public boolean canDownload(long ownerId) {
        return permissions
                .hasPermission(currentUser.getObject(), VIEW, contracts.targetOf(ownerId))
                .allowed();
    }
}

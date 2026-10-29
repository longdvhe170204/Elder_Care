package com.eldercare.backend.resident.internal.consent.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền tải tệp gắn vào bản đồng ý (001 T011 phần bản đồng ý; research H-13, NFR-08): bản ký, bản yêu cầu rút lại và
 * bản scan quyết định của Tòa án đều có chủ sở hữu {@code M01.CONSENT}, tải được khi người xem có quyền xem bản đồng ý
 * của người cao tuổi đó (Phụ lục 27, dòng "Bản đồng ý chia sẻ dữ liệu").
 */
@Component
class ConsentFilePermission implements FilePermissionProvider {

    private static final PermissionKey VIEW = new PermissionKey(Function.DATA_SHARING_CONSENT, Operation.VIEW);

    private final ConsentService consents;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> currentUser;

    ConsentFilePermission(ConsentService consents, PermissionApi permissions, ObjectProvider<CurrentUser> currentUser) {
        this.consents = consents;
        this.permissions = permissions;
        this.currentUser = currentUser;
    }

    @Override
    public String ownerType() {
        return ConsentService.ENTITY_TYPE;
    }

    @Override
    public boolean canDownload(long ownerId) {
        return permissions
                .hasPermission(currentUser.getObject(), VIEW, consents.targetOf(ownerId))
                .allowed();
    }
}

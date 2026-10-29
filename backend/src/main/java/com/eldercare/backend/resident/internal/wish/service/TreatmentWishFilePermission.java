package com.eldercare.backend.resident.internal.wish.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền tải bản ký gắn vào phiếu nguyện vọng khi điều trị (001 T079; research H-13, H-18): chủ sở hữu
 * {@code M01.TREATMENT_WISH}, tải được khi người xem có quyền xem dị ứng, bệnh nền của người cao tuổi đó.
 */
@Component
class TreatmentWishFilePermission implements FilePermissionProvider {

    private static final PermissionKey VIEW = new PermissionKey(Function.ALLERGY_CONDITION, Operation.VIEW);

    private final TreatmentWishService wishes;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> currentUser;

    TreatmentWishFilePermission(
            TreatmentWishService wishes, PermissionApi permissions, ObjectProvider<CurrentUser> currentUser) {
        this.wishes = wishes;
        this.permissions = permissions;
        this.currentUser = currentUser;
    }

    @Override
    public String ownerType() {
        return TreatmentWishService.ENTITY_TYPE;
    }

    @Override
    public boolean canDownload(long ownerId) {
        return permissions
                .hasPermission(currentUser.getObject(), VIEW, wishes.targetOf(ownerId))
                .allowed();
    }
}

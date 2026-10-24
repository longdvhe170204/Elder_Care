package com.cambae.backend.m01.internal.wish.service;

import com.cambae.backend.common.file.FilePermissionProvider;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
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

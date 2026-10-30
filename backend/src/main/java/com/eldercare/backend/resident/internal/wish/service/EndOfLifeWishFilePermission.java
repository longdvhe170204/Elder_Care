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
 * Quyền tải tệp gắn vào phiếu nguyện vọng cuối đời (001 T069; research H-13, NFR-08): bản ký và quyết định của Tòa án
 * có chủ sở hữu {@code M01.END_OF_LIFE_WISH}, tải được khi người xem có quyền xem phiếu của người cao tuổi đó
 * (Phụ lục 27, dòng "Nguyện vọng cuối đời").
 */
@Component
class EndOfLifeWishFilePermission implements FilePermissionProvider {

    private static final PermissionKey VIEW = new PermissionKey(Function.END_OF_LIFE_WISHES, Operation.VIEW);

    private final EndOfLifeWishService wishes;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> currentUser;

    EndOfLifeWishFilePermission(
            EndOfLifeWishService wishes, PermissionApi permissions, ObjectProvider<CurrentUser> currentUser) {
        this.wishes = wishes;
        this.permissions = permissions;
        this.currentUser = currentUser;
    }

    @Override
    public String ownerType() {
        return EndOfLifeWishService.ENTITY_TYPE;
    }

    @Override
    public boolean canDownload(long ownerId) {
        return permissions
                .hasPermission(currentUser.getObject(), VIEW, wishes.targetOf(ownerId))
                .allowed();
    }
}

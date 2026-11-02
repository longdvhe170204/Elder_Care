package com.eldercare.backend.resident.internal.profile.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền tải ảnh người cao tuổi (001 T018, T011; research H-13, NFR-08): ảnh có chủ sở hữu {@code M01.RESIDENT_PHOTO}
 * (id hồ sơ), tải được khi người xem có quyền xem hồ sơ trong phạm vi và thấy đầy đủ nhóm nhận dạng, đúng như trường
 * {@code photoFileId} ở phản hồi hồ sơ ({@code photoFileId} thuộc nhóm nhận dạng, data-model mục 10).
 */
@Component
class ResidentPhotoFilePermission implements FilePermissionProvider {

    private static final PermissionKey VIEW = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);

    private final PermissionApi permissions;
    private final FieldVisibilityApi visibility;
    private final ObjectProvider<CurrentUser> currentUser;

    ResidentPhotoFilePermission(
            PermissionApi permissions, FieldVisibilityApi visibility, ObjectProvider<CurrentUser> currentUser) {
        this.permissions = permissions;
        this.visibility = visibility;
        this.currentUser = currentUser;
    }

    @Override
    public String ownerType() {
        return ResidentProfileService.PHOTO_OWNER;
    }

    @Override
    public boolean canDownload(long ownerId) {
        CurrentUser user = currentUser.getObject();
        Target target = ResidentProfileService.target(ownerId);
        return permissions.hasPermission(user, VIEW, target).allowed()
                && visibility.canView(user, InfoGroup.IDENTITY, target) == Visibility.FULL;
    }
}

package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.common.file.FilePermissionProvider;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền tải ảnh của bản ghi bàn giao ({@code QuyenTepProvider} {@code M12.ANH_DO_GUI}; 013 T007, NFR-08): người xem được
 * đồ gửi của người cao tuổi đó (khóa "Đồ gửi" thao tác Xem, theo phạm vi). Người thân có quyền xem đồ gửi (BR-M12-09) và
 * người cao tuổi chủ đồ (FR-030a) thêm ở US6, Phase 9.
 */
@Component
class BelongingPhotoPermission implements FilePermissionProvider {

    static final String OWNER_TYPE = "M12.BELONGING_PHOTO";

    private final BelongingStore reads;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> users;
    private final FamilyRightsApi family;
    private final BusinessTime time;

    BelongingPhotoPermission(
            BelongingStore reads,
            PermissionApi permissions,
            ObjectProvider<CurrentUser> users,
            FamilyRightsApi family,
            BusinessTime time) {
        this.reads = reads;
        this.permissions = permissions;
        this.users = users;
        this.family = family;
        this.time = time;
    }

    @Override
    public String ownerType() {
        return OWNER_TYPE;
    }

    /** {@code ownerId} là mã bản ghi bàn giao mang ảnh. */
    @Override
    public boolean canDownload(long ownerId) {
        CurrentUser user = users.getObject();
        if (user.type() == AccountType.FAMILY_MEMBER) {
            // BR-M12-09: người đại diện và người được phép đón, kiểm lại mỗi lần tải
            return reads.residentOfHandover(ownerId)
                    .flatMap(residentId -> user.familyMemberId()
                            .map(member -> family.canViewBelongings(member, residentId, time.now())))
                    .orElse(false);
        }
        return reads.residentOfHandover(ownerId)
                .map(residentId -> permissions
                        .hasPermission(
                                users.getObject(),
                                new PermissionKey(Function.BELONGINGS, Operation.VIEW),
                                new Target("M12.BELONGING_HANDOVER", Long.toString(ownerId), residentId, null))
                        .allowed())
                .orElse(false);
    }
}

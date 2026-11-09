package com.eldercare.backend.belongings.internal.inventorycheck.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Ảnh đồ không rõ chủ ({@code M12.INVENTORY_PHOTO}) chỉ Hành chính, Quản lý viện tải (013 T025, FR-028b). Phạm vi phiếu
 * kiểm kê ({@code M12.INVENTORY_CHECK}, toàn viện) khai một lần ở {@code BelongingRecordScopes} (013 T007).
 */
@Component
class InventoryPhotoPermission implements FilePermissionProvider {

    static final String OWNER_TYPE = "M12.INVENTORY_PHOTO";

    private final ObjectProvider<CurrentUser> users;

    InventoryPhotoPermission(ObjectProvider<CurrentUser> users) {
        this.users = users;
    }

    @Override
    public String ownerType() {
        return OWNER_TYPE;
    }

    @Override
    public boolean canDownload(long ownerId) {
        var roles = users.getObject().roles();
        return roles.contains(Role.OFFICE_STAFF) || roles.contains(Role.NURSING_HOME_MANAGER);
    }
}

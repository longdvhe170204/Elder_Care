package com.eldercare.backend.belongings.internal.inventorycheck.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Phiếu kiểm kê không gắn người cao tuổi nên phạm vi là toàn viện ({@code GanPhamViProvider M12.PHIEU_KIEM_KE}), và ảnh
 * đồ không rõ chủ ({@code M12.INVENTORY_PHOTO}) chỉ Hành chính, Quản lý viện tải (013 T007, T025, FR-028b).
 */
@Component
class InventoryPhotoPermission implements FilePermissionProvider, ScopeAssignmentProvider {

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

    @Override
    public String entityType() {
        return InventoryCheckService.RECORD_TYPE;
    }

    @Override
    public Target assignScope(long id) {
        return new Target(InventoryCheckService.RECORD_TYPE, Long.toString(id), null, null);
    }
}

package com.eldercare.backend.nutrition.internal.catalog.service;

import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền tải ảnh món ({@code M08.DISH_IMAGE}): ảnh thuộc thực đơn chung mà mọi người dùng app xem được (spec 011 FR-028,
 * Q-445), nên người đã đăng nhập đều tải được; ảnh không có thông tin cá nhân.
 */
@Component
class DishImageFilePermission implements FilePermissionProvider {

    @Override
    public String ownerType() {
        return DishService.IMAGE_OWNER;
    }

    @Override
    public boolean canDownload(long ownerId) {
        return true;
    }
}

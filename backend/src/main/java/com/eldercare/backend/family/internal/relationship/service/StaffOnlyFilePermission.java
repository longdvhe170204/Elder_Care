package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FilePermissionProvider;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Quyền tải tệp của hồ sơ người thân ({@code QuyenTepProvider}; contracts mục 1.1, NFR-08): bằng chứng đặt người đại
 * diện ({@code M10.BANG_CHUNG}) và bản scan phiếu đăng ký người thân ({@code M10.PHIEU_DANG_KY}) chỉ Hành chính, Quản lý
 * viện tải được.
 */
public record StaffOnlyFilePermission(String ownerType, ObjectProvider<CurrentUser> currentUser)
        implements FilePermissionProvider {

    private static final Set<Role> VIEWERS = Set.of(Role.OFFICE_STAFF, Role.NURSING_HOME_MANAGER);

    @Override
    public boolean canDownload(long ownerId) {
        return currentUser.getObject().roles().stream().anyMatch(VIEWERS::contains);
    }
}

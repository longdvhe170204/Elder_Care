package com.eldercare.backend.reporting.internal.export.service;

import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quyền tải tệp xuất ({@code QuyenTepProvider} loại {@code M14.BAO_CAO_XUAT}): người xuất, Quản lý viện. Tệp gắn với chính
 * id của nó; người xuất là người thực hiện của bản ghi nhật ký lần xuất.
 */
@Component
@Transactional(readOnly = true)
class ReportExportFilePermission implements FilePermissionProvider {

    private final ReportExportService exports;

    ReportExportFilePermission(ReportExportService exports) {
        this.exports = exports;
    }

    @Override
    public String ownerType() {
        return ReportExportService.FILE_OWNER;
    }

    @Override
    public boolean canDownload(long ownerId) {
        return exports.canDownload(ownerId);
    }
}

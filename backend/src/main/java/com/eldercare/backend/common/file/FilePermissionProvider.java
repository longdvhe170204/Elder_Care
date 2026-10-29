package com.eldercare.backend.common.file;

/**
 * SPI do module sở hữu bản ghi cài: quyền tải tệp bằng quyền xem bản ghi chủ sở hữu (001 research H-13, NFR-08).
 * Người xem lấy từ người dùng hiện tại trong module cài.
 */
public interface FilePermissionProvider {

    /** Loại chủ sở hữu, ví dụ {@code M01.CONSENT}. */
    String ownerType();

    boolean canDownload(long ownerId);
}

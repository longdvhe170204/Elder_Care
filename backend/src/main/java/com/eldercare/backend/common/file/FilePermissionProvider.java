package com.eldercare.backend.common.file;

/**
 * SPI do module sở hữu bản ghi cài: quyền tải tệp bằng quyền xem bản ghi chủ sở hữu (001 research H-13, NFR-08).
 * Người xem lấy từ người dùng hiện tại trong module cài.
 */
public interface FilePermissionProvider {

    /** Loại chủ sở hữu, ví dụ {@code M01.CONSENT}. */
    String ownerType();

    boolean canDownload(long ownerId);

    /**
     * Loại bản ghi trong chuỗi lưu giữ mà {@code ownerId} của tệp là mã (000 T112, FR-051a): tệp xóa cùng bản ghi đó.
     * Mặc định trùng {@link #ownerType()}; khác khi loại chủ sở hữu chỉ để phân quyền tải, ví dụ ảnh người cao tuổi
     * {@code M01.RESIDENT_PHOTO} gắn mã hồ sơ {@code M01.RESIDENT}.
     */
    default String recordType() {
        return ownerType();
    }
}

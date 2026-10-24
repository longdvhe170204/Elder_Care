package com.cambae.backend.common.file;

import java.util.List;

/**
 * Tệp đính kèm dùng chung (001 research H-13, NFR-08). 000 khai giao diện (000 T036); 001 T003 cài bảng và dịch vụ.
 * Tệp vừa tải lên chưa có chủ sở hữu; lệnh nghiệp vụ gắn tệp vào bản ghi.
 */
public interface FileApi {

    /**
     * Lưu nội dung, trả id tệp. Nhận {@link FileTypes#SYSTEM_GENERATED} (PDF, JPEG, PNG và {@code .xlsx} cho tệp hệ
     * thống tạo, 017 research E-11), tối đa 10 MB; kiểm bằng {@link FileTypes#check}. {@code POST /files} kiểm trước
     * với {@link FileTypes#USER_UPLOAD} nên người dùng không tải lên được {@code .xlsx}.
     */
    long upload(String originalName, String mimeType, byte[] content, long uploadedBy);

    /**
     * Gắn tệp vào bản ghi chủ sở hữu trong transaction của lệnh (Propagation.MANDATORY). Tệp đã gắn không gắn lại được.
     */
    void attach(long fileId, String ownerType, long ownerId);

    /**
     * Các tệp đã gắn vào một bản ghi, theo thứ tự tải lên (001 T083; chỉ siêu dữ liệu). Hàm đọc thêm, không đổi
     * {@link #upload}, {@link #attach} hay quyền tải; người gọi tự kiểm quyền xem bản ghi chủ sở hữu.
     */
    List<FileView> attachedTo(String ownerType, long ownerId);
}

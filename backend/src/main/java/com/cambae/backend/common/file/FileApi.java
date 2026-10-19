package com.cambae.backend.common.file;

/**
 * Tệp đính kèm dùng chung (001 research H-13, NFR-08). 000 khai giao diện (000 T036); 001 T003 cài bảng và dịch vụ.
 * Tệp vừa tải lên chưa có chủ sở hữu; lệnh nghiệp vụ gắn tệp vào bản ghi.
 */
public interface FileApi {

    /** Lưu nội dung (PDF, JPEG, PNG, tối đa 10 MB), trả id tệp. */
    long upload(String originalName, String mimeType, byte[] content, long uploadedBy);

    /**
     * Gắn tệp vào bản ghi chủ sở hữu trong transaction của lệnh (Propagation.MANDATORY). Tệp đã gắn không gắn lại được.
     */
    void attach(long fileId, String ownerType, long ownerId);
}

package com.eldercare.backend.notification.api.spi;

/**
 * Module nguồn trả lời "tài khoản đang đăng nhập mở được bản ghi nguồn này không" (009 FR-033, FR-034: "mở thông báo để
 * chuyển sang bản ghi nguồn, nếu có quyền"; contracts/module-interfaces.md mục 2.1). Hộp thông báo dùng để đặt {@code
 * sourceRecord.canOpen}. Mỗi bản cài trả lời cho một loại nguồn; loại không có bản cài, hoặc bản cài ném lỗi, là không
 * mở được. Bản cài đọc người dùng hiện tại và kiểm theo đúng quyền của API xem bản ghi đó.
 */
public interface SourceRecordAccessPort {

    /** Loại bản ghi nguồn, như {@code SourceRecord.type} của yêu cầu thông báo (ví dụ {@code M12.RECIPIENT_CONFIRMATION}). */
    String sourceRecordType();

    /** Người dùng hiện tại mở được bản ghi {@code sourceRecordId} của loại này theo quyền hiện hành. */
    boolean canOpen(String sourceRecordId);
}

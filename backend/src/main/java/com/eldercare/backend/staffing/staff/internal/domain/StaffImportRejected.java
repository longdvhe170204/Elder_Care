package com.eldercare.backend.staffing.staff.internal.domain;
/** Xác nhận có lỗi dòng trả chính bảng xem trước theo hợp đồng 409, không ghi một phần. */
public final class StaffImportRejected extends RuntimeException {
    private final StaffImportData.Preview preview;

    public StaffImportRejected(StaffImportData.Preview preview) {
        super("File còn dòng lỗi");
        this.preview = preview;
    }

    public StaffImportData.Preview preview() {
        return preview;
    }
}

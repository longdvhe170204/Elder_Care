package com.cambae.backend.m15.account.api;

/**
 * Đối tượng được kiểm quyền, phạm vi: loại, id, người cao tuổi, tầng/khu vực nếu có (002 {@code DoiTuong}).
 * 000 khai báo trước (000 T025, contracts/module-interfaces.md mục 2); feature 002 cài bản thật.
 */
public record Target(String type, String id, Long residentId, Long floorAreaId) {

    /** Không gắn đối tượng cụ thể (ví dụ nhật ký toàn viện, tham số). */
    public static final Target UNSPECIFIED = new Target(null, null, null, null);
}

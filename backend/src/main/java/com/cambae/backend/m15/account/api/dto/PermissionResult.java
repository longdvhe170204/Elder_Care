package com.cambae.backend.m15.account.api.dto;

import java.util.Optional;

/**
 * Kết quả kiểm quyền ba lớp BR-M15-01 (002 research A-10): được phép kèm căn cứ, hoặc không kèm lớp không thỏa và giấy
 * phép khi lớp pháp lý không thỏa. 000 khai báo trước (000 T025); 002 thêm lớp không thỏa và căn cứ (002 T008).
 */
public record PermissionResult(
        boolean allowed,
        Optional<FailedLayer> failedLayer,
        Optional<LicenseView> license,
        Optional<PermissionBasis> basis) {

    public static PermissionResult allowed(PermissionBasis basis) {
        return new PermissionResult(true, Optional.empty(), Optional.empty(), Optional.of(basis));
    }

    public static PermissionResult denied(FailedLayer layer) {
        return new PermissionResult(false, Optional.of(layer), Optional.empty(), Optional.empty());
    }

    public static PermissionResult deniedByLicense(LicenseView license) {
        return new PermissionResult(false, Optional.of(FailedLayer.LEGAL), Optional.of(license), Optional.empty());
    }
}

package com.eldercare.backend.medication.internal.dose.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.spi.LicenseApi;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Quyền của lệnh liều qua HTTP (Cho dùng, Từ chối, Không thực hiện, Hoãn, Nhận liều, Bỏ nhận; BR-M07-21, Q-61): quyền
 * chuyên môn Phát thuốc theo vai trò (Phụ lục 27: Điều dưỡng) và giấy phép còn hiệu lực cho hoạt động Phát thuốc, cùng
 * khóa với đường ngoại tuyến ({@code DoseResultOfflineHandler}). Không xét phạm vi phân công: liều chung của tầng (Q-61)
 * và Điều dưỡng phụ trách tạm (BR-M07-31) nằm ngoài phân công, người được xác nhận liều nào do lệnh liều tự kiểm (review
 * BF-07 2026-11-25; trước đây mọi tài khoản đã đăng nhập đều gọi được).
 */
@Service
public class DoseCommandPermission {

    private static final PermissionKey ADMINISTER =
            new PermissionKey(Function.MEDICATION_ADMINISTRATION, Operation.PROFESSIONAL);

    private final PermissionApi permissions;
    private final ProfessionalActivityApi activities;
    private final LicenseApi licenses;
    private final BusinessTime time;

    public DoseCommandPermission(
            PermissionApi permissions, ProfessionalActivityApi activities, LicenseApi licenses, BusinessTime time) {
        this.permissions = permissions;
        this.activities = activities;
        this.licenses = licenses;
        this.time = time;
    }

    /** 403 {@code PERMISSION_DENIED} khi thiếu quyền theo vai trò; 403 {@code LEGAL_CONDITION_NOT_MET} khi thiếu giấy phép. */
    public void require(CurrentUser user) {
        if (user.staffId().isEmpty() || !permissions.permissions(user).contains(ADMINISTER)) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED",
                    "Chỉ Điều dưỡng xác nhận được liều thuốc (BR-M07-21)",
                    Map.of("failedLayer", "ROLE"));
        }
        Optional<String> required = activities.requiredLicenseType(ProfessionalActivityApi.MEDICATION_ADMINISTRATION);
        if (required.isEmpty()
                || licenses.valid(user.staffId().get(), required.get(), time.today())
                        .isEmpty()) {
            throw new PermissionDeniedException(
                    "LEGAL_CONDITION_NOT_MET", "Thiếu điều kiện pháp lý", Map.of("failedLayer", "LEGAL"));
        }
    }
}

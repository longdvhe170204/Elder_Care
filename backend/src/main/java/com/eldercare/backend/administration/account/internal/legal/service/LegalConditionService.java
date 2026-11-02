package com.eldercare.backend.administration.account.internal.legal.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.LicenseView;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.spi.FacilityLicenseApi;
import com.eldercare.backend.administration.account.api.spi.LicenseApi;
import com.eldercare.backend.administration.account.api.spi.LicenseValidity;
import com.eldercare.backend.administration.account.internal.legal.domain.ProfessionalActivity;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Lớp pháp lý, bước 4 của {@code PermissionService} (002 T064; research A-09; FR-037, FR-038, FR-040, BR-M06-05). Chỉ
 * thao tác {@code PROFESSIONAL}; phải thỏa mọi mục đang hiệu lực của chức năng, ở ngày của thời điểm kiểm theo
 * Asia/Ho_Chi_Minh, không lưu trạng thái bật, tắt nên gia hạn có hiệu lực ngay:
 *
 * <ol>
 *   <li>mục đã cấu hình loại giấy phép và người dùng là nhân viên; không thì từ chối (BR-M06-05 cấm mặc định được làm);
 *   <li>(a) có giấy phép loại đó còn hiệu lực ({@code LicenseApi.valid}); không thì kèm giấy phép gần nhất để nêu tên,
 *       ngày hết hạn;
 *   <li>(b) giấy phép có mọi mục phạm vi hành nghề mục yêu cầu;
 *   <li>(c) mục cần phạm vi cơ sở thì {@code FacilityLicenseApi.coversScope(mã mục, ngày)}.
 * </ol>
 *
 * Module 008, 007 chưa cài thì từ chối.
 */
@Service
public class LegalConditionService {

    private final ProfessionalActivities activities;
    // ObjectProvider: 008, 007 cài; trước đó chỉ test khai bản giả
    private final ObjectProvider<LicenseApi> licenses;
    private final ObjectProvider<FacilityLicenseApi> facility;

    LegalConditionService(
            ProfessionalActivities activities,
            ObjectProvider<LicenseApi> licenses,
            ObjectProvider<FacilityLicenseApi> facility) {
        this.activities = activities;
        this.licenses = licenses;
        this.facility = facility;
    }

    /** Kết quả từ chối của lớp pháp lý; rỗng khi thỏa hoặc khóa không phải thao tác chuyên môn. */
    public Optional<PermissionResult> denial(CurrentUser user, PermissionKey key, Instant at) {
        if (key.operation() != Operation.PROFESSIONAL) {
            return Optional.empty();
        }
        LocalDate date = LocalDate.ofInstant(at, BusinessTime.ZONE);
        for (ProfessionalActivity activity : activities.activeFor(key.function())) {
            Optional<PermissionResult> denied = check(user, activity, date);
            if (denied.isPresent()) {
                return denied;
            }
        }
        return Optional.empty();
    }

    private Optional<PermissionResult> check(CurrentUser user, ProfessionalActivity activity, LocalDate date) {
        LicenseApi api = licenses.getIfAvailable();
        if (activity.requiredLicenseType() == null || user.staffId().isEmpty() || api == null) {
            return Optional.of(PermissionResult.denied(FailedLayer.LEGAL));
        }
        long staffId = user.staffId().get();
        Optional<LicenseValidity> valid = api.valid(staffId, activity.requiredLicenseType(), date);
        if (valid.isEmpty()) {
            return Optional.of(api.latest(staffId, activity.requiredLicenseType())
                    .map(l -> PermissionResult.deniedByLicense(view(l)))
                    .orElseGet(() -> PermissionResult.denied(FailedLayer.LEGAL)));
        }
        if (!valid.get().practiceScopeIds().containsAll(activity.practiceScopes())) {
            return Optional.of(PermissionResult.deniedByLicense(view(valid.get())));
        }
        if (activity.requiresFacilityScope()) {
            FacilityLicenseApi facilityApi = facility.getIfAvailable();
            if (facilityApi == null || !facilityApi.coversScope(activity.code(), date)) {
                return Optional.of(PermissionResult.denied(FailedLayer.LEGAL));
            }
        }
        return Optional.empty();
    }

    private static LicenseView view(LicenseValidity license) {
        return new LicenseView(license.name(), license.expiryDate());
    }
}

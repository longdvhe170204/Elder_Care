package com.eldercare.backend.administration.account.internal.legal.service;

import com.eldercare.backend.administration.account.api.events.LicenseExpiring;
import com.eldercare.backend.administration.account.api.spi.ExpiringFacilityLicense;
import com.eldercare.backend.administration.account.api.spi.ExpiringLicense;
import com.eldercare.backend.administration.account.api.spi.FacilityLicenseApi;
import com.eldercare.backend.administration.account.api.spi.LicenseApi;
import com.eldercare.backend.administration.account.internal.legal.domain.ProfessionalActivity;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.ParameterCode;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cảnh báo giấy phép sắp hết hạn (002 T066; research A-09; FR-039): giấy phép cơ sở (007) và người hành nghề (008) hết
 * hạn trong CFG-M06-02; mỗi {@code (loại, giấy phép, ngày hết hạn)} một lần nhờ dấu đã nhắc
 * {@code M15.LICENSE_EXPIRY_WARNING} ({@code reminder_marker}, 002 T098), gia hạn đổi ngày nên lần sau lại cảnh báo. Sự kiện {@code LicenseExpiring} nêu tên nghiệp vụ chuyên môn đang hiệu lực sẽ
 * bị tắt và người liên quan (người hành nghề; giấy phép cơ sở chưa nêu người). 009 gửi Quản lý viện.
 */
@Service
public class LicenseExpiryWarnings {

    public static final String JOB = "WARN_EXPIRING_LICENSES";

    private static final String MARKER_KIND = "M15.LICENSE_EXPIRY_WARNING";

    private final ReminderMarkers markers;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final ProfessionalActivities activities;
    private final ApplicationEventPublisher events;
    // ObjectProvider: 008, 007 cài; trước đó chỉ test khai bản giả
    private final ObjectProvider<LicenseApi> licenses;
    private final ObjectProvider<FacilityLicenseApi> facility;

    LicenseExpiryWarnings(
            ReminderMarkers markers,
            BusinessTime time,
            ParameterApi parameters,
            ProfessionalActivities activities,
            ApplicationEventPublisher events,
            ObjectProvider<LicenseApi> licenses,
            ObjectProvider<FacilityLicenseApi> facility) {
        this.markers = markers;
        this.time = time;
        this.parameters = parameters;
        this.activities = activities;
        this.events = events;
        this.licenses = licenses;
        this.facility = facility;
    }

    @Transactional
    public void run() {
        Period within = Period.ofDays(
                (int) parameters.read(ParameterCode.CFG_M06_02).asDuration().toDays());
        FacilityLicenseApi facilityApi = facility.getIfAvailable();
        if (facilityApi != null) {
            for (ExpiringFacilityLicense l : facilityApi.expiringWithin(within)) {
                if (firstWarning("FACILITY", l.licenseId(), l.expiryDate())) {
                    events.publishEvent(new LicenseExpiring(
                            "FACILITY",
                            l.licenseId(),
                            l.expiryDate(),
                            affected(ProfessionalActivity::requiresFacilityScope),
                            List.of(),
                            time.now()));
                }
            }
        }
        LicenseApi licenseApi = licenses.getIfAvailable();
        if (licenseApi != null) {
            for (ExpiringLicense l : licenseApi.expiringWithin(within)) {
                if (firstWarning("PRACTITIONER", l.licenseId(), l.expiryDate())) {
                    events.publishEvent(new LicenseExpiring(
                            "PRACTITIONER",
                            l.licenseId(),
                            l.expiryDate(),
                            affected(a -> Objects.equals(a.requiredLicenseType(), l.licenseType())),
                            List.of(l.staffId()),
                            time.now()));
                }
            }
        }
    }

    private boolean firstWarning(String kind, long licenseId, LocalDate expiryDate) {
        return markers.mark(MARKER_KIND, kind, licenseId, expiryDate.toString(), null);
    }

    private List<String> affected(Predicate<ProfessionalActivity> condition) {
        return activities.all().stream()
                .filter(ProfessionalActivity::active)
                .filter(condition)
                .map(ProfessionalActivity::name)
                .toList();
    }
}

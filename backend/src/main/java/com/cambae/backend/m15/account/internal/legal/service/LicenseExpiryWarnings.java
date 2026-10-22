package com.cambae.backend.m15.account.internal.legal.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.events.LicenseExpiring;
import com.cambae.backend.m15.account.api.spi.ExpiringFacilityLicense;
import com.cambae.backend.m15.account.api.spi.ExpiringLicense;
import com.cambae.backend.m15.account.api.spi.FacilityLicenseApi;
import com.cambae.backend.m15.account.api.spi.LicenseApi;
import com.cambae.backend.m15.account.internal.legal.domain.ProfessionalActivity;
import com.cambae.backend.m15.account.internal.legal.persistence.ProfessionalActivities;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cảnh báo giấy phép sắp hết hạn (002 T066; research A-09; FR-039): giấy phép cơ sở (007) và người hành nghề (008) hết
 * hạn trong CFG-M06-02; mỗi {@code (loại, giấy phép, ngày hết hạn)} một lần nhờ bảng {@code license_expiry_warning}, gia
 * hạn đổi ngày nên lần sau lại cảnh báo. Sự kiện {@code LicenseExpiring} nêu tên nghiệp vụ chuyên môn đang hiệu lực sẽ
 * bị tắt và người liên quan (người hành nghề; giấy phép cơ sở chưa nêu người). 009 gửi Quản lý viện.
 */
@Service
public class LicenseExpiryWarnings {

    public static final String JOB = "WARN_EXPIRING_LICENSES";

    private final JdbcTemplate jdbc;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final ProfessionalActivities activities;
    private final ApplicationEventPublisher events;
    // ObjectProvider: 008, 007 cài; trước đó chỉ test khai bản giả
    private final ObjectProvider<LicenseApi> licenses;
    private final ObjectProvider<FacilityLicenseApi> facility;

    LicenseExpiryWarnings(
            JdbcTemplate jdbc,
            BusinessTime time,
            ParameterApi parameters,
            ProfessionalActivities activities,
            ApplicationEventPublisher events,
            ObjectProvider<LicenseApi> licenses,
            ObjectProvider<FacilityLicenseApi> facility) {
        this.jdbc = jdbc;
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
        try {
            jdbc.update(
                    "INSERT INTO license_expiry_warning (kind, license_id, expiry_date, warned_at) VALUES (?, ?, ?, ?)",
                    kind,
                    licenseId,
                    expiryDate,
                    LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private List<String> affected(Predicate<ProfessionalActivity> condition) {
        return activities.all().stream()
                .filter(ProfessionalActivity::active)
                .filter(condition)
                .map(ProfessionalActivity::name)
                .toList();
    }
}

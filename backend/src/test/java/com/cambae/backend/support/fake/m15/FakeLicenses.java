package com.cambae.backend.support.fake.m15;

import com.cambae.backend.m15.account.api.spi.ExpiringLicense;
import com.cambae.backend.m15.account.api.spi.LicenseApi;
import com.cambae.backend.m15.account.api.spi.LicenseValidity;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bản giả {@link LicenseApi} (008 cài): giấy phép còn hiệu lực tới hết ngày hết hạn (FR-038); {@link #expiringWithin}
 * trả danh sách test đặt sẵn.
 */
public class FakeLicenses implements LicenseApi {

    private record Entry(long staffId, String licenseType, LicenseValidity license) {}

    private final List<Entry> licenses = new ArrayList<>();
    private final List<ExpiringLicense> expiring = new ArrayList<>();

    public FakeLicenses license(long staffId, String licenseType, LicenseValidity license) {
        licenses.add(new Entry(staffId, licenseType, license));
        return this;
    }

    public FakeLicenses expiring(ExpiringLicense license) {
        expiring.add(license);
        return this;
    }

    @Override
    public Optional<LicenseValidity> valid(long staffId, String licenseType, LocalDate date) {
        return licenses.stream()
                .filter(e -> e.staffId() == staffId && e.licenseType().equals(licenseType))
                .map(Entry::license)
                .filter(l -> !date.isAfter(l.expiryDate()))
                .findFirst();
    }

    /** Bản đặt sau cùng của loại thắng (gia hạn là đặt bản mới). */
    @Override
    public Optional<LicenseValidity> latest(long staffId, String licenseType) {
        return licenses.reversed().stream()
                .filter(e -> e.staffId() == staffId && e.licenseType().equals(licenseType))
                .map(Entry::license)
                .findFirst();
    }

    @Override
    public List<ExpiringLicense> expiringWithin(Period within) {
        return List.copyOf(expiring);
    }
}

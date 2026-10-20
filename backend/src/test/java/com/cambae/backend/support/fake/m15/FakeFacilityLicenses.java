package com.cambae.backend.support.fake.m15;

import com.cambae.backend.m15.account.api.ExpiringFacilityLicense;
import com.cambae.backend.m15.account.api.FacilityLicenseApi;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bản giả {@link FacilityLicenseApi} (007 cài ở M06): phạm vi hoạt động có giấy phép tới hết ngày hết hạn test đặt sẵn;
 * {@link #expiringWithin} trả danh sách test đặt sẵn.
 */
public class FakeFacilityLicenses implements FacilityLicenseApi {

    private final Map<String, LocalDate> scopes = new HashMap<>();
    private final List<ExpiringFacilityLicense> expiring = new ArrayList<>();

    /** Phạm vi hoạt động có giấy phép còn hiệu lực tới hết {@code expiryDate}. */
    public FakeFacilityLicenses scope(String operationScope, LocalDate expiryDate) {
        scopes.put(operationScope, expiryDate);
        return this;
    }

    public FakeFacilityLicenses expiring(ExpiringFacilityLicense license) {
        expiring.add(license);
        return this;
    }

    @Override
    public boolean coversScope(String operationScope, LocalDate date) {
        LocalDate expiry = scopes.get(operationScope);
        return expiry != null && !date.isAfter(expiry);
    }

    @Override
    public List<ExpiringFacilityLicense> expiringWithin(Period within) {
        return List.copyOf(expiring);
    }
}

package com.eldercare.backend.staffing.duty.internal.service;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.credential.internal.domain.*;
import com.eldercare.backend.staffing.credential.internal.persistence.CredentialStore;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T007: 002 và 008 dùng chung hàm hiệu lực; loại nhận code hoặc id kiểu chuỗi của danh mục. */
@Service
@Transactional(readOnly = true)
class StaffLicenseLookup implements LicenseApi {
    private final CredentialStore credentials;
    private final BusinessTime time;

    StaffLicenseLookup(CredentialStore credentials, BusinessTime time) {
        this.credentials = credentials;
        this.time = time;
    }

    private boolean typeMatches(Credential c, String type) {
        return c.typeCode().equals(type) || Long.toString(c.credentialTypeId()).equals(type);
    }

    private LicenseValidity view(Credential c) {
        return new LicenseValidity(c.id(), c.typeName(), c.expiryDate(), c.practiceScopeIds());
    }

    public Optional<LicenseValidity> valid(long staffId, String licenseType, LocalDate date) {
        return credentials.staffCredentials(staffId).stream()
                .filter(c -> typeMatches(c, licenseType))
                .filter(c -> CredentialValidity.status(
                                c.issueDate(),
                                c.expiryDate(),
                                c.revokedFrom(),
                                c.replaced(),
                                time.startOfDay(date),
                                time)
                        == CredentialValidity.Status.VALID)
                .max(Comparator.comparing(Credential::issueDate).thenComparingLong(Credential::id))
                .map(this::view);
    }

    public Optional<LicenseValidity> latest(long staffId, String licenseType) {
        return credentials.staffCredentials(staffId).stream()
                .filter(c -> typeMatches(c, licenseType))
                .max(Comparator.comparing(Credential::issueDate).thenComparingLong(Credential::id))
                .map(this::view);
    }

    public List<ExpiringLicense> expiringWithin(Period within) {
        LocalDate today = time.today();
        LocalDate until = today.plus(within);
        return credentials.all().stream()
                .filter(c -> "PROFESSIONAL_LICENSE".equals(c.group()))
                .filter(c -> c.expiryDate() != null
                        && !c.expiryDate().isBefore(today)
                        && !c.expiryDate().isAfter(until))
                .filter(c -> CredentialValidity.status(
                                c.issueDate(), c.expiryDate(), c.revokedFrom(), c.replaced(), time.now(), time)
                        == CredentialValidity.Status.VALID)
                .map(c -> new ExpiringLicense(c.id(), c.staffId(), c.typeCode(), c.typeName(), c.expiryDate()))
                .toList();
    }
}

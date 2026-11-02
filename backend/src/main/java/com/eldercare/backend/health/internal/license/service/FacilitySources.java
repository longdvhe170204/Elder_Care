package com.eldercare.backend.health.internal.license.service;

import com.eldercare.backend.administration.account.api.spi.ExpiringFacilityLicense;
import com.eldercare.backend.administration.account.api.spi.FacilityLicenseApi;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.FacilityContactApi;
import com.eldercare.backend.health.api.dto.FacilityContactView;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nguồn đọc giấy phép và số liên hệ theo FR-070/071/075, research S-16. */
@Service
@Transactional(readOnly = true)
public class FacilitySources implements FacilityLicenseApi, FacilityContactApi {
    private static final SharedKind<Map> LICENSE = SharedKind.of("M06.FACILITY_LICENSE", Map.class).notDeletable();
    private static final SharedKind<Map> CONTACT = SharedKind.of("M06.FACILITY_CONTACT", Map.class).notDeletable();
    private final CatalogApi catalogs;
    private final BusinessTime time;

    public FacilitySources(CatalogApi catalogs, BusinessTime time) {
        this.catalogs = catalogs;
        this.time = time;
    }

    @Override
    public boolean coversScope(String scope, LocalDate date) {
        return usableLicenses().stream().anyMatch(row -> {
            var a = row.attributes();
            return Boolean.TRUE.equals(a.get("medicalCare"))
                    && a.get("operationScopes") instanceof Collection<?> scopes && scopes.contains(scope)
                    && !date.isBefore(date(a, "issueDate")) && !date.isAfter(date(a, "expiryDate"));
        });
    }

    @Override
    public List<ExpiringFacilityLicense> expiringWithin(Period within) {
        var today = time.today();
        var until = today.plus(within);
        return usableLicenses().stream()
                .filter(row -> !date(row.attributes(), "issueDate").isAfter(today))
                .filter(row -> !date(row.attributes(), "expiryDate").isBefore(today)
                        && !date(row.attributes(), "expiryDate").isAfter(until))
                .map(row -> new ExpiringFacilityLicense(row.id(), row.name(), date(row.attributes(), "expiryDate")))
                .toList();
    }

    @Override
    public FacilityContactView current() {
        return catalogs.findByCode(CONTACT, null, "CURRENT")
                .map(row -> new FacilityContactView((String) row.attributes().get("phoneNumber"),
                        (String) row.attributes().get("note"), row.version()))
                .orElseGet(() -> new FacilityContactView(null, null, 0));
    }

    private List<CatalogItem<Map>> usableLicenses() {
        var rows = catalogs.list(LICENSE, null, false);
        var replaced = new HashSet<Long>();
        for (var row : rows) {
            var a = row.attributes();
            if (Boolean.TRUE.equals(a.get("replacementDueToError")) && a.get("replacesId") instanceof Number id) {
                replaced.add(id.longValue());
            }
        }
        return rows.stream().filter(row -> !replaced.contains(row.id())).toList();
    }

    private static LocalDate date(Map attributes, String key) {
        return LocalDate.parse((String) attributes.get(key));
    }
}

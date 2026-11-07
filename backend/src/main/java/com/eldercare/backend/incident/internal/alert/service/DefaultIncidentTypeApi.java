package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.incident.api.IncidentTypeApi;
import com.eldercare.backend.incident.internal.alert.domain.IncidentCatalogs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tra id loại sự cố theo mã trong danh mục {@code M05.INCIDENT_TYPE}. */
@Service
public class DefaultIncidentTypeApi implements IncidentTypeApi {

    private final CatalogApi catalogs;

    public DefaultIncidentTypeApi(CatalogApi catalogs) {
        this.catalogs = catalogs;
    }

    @Override
    @Transactional(readOnly = true)
    public long idOf(String code) {
        return catalogs.findByCode(IncidentCatalogs.INCIDENT_TYPE, null, code)
                .orElseThrow(NotFoundException::new)
                .id();
    }
}

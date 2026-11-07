package com.eldercare.backend.staffing.internal.duty.service;

import com.eldercare.backend.administration.account.api.spi.PracticeScopeCatalogApi;
import com.eldercare.backend.administration.account.api.spi.PracticeScopeView;
import com.eldercare.backend.staffing.internal.credential.persistence.CredentialStore;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T007: tra danh mục M09 qua SPI của 002. */
@Service
@Transactional(readOnly = true)
class PracticeScopeLookup implements PracticeScopeCatalogApi {
    private final CredentialStore credentials;

    PracticeScopeLookup(CredentialStore credentials) {
        this.credentials = credentials;
    }

    public Optional<PracticeScopeView> find(long id) {
        return credentials
                .practiceScope(id)
                .map(scope -> new PracticeScopeView(scope.id(), scope.name(), scope.active()));
    }
}

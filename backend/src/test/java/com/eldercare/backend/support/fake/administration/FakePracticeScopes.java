package com.eldercare.backend.support.fake.administration;

import com.eldercare.backend.administration.account.api.spi.PracticeScopeCatalogApi;
import com.eldercare.backend.administration.account.api.spi.PracticeScopeView;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Bản giả {@link PracticeScopeCatalogApi} (008 cài). */
public class FakePracticeScopes implements PracticeScopeCatalogApi {

    private final Map<Long, PracticeScopeView> items = new HashMap<>();

    public FakePracticeScopes item(PracticeScopeView item) {
        items.put(item.id(), item);
        return this;
    }

    @Override
    public Optional<PracticeScopeView> find(long practiceScopeId) {
        return Optional.ofNullable(items.get(practiceScopeId));
    }
}

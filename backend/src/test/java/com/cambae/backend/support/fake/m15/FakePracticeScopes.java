package com.cambae.backend.support.fake.m15;

import com.cambae.backend.m15.account.api.PracticeScopeCatalogApi;
import com.cambae.backend.m15.account.api.PracticeScopeView;
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

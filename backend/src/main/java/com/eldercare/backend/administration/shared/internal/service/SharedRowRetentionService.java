package com.eldercare.backend.administration.shared.internal.service;

import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.internal.persistence.SharedRowRetentionStore;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Cài {@link SharedRowRetention} (research R-15 mục 2, R-11). */
@Service
class SharedRowRetentionService implements SharedRowRetention {

    private final SharedRowRetentionStore store;

    SharedRowRetentionService(SharedRowRetentionStore store) {
        this.store = store;
    }

    @Override
    public void deleteFor(Collection<EntityRef> records) {
        Map<String, List<Long>> byType = records.stream()
                .collect(Collectors.groupingBy(
                        EntityRef::entityType, Collectors.mapping(EntityRef::entityId, Collectors.toList())));
        byType.forEach(store::deleteForEntity);
        List<Long> residents = byType.getOrDefault(RESIDENT, List.of());
        if (!residents.isEmpty()) {
            deleteForResidents(residents);
        }
    }

    @Override
    public void deleteForResidents(Collection<Long> residentIds) {
        if (!residentIds.isEmpty()) {
            store.deleteForResidents(residentIds);
        }
    }
}

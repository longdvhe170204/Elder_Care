package com.eldercare.backend.administration.shared.internal.service;

import com.eldercare.backend.administration.shared.api.RecordTypeAliases;
import com.eldercare.backend.administration.shared.api.spi.LegacyRecordTypeSource;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Cài {@link RecordTypeAliases} từ các {@link LegacyRecordTypeSource} của module (research R-15 mục 5). */
@Service
class RecordTypeAliasService implements RecordTypeAliases {

    private final Map<String, String> currentByLegacy = new HashMap<>();
    private final Map<String, Set<String>> legacyByCurrent = new HashMap<>();

    RecordTypeAliasService(List<LegacyRecordTypeSource> sources) {
        for (LegacyRecordTypeSource source : sources) {
            source.legacyRecordTypes().forEach((legacy, current) -> {
                String previous = currentByLegacy.putIfAbsent(legacy, current);
                if (previous != null && !previous.equals(current)) {
                    throw new IllegalStateException(
                            "Loại cũ " + legacy + " ánh xạ tới hai loại: " + previous + ", " + current);
                }
                legacyByCurrent
                        .computeIfAbsent(current, k -> new LinkedHashSet<>())
                        .add(legacy);
            });
        }
    }

    @Override
    public Set<String> withLegacy(String recordType) {
        Set<String> types = new LinkedHashSet<>();
        types.add(recordType);
        types.addAll(legacyByCurrent.getOrDefault(recordType, Set.of()));
        return types;
    }

    @Override
    public String current(String recordType) {
        return currentByLegacy.getOrDefault(recordType, recordType);
    }
}

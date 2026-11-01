package com.eldercare.backend.support.fake.care;

import com.eldercare.backend.care.api.spi.TaskSourcePort;
import java.time.Instant;
import java.util.*;

/** Mỗi bean giữ dữ liệu của đúng nguồn 007/011/014; mặc định căn cứ không hợp lệ. */
public class FakeTaskSource implements TaskSourcePort {
    private final FakeM04Switches switches;
    private final String module;
    private final Set<SourceType> types;
    private final Map<Reference, SourceValidation> results = new HashMap<>();
    private final Map<MealResident, Boolean> served = new HashMap<>();
    private final Map<Long, List<SourceView>> sources = new HashMap<>();

    public void sources(long residentId, List<SourceView> values) {
        if (values.stream().anyMatch(v -> !types.contains(v.type())))
            throw new IllegalArgumentException("Loại căn cứ không thuộc nguồn " + module);
        sources.put(residentId, List.copyOf(values));
    }

    @Override
    public List<SourceView> sources(long residentId, Instant from, Instant to) {
        switches.requireEnabled(module);
        return sources.getOrDefault(residentId, List.of()).stream()
                .filter(v -> !v.at().isBefore(from) && v.at().isBefore(to))
                .toList();
    }

    public FakeTaskSource(FakeM04Switches switches, String module, Set<SourceType> types) {
        this.switches = switches;
        this.module = module;
        this.types = Set.copyOf(types);
    }

    public void validation(
            SourceType type, long sourceId, long residentId, Instant plannedAt, SourceValidation result) {
        if (!types.contains(type)) throw new IllegalArgumentException("Loại căn cứ không thuộc nguồn " + module);
        results.put(new Reference(type, sourceId, residentId, plannedAt), result);
    }

    public void served(long mealId, long residentId, boolean value) {
        served.put(new MealResident(mealId, residentId), value);
    }

    @Override
    public Set<SourceType> managedTypes() {
        return types;
    }

    @Override
    public SourceValidation validate(SourceType type, long sourceId, long residentId, Instant plannedAt) {
        switches.requireEnabled(module);
        return results.getOrDefault(
                new Reference(type, sourceId, residentId, plannedAt), new SourceValidation(false, false, false, false));
    }

    @Override
    public boolean confirmServing(long mealId, long residentId) {
        switches.requireEnabled(module);
        return served.getOrDefault(new MealResident(mealId, residentId), true);
    }

    public void reset() {
        results.clear();
        sources.clear();
        served.clear();
    }

    private record Reference(SourceType type, long sourceId, long residentId, Instant plannedAt) {}

    private record MealResident(long mealId, long residentId) {}
}

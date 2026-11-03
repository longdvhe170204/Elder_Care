package com.eldercare.backend.health.internal.measurement.service;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.health.api.dto.LatestVitalSignView;
import com.eldercare.backend.health.api.dto.VitalSignHandoverView;
import com.eldercare.backend.health.api.dto.VitalSignReadingView;
import com.eldercare.backend.health.internal.catalog.domain.HealthCatalogs;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Nguồn thật chỉ đọc của M06; không thực hiện lệnh ghi hoặc tự sinh bản ghi. */
@Service
@Transactional(readOnly = true)
public class DefaultVitalSignApi implements VitalSignApi {
    private static final String RECORD_TYPE = "M06.VITAL_SIGN_RECORD";
    private final VitalSignRecordStore records;
    private final CorrectionApi corrections;
    private final CatalogApi catalogs;
    private final ResidentLocationApi locations;
    private final JsonMapper json;

    public DefaultVitalSignApi(
            VitalSignRecordStore records,
            CorrectionApi corrections,
            CatalogApi catalogs,
            ResidentLocationApi locations,
            JsonMapper json) {
        this.records = records;
        this.corrections = corrections;
        this.catalogs = catalogs;
        this.locations = locations;
        this.json = json;
    }

    @Override
    public Map<Long, LatestVitalSignView> latestPerType(long residentId) {
        var rows = records.confirmedForResident(residentId);
        var current = current(rows);
        Map<Long, LatestVitalSignView> result = new LinkedHashMap<>();
        for (var row : rows) {
            if (voided(current.get(row.id())) || result.containsKey(row.indicatorId())) continue;
            var indicator = catalogs.require(HealthCatalogs.VITAL_SIGN, row.indicatorId());
            result.put(
                    row.indicatorId(),
                    new LatestVitalSignView(
                            row.id(),
                            row.indicatorId(),
                            indicator.code(),
                            indicator.name(),
                            indicator.attributes().unit(),
                            values(row, current.get(row.id())),
                            row.measuredAt()));
        }
        return Map.copyOf(result);
    }

    @Override
    public List<VitalSignReadingView> withinRange(long residentId, long indicatorId, Instant from, Instant to) {
        var rows = records.withinRange(residentId, indicatorId, from, to);
        var current = current(rows);
        return rows.stream()
                .filter(row -> !voided(current.get(row.id())))
                .map(row -> new VitalSignReadingView(
                        row.id(),
                        row.residentId(),
                        row.indicatorId(),
                        values(row, current.get(row.id())),
                        row.level(),
                        row.confirmationStatus(),
                        row.measuredAt(),
                        row.labels()))
                .toList();
    }

    @Override
    public Set<Long> vitalSignIndicatorIds() {
        return catalogs.list(HealthCatalogs.VITAL_SIGN, null, true).stream()
                .filter(item -> item.attributes().vitalSign())
                .map(item -> item.id())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public List<VitalSignHandoverView> aboveThresholdInRange(long floorAreaId, Instant from, Instant to) {
        var rows = records.aboveThreshold(from, to);
        var current = current(rows);
        var result = new ArrayList<VitalSignHandoverView>();
        for (var row : rows) {
            // Q-88: nguồn vượt ngưỡng vẫn giữ mức tại lúc đo sau sửa; bản hủy không còn hiệu lực.
            if (voided(current.get(row.id()))) continue;
            var location = locations.at(row.residentId(), row.measuredAt());
            if (location.isEmpty() || !Objects.equals(location.get().floorId(), floorAreaId)) continue;
            var indicator = catalogs.require(HealthCatalogs.VITAL_SIGN, row.indicatorId());
            String description = indicator.name() + ": "
                    + new TreeMap<>(values(row, current.get(row.id())))
                            .entrySet().stream()
                                    .map(entry -> entry.getKey() + "="
                                            + entry.getValue().toPlainString())
                                    .collect(Collectors.joining(", "))
                    + " " + indicator.attributes().unit();
            result.add(new VitalSignHandoverView(
                    row.id(), row.residentId(), location.get().roomId(), description, row.measuredAt()));
        }
        return List.copyOf(result);
    }

    private Map<Long, CurrentValue> current(List<VitalSignRecord> rows) {
        return corrections.current(
                RECORD_TYPE, rows.stream().map(VitalSignRecord::id).toList());
    }

    private boolean voided(CurrentValue value) {
        return value != null && value.voided();
    }

    private Map<String, BigDecimal> values(VitalSignRecord row, CurrentValue current) {
        if (current != null && current.content() != null) {
            JsonNode content = json.valueToTree(current.content());
            return Map.copyOf(
                    json.convertValue(content.required("values"), new TypeReference<Map<String, BigDecimal>>() {}));
        }
        return row.components().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> entry.getValue().value()));
    }
}

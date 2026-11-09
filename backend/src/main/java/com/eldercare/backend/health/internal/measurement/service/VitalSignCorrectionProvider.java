package com.eldercare.backend.health.internal.measurement.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.api.events.VitalSignCorrected;
import com.eldercare.backend.health.internal.catalog.domain.HealthCatalogs;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdComparator;
import com.eldercare.backend.health.internal.threshold.service.ApplicableThresholdResolver;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Đính chính bản ghi chỉ số {@code M06.VITAL_SIGN_RECORD} qua 000 (FR-019, BR-M06-06): bản gốc không sửa; sau đính
 * chính phát {@link VitalSignCorrected} kèm mức hiện hành tính lại theo phiên bản ngưỡng áp dụng lúc đo. Cảnh báo đã
 * tạo không tự đóng (007 báo người phụ trách).
 */
@Component
public class VitalSignCorrectionProvider implements CorrectableRecordProvider {
    public static final String RECORD_TYPE = "M06.VITAL_SIGN_RECORD";
    private static final Set<Role> RECORDERS =
            Set.of(Role.DOCTOR, Role.NURSING_STAFF, Role.FLOOR_MANAGER, Role.CAREGIVER);

    private final VitalSignRecordStore records;
    private final CatalogApi catalogs;
    private final ApplicableThresholdResolver thresholds;
    private final ResidentLocationApi locations;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public VitalSignCorrectionProvider(
            VitalSignRecordStore records,
            CatalogApi catalogs,
            ApplicableThresholdResolver thresholds,
            ResidentLocationApi locations,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.records = records;
        this.catalogs = catalogs;
        this.thresholds = thresholds;
        this.locations = locations;
        this.events = events;
        this.time = time;
    }

    @Override
    public String recordType() {
        return RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return records.findById(id)
                .map(row -> row.confirmationStatus() == VitalSignConfirmationStatus.CONFIRMED)
                .orElse(false);
    }

    @Override
    public long originalRecorder(long id) {
        return require(id).measuredBy();
    }

    /** Tầng của người cao tuổi lúc đo (FR-019). */
    @Override
    public Optional<Long> floorArea(long id) {
        var row = require(id);
        return locations.at(row.residentId(), row.measuredAt()).map(location -> location.floorId());
    }

    @Override
    public Optional<Long> residentId(long id) {
        return Optional.of(require(id).residentId());
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return RECORDERS.stream().anyMatch(user.roles()::contains);
    }

    @Override
    public void validateContent(Object correctedContent) {
        values(correctedContent);
    }

    @Override
    public Object originalContent(long id) {
        var row = require(id);
        var values = new LinkedHashMap<String, BigDecimal>();
        row.components().forEach((code, component) -> values.put(code, component.value()));
        return Map.of("values", values);
    }

    @Override
    public void afterCorrection(CorrectionView correction) {
        var row = require(correction.originalRecordId());
        VitalSignLevel current =
                correction.type() == CorrectionType.VOID ? null : levelOf(row, values(correction.correctedContent()));
        events.publishEvent(new VitalSignCorrected(
                row.id(),
                row.residentId(),
                row.indicatorId(),
                row.level(),
                current,
                row.measuredAt(),
                "VITAL_SIGN_CORRECTED:" + correction.id(),
                time.now()));
    }

    /** Mức tính lại cho giá trị đã đính chính, theo phiên bản ngưỡng áp dụng lúc đo. */
    public VitalSignLevel levelOf(VitalSignRecord row, Map<String, BigDecimal> values) {
        String code =
                catalogs.require(HealthCatalogs.VITAL_SIGN, row.indicatorId()).code();
        var resolved = thresholds.resolve(row.residentId(), row.indicatorId(), code, row.measuredAt());
        var levels = new ArrayList<VitalSignLevel>();
        values.forEach((component, value) -> levels.add(
                ThresholdComparator.classify(value, resolved.bands().getOrDefault(component, ThresholdBand.none()))));
        return ThresholdComparator.mostSevere(levels);
    }

    /** Nội dung đúng: {@code {"values": {"<thànhPhần>": số}}}, đủ các thành phần của bản gốc không bắt buộc. */
    public static Map<String, BigDecimal> values(Object content) {
        if (!(content instanceof Map<?, ?> map) || !(map.get("values") instanceof Map<?, ?> raw) || raw.isEmpty()) {
            throw new InvalidValueException("Nội dung đúng cần values");
        }
        var result = new LinkedHashMap<String, BigDecimal>();
        raw.forEach((key, value) -> {
            try {
                result.put(String.valueOf(key), new BigDecimal(String.valueOf(value)));
            } catch (NumberFormatException e) {
                throw new InvalidValueException("Giá trị " + key + " không phải số");
            }
        });
        return result;
    }

    private VitalSignRecord require(long id) {
        return records.findById(id).orElseThrow(NotFoundException::new);
    }
}

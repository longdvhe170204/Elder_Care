package com.eldercare.backend.roombedallocation.internal.vehicle.service;

import static com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleSchedule.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.roombedallocation.api.events.VehicleScheduleCorrected;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleBookingCheck;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleSchedule;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 019 T022 (loại {@code M03.VEHICLE_SCHEDULE}; chủ dự án chốt 2026-11-07; 000 FR-026, FR-029): đính chính lịch xe Đã hoàn
 * thành (đã khóa): giờ đi, giờ về dự kiến, giờ xuất phát, trả thực tế, người lái, ghi chú, hoặc Hủy ghi nhận. Đổi giờ dự
 * kiến phải kiểm lại chồng giờ với lịch chưa hủy khác của cùng xe (BR-M03-16; 409 {@code VEHICLE_SCHEDULE_OVERLAP}, không
 * ghi gì); lịch gắn chuyến đổi giờ thì phát {@link VehicleScheduleCorrected} cho 014. Lịch toàn viện không gắn tầng nên đi
 * cách A (người ghi gốc) hoặc C (yêu cầu phê duyệt).
 */
@Component
class VehicleScheduleCorrectionProvider implements CorrectableRecordProvider {
    static final String RECORD_TYPE = "M03.VEHICLE_SCHEDULE";
    private static final Set<Role> BOOKERS = Set.of(Role.FLOOR_MANAGER, Role.OFFICE_STAFF, Role.NURSING_HOME_MANAGER);
    private static final Set<String> FIELDS =
            Set.of("plannedDepartureAt", "plannedReturnAt", "departedAt", "returnedAt", "driverStaffId", "note");

    private final SimpleRecordApi records;
    private final ObjectProvider<CorrectionApi> corrections;
    private final ApplicationEventPublisher events;

    VehicleScheduleCorrectionProvider(
            SimpleRecordApi records, ObjectProvider<CorrectionApi> corrections, ApplicationEventPublisher events) {
        this.records = records;
        this.corrections = corrections;
        this.events = events;
    }

    @Override
    public String recordType() {
        return RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return find(id).map(r -> COMPLETED.equals(r.status())).orElse(false);
    }

    @Override
    public long originalRecorder(long id) {
        return required(id).createdBy();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().stream().anyMatch(BOOKERS::contains);
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || map.isEmpty() || !FIELDS.containsAll(map.keySet()))
            throw new InvalidValueException(
                    "Chỉ đính chính giờ đi, giờ về dự kiến, giờ xuất phát, trả thực tế, người lái, ghi chú");
        map.forEach((key, value) -> {
            switch ((String) key) {
                case "driverStaffId" -> driver(value);
                case "note" -> {
                    if (!(value instanceof String text) || text.length() > 2000)
                        throw new InvalidValueException("note là chữ, tối đa 2000 ký tự");
                }
                case "departedAt", "returnedAt" -> optional(value);
                default -> instant(value);
            }
        });
    }

    @Override
    public Object originalContent(long id) {
        return snapshot(required(id).content());
    }

    @Override
    public void afterCorrection(CorrectionView correction) {
        if (correction.type() == CorrectionType.VOID) return;
        var row = records.lockForUpdate(KIND, correction.originalRecordId());
        var merged = effective(row.content(), correction);
        var from = instant(merged.get("plannedDepartureAt"));
        var to = instant(merged.get("plannedReturnAt"));
        var departed = optional(merged.get("departedAt"));
        var returned = optional(merged.get("returnedAt"));
        if (!to.isAfter(from)) throw new InvalidValueException("Giờ về dự kiến phải sau giờ đi dự kiến");
        if (departed != null && returned != null && !returned.isAfter(departed))
            throw new InvalidValueException("Giờ trả xe phải sau giờ xuất phát");
        var c = row.content();
        boolean plannedChanged = !from.equals(c.plannedDepartureAt()) || !to.equals(c.plannedReturnAt());
        if (!plannedChanged) return;
        var others = records.listByParent(KIND, new EntityRef(AssetKinds.ENTITY, c.vehicleId())).stream()
                .filter(r -> r.id() != row.id() && !CANCELLED.equals(r.status()))
                .map(r -> new VehicleBookingCheck.Slot(
                        r.id(), r.content().plannedDepartureAt(), r.content().plannedReturnAt()))
                .toList();
        if (!VehicleBookingCheck.overlaps(others, from, to).isEmpty())
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "VEHICLE_SCHEDULE_OVERLAP",
                    "Đính chính làm chồng giờ với lịch khác của cùng xe");
        if (c.tripId() != null) events.publishEvent(new VehicleScheduleCorrected(row.id(), c.tripId(), from, to));
    }

    /** Nội dung gốc, rồi các bản đính chính tới và gồm bản đang lưu, theo thứ tự (bản Hủy ghi nhận không đổi nội dung). */
    private Map<String, Object> effective(VehicleSchedule original, CorrectionView current) {
        var values = snapshot(original);
        corrections.getObject().chain(RECORD_TYPE, current.originalRecordId()).corrections().stream()
                .filter(c -> c.sequenceNo() <= current.sequenceNo() && c.type() == CorrectionType.AMEND_CONTENT)
                .sorted(java.util.Comparator.comparingInt(CorrectionView::sequenceNo))
                .forEach(c -> ((Map<?, ?>) c.correctedContent()).forEach((k, v) -> values.put((String) k, v)));
        return values;
    }

    private static Map<String, Object> snapshot(VehicleSchedule s) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("plannedDepartureAt", s.plannedDepartureAt().toString());
        values.put("plannedReturnAt", s.plannedReturnAt().toString());
        values.put("departedAt", s.departedAt() == null ? null : s.departedAt().toString());
        values.put("returnedAt", s.returnedAt() == null ? null : s.returnedAt().toString());
        values.put("driverStaffId", s.driverStaffId());
        values.put("note", s.note());
        return values;
    }

    private SimpleRecord<VehicleSchedule> required(long id) {
        return find(id).orElseThrow(NotFoundException::new);
    }

    private Optional<SimpleRecord<VehicleSchedule>> find(long id) {
        try {
            return Optional.of(records.require(KIND, id));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    private static Instant instant(Object value) {
        try {
            return Instant.parse(String.valueOf(value));
        } catch (DateTimeParseException e) {
            throw new InvalidValueException("Thời điểm phải theo ISO-8601: " + value);
        }
    }

    private static Instant optional(Object value) {
        return value == null ? null : instant(value);
    }

    private static void driver(Object value) {
        if (!(value instanceof Number n) || n.longValue() <= 0 || n.doubleValue() != n.longValue())
            throw new InvalidValueException("driverStaffId là mã nhân viên");
    }
}

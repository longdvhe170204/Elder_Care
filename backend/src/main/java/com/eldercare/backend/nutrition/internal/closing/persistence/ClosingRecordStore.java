package com.eldercare.backend.nutrition.internal.closing.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.Adjustment;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.ExtraPortionRequest;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.FamilyMeal;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.MealSlip;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.RetainedSample;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SlipDiscrepancy;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SpecialPortion;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SupplementaryPortion;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Bản ghi của chốt suất ở bảng chung (R-15; data-model mục 0.1): phiếu, suất đặc biệt, suất ăn người thân
 * ({@code simple_record}); phát sinh, yêu cầu suất bổ sung ({@code record_event}, cha là bữa ngày). Truy cập qua API của chủ
 * bảng; lệnh ghi chạy trong transaction của service.
 */
@Repository
public class ClosingRecordStore {

    public static final String DAILY_MEAL = "M08.DAILY_MEAL";
    public static final String ACTIVE = "ACTIVE";
    public static final String DROPPED = "DROPPED";
    public static final String RECORDED = "RECORDED";
    public static final String CANCELLED = "CANCELLED";
    private static final int MAX_KEY = 120;

    private final SimpleRecordApi records;
    private final RecordEventApi events;

    ClosingRecordStore(SimpleRecordApi records, RecordEventApi events) {
        this.records = records;
        this.events = events;
    }

    public static EntityRef dailyMeal(long dailyMealId) {
        return new EntityRef(DAILY_MEAL, dailyMealId);
    }

    public SimpleRecord<MealSlip> createSlip(MealSlip slip, long by) {
        return records.create(
                MealSlip.KIND,
                new NewSimpleRecord<>(
                        null,
                        dailyMeal(slip.dailyMealId()),
                        "CLOSED",
                        slip,
                        null,
                        null,
                        MealSlip.key(slip.dailyMealId(), slip.floorAreaId()),
                        by,
                        false));
    }

    public Optional<SimpleRecord<MealSlip>> slip(long dailyMealId, long floorAreaId) {
        return records.findByUniqueKey(MealSlip.KIND, MealSlip.key(dailyMealId, floorAreaId));
    }

    public List<SimpleRecord<MealSlip>> slips(long dailyMealId) {
        return records.listByParent(MealSlip.KIND, dailyMeal(dailyMealId));
    }

    public SimpleRecord<MealSlip> lockSlip(long id) {
        return records.lockForUpdate(MealSlip.KIND, id);
    }

    public SimpleRecord<MealSlip> requireSlip(long id) {
        return records.require(MealSlip.KIND, id);
    }

    /** Đổi trạng thái, nội dung phiếu; giữ {@code uniqueKey} (bảng chung ghi lại cột này mỗi lần đổi; DBR-27). */
    public SimpleRecord<MealSlip> changeSlip(SimpleRecord<MealSlip> row, String status, MealSlip content) {
        String key = MealSlip.key(content.dailyMealId(), content.floorAreaId());
        return records.change(
                        MealSlip.KIND, row.id(), row.version(), new RecordChange<>(status, content, null, key, false))
                .after();
    }

    public static EntityRef slipRef(long slipId) {
        return new EntityRef(MealSlip.KIND.code(), slipId);
    }

    public RecordEvent<SupplementaryPortion> addSupplement(SupplementaryPortion portion, long actor, Instant at) {
        return events.add(
                SupplementaryPortion.KIND,
                slipRef(portion.slipId()),
                new NewRecordEvent<>(portion, null, actor, false, at, null, null, portion.floorAreaId(), null));
    }

    public List<RecordEvent<SupplementaryPortion>> supplements(long slipId) {
        return events.list(SupplementaryPortion.KIND, slipRef(slipId));
    }

    public RecordEvent<SupplementaryPortion> supplement(long id) {
        return events.require(SupplementaryPortion.KIND, id);
    }

    public void deliverSupplement(RecordEvent<SupplementaryPortion> row, long by, Instant at) {
        events.updateDraft(SupplementaryPortion.KIND, row.id(), row.content().delivered(by, at));
    }

    /** Nhận phần bổ sung: đặt người, thời điểm nhận rồi khóa dòng (nhóm 3). */
    public void receiveSupplement(RecordEvent<SupplementaryPortion> row, long by, Instant at) {
        events.updateDraft(SupplementaryPortion.KIND, row.id(), row.content().received(by, at));
        events.confirm(SupplementaryPortion.KIND, row.id(), at);
    }

    public RecordEvent<SlipDiscrepancy> addDiscrepancy(
            SlipDiscrepancy discrepancy, long actor, Instant at, long floor) {
        return events.add(
                SlipDiscrepancy.KIND,
                slipRef(discrepancy.slipId()),
                new NewRecordEvent<>(discrepancy, null, actor, false, at, null, null, floor, null));
    }

    public List<RecordEvent<SlipDiscrepancy>> discrepancies(long slipId) {
        return events.list(SlipDiscrepancy.KIND, slipRef(slipId));
    }

    /** Ghi xử lý sai lệch (đặt một lần) rồi khóa dòng; lịch sử sai lệch giữ nguyên (BR-M08-11). */
    public void resolveDiscrepancy(
            RecordEvent<SlipDiscrepancy> row, String resolution, String description, long by, Instant at) {
        events.updateDraft(SlipDiscrepancy.KIND, row.id(), row.content().resolved(resolution, description, by));
        events.confirm(SlipDiscrepancy.KIND, row.id(), at);
    }

    /** Bữa ngày đã có bản ghi lưu mẫu (FR-047, FR-060). */
    public boolean hasRetainedSample(long dailyMealId) {
        return !records.listByParent(RetainedSample.KIND, dailyMeal(dailyMealId))
                .isEmpty();
    }

    public Optional<SimpleRecord<RetainedSample>> retainedSample(long dailyMealId) {
        return records.findByUniqueKey(RetainedSample.KIND, RetainedSample.key(dailyMealId));
    }

    public SimpleRecord<RetainedSample> createSample(RetainedSample sample, long by) {
        return records.create(
                RetainedSample.KIND,
                new NewSimpleRecord<>(
                        null,
                        dailyMeal(sample.dailyMealId()),
                        RECORDED,
                        sample,
                        null,
                        null,
                        RetainedSample.key(sample.dailyMealId()),
                        by,
                        false));
    }

    public SimpleRecord<RetainedSample> lockSample(long id) {
        return records.lockForUpdate(RetainedSample.KIND, id);
    }

    public List<SimpleRecord<RetainedSample>> samplesWithStatus(String status) {
        return records.listByStatus(RetainedSample.KIND, java.util.Set.of(status));
    }

    /** Đổi nội dung, trạng thái bản ghi lưu mẫu; giữ {@code uniqueKey}, khóa dòng khi {@code lock} (hủy mẫu đặt một lần). */
    public void changeSample(SimpleRecord<RetainedSample> row, String status, RetainedSample content, boolean lock) {
        records.change(
                RetainedSample.KIND,
                row.id(),
                row.version(),
                new RecordChange<>(status, content, null, RetainedSample.key(content.dailyMealId()), lock));
    }

    public SimpleRecord<SpecialPortion> createSpecial(long slipId, SpecialPortion portion, long by) {
        return records.create(
                SpecialPortion.KIND,
                new NewSimpleRecord<>(
                        portion.residentId(),
                        new EntityRef(MealSlip.KIND.code(), slipId),
                        ACTIVE,
                        portion,
                        null,
                        null,
                        null,
                        by,
                        false));
    }

    public List<SimpleRecord<SpecialPortion>> specials(long slipId) {
        return records.listByParent(SpecialPortion.KIND, new EntityRef(MealSlip.KIND.code(), slipId));
    }

    public SimpleRecord<SpecialPortion> special(long id) {
        return records.lockForUpdate(SpecialPortion.KIND, id);
    }

    public SimpleRecord<SpecialPortion> changeSpecial(
            SimpleRecord<SpecialPortion> row, String status, SpecialPortion content) {
        return records.change(
                        SpecialPortion.KIND,
                        row.id(),
                        row.version(),
                        new RecordChange<>(status, content, null, null, false))
                .after();
    }

    public SimpleRecord<FamilyMeal> createFamilyMeal(FamilyMeal meal, long by) {
        return records.create(
                FamilyMeal.KIND,
                new NewSimpleRecord<>(
                        meal.residentId(), dailyMeal(meal.dailyMealId()), RECORDED, meal, null, null, null, by, false));
    }

    public List<SimpleRecord<FamilyMeal>> familyMeals(long dailyMealId) {
        return records.listByParent(FamilyMeal.KIND, dailyMeal(dailyMealId));
    }

    /** Hủy suất ăn người thân và khóa dòng: Đã hủy là trạng thái cuối, giữ để truy vết nguồn chi phí (FR-043). */
    public void cancelFamilyMeal(SimpleRecord<FamilyMeal> row, Instant at) {
        FamilyMeal m = row.content();
        records.change(
                FamilyMeal.KIND,
                row.id(),
                row.version(),
                new RecordChange<>(
                        CANCELLED,
                        new FamilyMeal(
                                m.dailyMealId(), m.mealDate(), m.familyMemberId(), m.residentId(), m.stayId(), at),
                        null,
                        null,
                        true));
    }

    /** Phát sinh; trùng {@code uniqueKey} (cùng sự kiện nguồn, bữa ngày, người, loại) là đã ghi (409 ALREADY_PROCESSED). */
    public Optional<RecordEvent<Adjustment>> addAdjustment(Adjustment adjustment, Long actor, Instant at, String key) {
        String unique = shortKey(key);
        if (events.findByUniqueKey(Adjustment.KIND, unique).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(events.add(
                Adjustment.KIND,
                dailyMeal(adjustment.dailyMealId()),
                new NewRecordEvent<>(
                        adjustment,
                        null,
                        actor,
                        actor == null,
                        at,
                        null,
                        adjustment.needsDelivery() ? null : at,
                        adjustment.floorAreaId(),
                        unique)));
    }

    public List<RecordEvent<Adjustment>> adjustments(long dailyMealId) {
        return events.list(Adjustment.KIND, dailyMeal(dailyMealId));
    }

    public RecordEvent<Adjustment> adjustment(long id) {
        return events.require(Adjustment.KIND, id);
    }

    public void confirmAdjustment(RecordEvent<Adjustment> row, long by, Instant at) {
        events.updateDraft(Adjustment.KIND, row.id(), row.content().confirmedBy(by));
        events.confirm(Adjustment.KIND, row.id(), at);
    }

    public RecordEvent<ExtraPortionRequest> addExtra(ExtraPortionRequest request, long actor, Instant at) {
        return events.add(
                ExtraPortionRequest.KIND,
                dailyMeal(request.dailyMealId()),
                new NewRecordEvent<>(request, null, actor, false, at, null, null, request.floorAreaId(), null));
    }

    public List<RecordEvent<ExtraPortionRequest>> extras(long dailyMealId) {
        return events.list(ExtraPortionRequest.KIND, dailyMeal(dailyMealId));
    }

    public Optional<RecordEvent<ExtraPortionRequest>> extra(long id) {
        try {
            return Optional.of(events.require(ExtraPortionRequest.KIND, id));
        } catch (com.eldercare.backend.common.error.NotFoundException notExtra) {
            return Optional.empty();
        }
    }

    public void confirmExtra(RecordEvent<ExtraPortionRequest> row, long by, Instant at) {
        events.updateDraft(ExtraPortionRequest.KIND, row.id(), row.content().confirmedBy(by));
        events.confirm(ExtraPortionRequest.KIND, row.id(), at);
    }

    /** Khóa dài hơn {@value #MAX_KEY} ký tự thì dùng SHA-256 hex (như {@code M08EventListener}). */
    private static String shortKey(String key) {
        if (key.length() <= MAX_KEY) {
            return key;
        }
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}

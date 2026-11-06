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
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SpecialPortion;
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

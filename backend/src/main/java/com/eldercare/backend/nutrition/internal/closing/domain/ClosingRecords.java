package com.eldercare.backend.nutrition.internal.closing.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson.DishChoice;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Nội dung các bản ghi của chốt suất ở bảng chung (data-model mục 6, 7, 0.1; R-15): phiếu bữa ăn, suất đặc biệt, suất ăn
 * người thân ({@code simple_record}); phát sinh, yêu cầu suất bổ sung ({@code record_event}).
 */
public final class ClosingRecords {

    private ClosingRecords() {}

    /** Phiếu bữa ăn mỗi (bữa ngày, tầng/khu) (DBR-27); trạng thái ở cột {@code status}: {@code CLOSED} → … (Phase 7). */
    public record MealSlip(long dailyMealId, long floorAreaId, String source, Set<String> flags) {

        public static final SharedKind<MealSlip> KIND = SharedKind.of("M08.MEAL_SLIP", MealSlip.class);

        public static String key(long dailyMealId, long floorAreaId) {
            return "M08.MEAL_SLIP:" + dailyMealId + ":" + floorAreaId;
        }
    }

    /**
     * Suất đặc biệt, hoặc suất có dấu, của một người trên phiếu (FR-035, FR-036a, FR-037, FR-040a); cha là phiếu. Trạng thái
     * {@code ACTIVE}, {@code DROPPED} (người không còn được tính, hoặc chuyển sang phiếu khác). Dấu cần đối chiếu chỉ hiện
     * cho nhân viên tại tầng (19.3).
     */
    public record SpecialPortion(
            long dailyMealId,
            long residentId,
            long dietId,
            Texture texture,
            List<DishChoice> dishes,
            Set<PortionFlag> flags,
            boolean special,
            Long assignedDishId,
            String assignNote,
            Long assignedBy,
            Instant assignedAt,
            Instant heldServedAt,
            Instant heldDiscardedAt,
            boolean prepared) {

        public static final SharedKind<SpecialPortion> KIND =
                SharedKind.of("M08.SPECIAL_PORTION", SpecialPortion.class);

        public SpecialPortion {
            dishes = dishes == null ? List.of() : List.copyOf(dishes);
            flags = flags == null ? Set.of() : Set.copyOf(flags);
        }

        public static SpecialPortion of(long dailyMealId, CountedPerson person) {
            return new SpecialPortion(
                    dailyMealId,
                    person.residentId(),
                    person.dietId(),
                    person.texture(),
                    person.dishes(),
                    person.flags(),
                    person.special(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false);
        }
    }

    /** Suất ăn người thân ở lại (FR-043; nguồn chi phí cho 010, DBR-15); trạng thái {@code RECORDED}, {@code CANCELLED}. */
    public record FamilyMeal(
            long dailyMealId,
            LocalDate mealDate,
            long familyMemberId,
            long residentId,
            Long stayId,
            Instant cancelledAt) {

        public static final SharedKind<FamilyMeal> KIND = SharedKind.of("M08.FAMILY_MEAL", FamilyMeal.class);
    }

    /**
     * Phát sinh sau chốt (FR-039, FR-040a; thay {@code phat_sinh}, nhóm 3): bản nháp tới khi bếp xác nhận (cột
     * {@code confirmed_at} của bảng chung đặt một lần); phát sinh chỉ ghi nhận ({@code needsDelivery = false}) xác nhận ngay.
     * {@code after} là trạng thái suất của người sau phát sinh (rỗng khi người không còn được tính).
     */
    public record Adjustment(
            long dailyMealId,
            long floorAreaId,
            long dietId,
            int portionCount,
            String changeType,
            String sourceKey,
            boolean needsDelivery,
            String personKey,
            CountedPerson before,
            CountedPerson after,
            Long confirmedBy) {

        public static final SharedKind<Adjustment> KIND = SharedKind.of("M08.ADJUSTMENT", Adjustment.class);

        public Adjustment confirmedBy(long by) {
            return new Adjustment(
                    dailyMealId,
                    floorAreaId,
                    dietId,
                    portionCount,
                    changeType,
                    sourceKey,
                    needsDelivery,
                    personKey,
                    before,
                    after,
                    by);
        }
    }

    /** Yêu cầu suất bổ sung sau giờ bữa (FR-040; thay {@code yeu_cau_suat_bo_sung}, nhóm 3), gửi bếp như phát sinh. */
    public record ExtraPortionRequest(
            long dailyMealId, long floorAreaId, long residentId, String reason, Long confirmedBy) {

        public static final SharedKind<ExtraPortionRequest> KIND =
                SharedKind.of("M08.EXTRA_PORTION_REQUEST", ExtraPortionRequest.class);

        public ExtraPortionRequest confirmedBy(long by) {
            return new ExtraPortionRequest(dailyMealId, floorAreaId, residentId, reason, by);
        }
    }
}

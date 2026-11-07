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

    /**
     * Phiếu bữa ăn mỗi (bữa ngày, tầng/khu) (DBR-27); trạng thái ở cột {@code status}: {@code CLOSED}, {@code PREPARED},
     * {@code DELIVERED}, {@code HAS_DISCREPANCY}, {@code RECEIVED} (bảng phiếu của spec). Người, thời điểm giao, nhận đặt
     * một lần (FR-047, FR-048).
     */
    public record MealSlip(
            long dailyMealId,
            long floorAreaId,
            String source,
            Set<String> flags,
            Long deliveredBy,
            Instant deliveredAt,
            Long receivedBy,
            Instant receivedAt) {

        public static final SharedKind<MealSlip> KIND = SharedKind.of("M08.MEAL_SLIP", MealSlip.class);

        public static MealSlip of(long dailyMealId, long floorAreaId, String source, Set<String> flags) {
            return new MealSlip(dailyMealId, floorAreaId, source, flags, null, null, null, null);
        }

        public static String key(long dailyMealId, long floorAreaId) {
            return "M08.MEAL_SLIP:" + dailyMealId + ":" + floorAreaId;
        }

        public MealSlip delivered(long by, Instant at) {
            return new MealSlip(dailyMealId, floorAreaId, source, flags, by, at, receivedBy, receivedAt);
        }

        public MealSlip received(long by, Instant at) {
            return new MealSlip(dailyMealId, floorAreaId, source, flags, deliveredBy, deliveredAt, by, at);
        }
    }

    /**
     * Phần bổ sung của phiếu (FR-048b; thay {@code phan_bo_sung}, nhóm 3, cha là phiếu): phát sinh cần giao hoặc yêu cầu suất
     * bổ sung bếp đã xác nhận sau khi phiếu Đã giao; đúng một nguồn khác {@code null}. Giao là sửa bản nháp (đặt một lần),
     * nhận là xác nhận dòng (khóa).
     */
    public record SupplementaryPortion(
            long dailyMealId,
            long slipId,
            long floorAreaId,
            Long dietId,
            int portionCount,
            Long adjustmentId,
            Long extraRequestId,
            Long deliveredBy,
            Instant deliveredAt,
            Long receivedBy,
            Instant receivedAt) {

        public static final SharedKind<SupplementaryPortion> KIND =
                SharedKind.of("M08.SUPPLEMENTARY_PORTION", SupplementaryPortion.class);

        public SupplementaryPortion {
            if ((adjustmentId == null) == (extraRequestId == null)) {
                throw new IllegalArgumentException(
                        "Phần bổ sung có đúng một nguồn (phát sinh hoặc yêu cầu suất bổ sung)");
            }
        }

        public SupplementaryPortion delivered(long by, Instant at) {
            return new SupplementaryPortion(
                    dailyMealId,
                    slipId,
                    floorAreaId,
                    dietId,
                    portionCount,
                    adjustmentId,
                    extraRequestId,
                    by,
                    at,
                    receivedBy,
                    receivedAt);
        }

        public SupplementaryPortion received(long by, Instant at) {
            return new SupplementaryPortion(
                    dailyMealId,
                    slipId,
                    floorAreaId,
                    dietId,
                    portionCount,
                    adjustmentId,
                    extraRequestId,
                    deliveredBy,
                    deliveredAt,
                    by,
                    at);
        }
    }

    /**
     * Sai lệch của phiếu hoặc phần bổ sung (FR-048, FR-048b; thay {@code sai_lech_phieu}, nhóm 3, cha là phiếu): bản nháp tới
     * khi bếp ghi xử lý (đặt một lần, rồi khóa). {@code supplementId} khác {@code null} là sai lệch trên phần bổ sung.
     */
    public record SlipDiscrepancy(
            long slipId,
            Long supplementId,
            String content,
            long reportedBy,
            Long reporterStaffId,
            String resolution,
            String description,
            Long resolvedBy) {

        public static final SharedKind<SlipDiscrepancy> KIND =
                SharedKind.of("M08.SLIP_DISCREPANCY", SlipDiscrepancy.class);

        public SlipDiscrepancy resolved(String resolution, String description, long by) {
            return new SlipDiscrepancy(
                    slipId, supplementId, content, reportedBy, reporterStaffId, resolution, description, by);
        }
    }

    /**
     * Bản ghi lưu mẫu của bữa ngày (FR-060 → FR-062; thay {@code luu_mau}, {@code luu_mau_mon}, nhóm 3, cha là bữa ngày):
     * một bản ghi mỗi bữa (khóa {@link #key(long)}), trạng thái {@code RECORDED}, {@code DISCARDED}. {@code expiresAt} là
     * lúc lưu + CFG-M08-06; hủy mẫu ({@code discardedBy}, {@code discardedAt}) đặt một lần rồi khóa dòng. Món ghi thêm sau
     * phát sinh có {@code addedLater}. Bản ghi dạng dấu tối thiểu (không món, không hạn) chỉ để giao phiếu qua kiểm lưu mẫu.
     */
    public record RetainedSample(
            long dailyMealId,
            String storageLocation,
            long retainedBy,
            Instant retainedAt,
            Instant expiresAt,
            List<SampleDish> dishes,
            Long discardedBy,
            Instant discardedAt) {

        public static final SharedKind<RetainedSample> KIND =
                SharedKind.of("M08.RETAINED_SAMPLE", RetainedSample.class);

        /** Một món trong bản ghi: đã lưu mẫu hay chưa, lý do khi không lưu. */
        public record SampleDish(long dishId, boolean retained, String notRetainedReason, boolean addedLater) {}

        public RetainedSample {
            dishes = dishes == null ? List.of() : List.copyOf(dishes);
        }

        public static String key(long dailyMealId) {
            return "M08.RETAINED_SAMPLE:" + dailyMealId;
        }

        public static RetainedSample recorded(
                long dailyMealId,
                String storageLocation,
                long by,
                Instant at,
                Instant expiresAt,
                List<SampleDish> dishes) {
            return new RetainedSample(dailyMealId, storageLocation, by, at, expiresAt, dishes, null, null);
        }

        public RetainedSample withDishes(List<SampleDish> more) {
            return new RetainedSample(
                    dailyMealId, storageLocation, retainedBy, retainedAt, expiresAt, more, discardedBy, discardedAt);
        }

        public RetainedSample discarded(long by, Instant at) {
            return new RetainedSample(dailyMealId, storageLocation, retainedBy, retainedAt, expiresAt, dishes, by, at);
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

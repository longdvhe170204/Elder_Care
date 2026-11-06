package com.eldercare.backend.nutrition.internal.closing.domain;

import com.eldercare.backend.nutrition.api.dto.Texture;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Một người được tính trong lần chốt hoặc sau một phát sinh (data-model mục 6 {@code nguoi_duoc_tinh}, gộp vào cột
 * {@code JSON} của {@code portion_closing}; R-15): tầng/khu, chế độ ăn, kết cấu, món phục vụ, dấu, suất đặc biệt.
 */
public record CountedPerson(
        CountedKind kind,
        long residentId,
        Long familyMemberId,
        Long stayId,
        long floorAreaId,
        long dietId,
        Long assignmentId,
        Texture texture,
        List<DishChoice> dishes,
        Set<PortionFlag> flags,
        boolean special) {

    /** Món phục vụ: món của thực đơn, món thay thế đã chọn (rỗng là không cần thay hoặc thiếu món thay thế). */
    public record DishChoice(long dishId, Long substituteDishId) {}

    public CountedPerson {
        dishes = dishes == null ? List.of() : List.copyOf(dishes);
        flags = flags == null || flags.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(flags));
    }

    /** Khóa người: người cao tuổi, hoặc người thân của lượt ở lại. */
    public String key() {
        return kind == CountedKind.FAMILY ? "FAMILY:" + familyMemberId + ":" + residentId : "RESIDENT:" + residentId;
    }

    /**
     * Cùng nội dung phục vụ (kết cấu, món, dấu, suất đặc biệt): khác thì là phát sinh đổi suất đặc biệt (FR-039). Suất thường
     * (không đặc biệt, không dấu) theo món của thực đơn nên đổi món không là phát sinh của từng người (đổi món ghi phát sinh
     * theo tầng/khu).
     */
    public boolean sameServing(CountedPerson other) {
        boolean plain = !special && !other.special && flags.isEmpty() && other.flags.isEmpty();
        return texture == other.texture
                && (plain || dishes.equals(other.dishes))
                && flags.equals(other.flags)
                && special == other.special;
    }
}

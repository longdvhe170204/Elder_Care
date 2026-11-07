package com.eldercare.backend.nutrition.internal.familyfood.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.util.List;

/**
 * Đồ ăn gia đình mang vào (FR-052 → FR-057, BR-M08-04; thay {@code do_an_gia_dinh}, nhóm 2, R-15): bản ghi {@code simple_record}
 * gắn người cao tuổi; trạng thái ở cột {@code status} ({@code NOT_ALLOWED}, {@code NEEDS_CONFIRMATION}, {@code ALLOWED},
 * {@code ENDED}). {@code systemReasons} là kết quả đối chiếu (mã của {@link FamilyFoodCheck}); {@code movedBecause} là
 * {@code NEW_CONFLICT} hoặc {@code EXPIRED} khi hệ thống chuyển Được sử dụng sang Không sử dụng (FR-055, FR-056).
 */
public record FamilyFood(
        String senderName,
        Long familyMemberId,
        String type,
        String description,
        List<Long> ingredients,
        boolean ingredientsUnknown,
        Instant expiresAt,
        String storageMethod,
        Instant recordedAt,
        List<String> systemReasons,
        String movedBecause,
        Long confirmedBy,
        Instant confirmedAt,
        String confirmNote,
        String rejectReason,
        String endMethod,
        String returnedTo,
        Long endedBy,
        Instant endedAt) {

    public static final SharedKind<FamilyFood> KIND = SharedKind.of("M08.FAMILY_FOOD", FamilyFood.class);

    public static final String NEW_CONFLICT = "NEW_CONFLICT";
    public static final String EXPIRED = "EXPIRED";

    public FamilyFood {
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        systemReasons = systemReasons == null ? List.of() : List.copyOf(systemReasons);
    }

    public static FamilyFood recorded(
            String senderName,
            Long familyMemberId,
            String type,
            String description,
            List<Long> ingredients,
            boolean ingredientsUnknown,
            Instant expiresAt,
            String storageMethod,
            Instant at,
            List<String> systemReasons) {
        return new FamilyFood(
                senderName,
                familyMemberId,
                type,
                description,
                ingredients,
                ingredientsUnknown,
                expiresAt,
                storageMethod,
                at,
                systemReasons,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /** Hệ thống chuyển Được sử dụng sang Không sử dụng (FR-055, FR-056). */
    public FamilyFood movedByRule(List<String> reasons, String because) {
        return new FamilyFood(
                senderName,
                familyMemberId,
                type,
                description,
                ingredients,
                ingredientsUnknown,
                expiresAt,
                storageMethod,
                recordedAt,
                reasons,
                because,
                confirmedBy,
                confirmedAt,
                confirmNote,
                rejectReason,
                endMethod,
                returnedTo,
                endedBy,
                endedAt);
    }

    public FamilyFood approved(long by, Instant at, String note) {
        return new FamilyFood(
                senderName,
                familyMemberId,
                type,
                description,
                ingredients,
                ingredientsUnknown,
                expiresAt,
                storageMethod,
                recordedAt,
                systemReasons,
                movedBecause,
                by,
                at,
                note,
                rejectReason,
                endMethod,
                returnedTo,
                endedBy,
                endedAt);
    }

    public FamilyFood rejected(long by, Instant at, String reason) {
        return new FamilyFood(
                senderName,
                familyMemberId,
                type,
                description,
                ingredients,
                ingredientsUnknown,
                expiresAt,
                storageMethod,
                recordedAt,
                systemReasons,
                movedBecause,
                by,
                at,
                confirmNote,
                reason,
                endMethod,
                returnedTo,
                endedBy,
                endedAt);
    }

    public FamilyFood ended(long by, Instant at, String method, String returnedTo) {
        return new FamilyFood(
                senderName,
                familyMemberId,
                type,
                description,
                ingredients,
                ingredientsUnknown,
                expiresAt,
                storageMethod,
                recordedAt,
                systemReasons,
                movedBecause,
                confirmedBy,
                confirmedAt,
                confirmNote,
                rejectReason,
                method,
                returnedTo,
                by,
                at);
    }
}

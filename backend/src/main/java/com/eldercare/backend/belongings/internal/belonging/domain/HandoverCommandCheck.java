package com.eldercare.backend.belongings.internal.belonging.domain;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.HolderType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Kiểm một lệnh bàn giao trên một đồ ({@code KiemTraLenhBanGiao}; research K-04): bảng trạng thái đồ gửi, phiên bản
 * (FR-011b), người giao là người giữ (FR-014), ảnh (FR-008), lý do, vị trí (FR-011), đồng ý cho tự giữ (FR-015a), trạng
 * thái cuối (FR-027), mức tình trạng (FR-015). Không đọc dữ liệu; dữ kiện do service tính trước. Lệnh Tiếp nhận kiểm
 * riêng vì chưa có đồ.
 */
public final class HandoverCommandCheck {

    private static final Map<HandoverType, Set<BelongingStatus>> FROM = Map.of(
            HandoverType.GIVE_FOR_USE, EnumSet.of(BelongingStatus.HELD),
            HandoverType.TAKE_BACK, EnumSet.of(BelongingStatus.IN_USE),
            HandoverType.TRANSFER_CUSTODY, EnumSet.of(BelongingStatus.HELD, BelongingStatus.DAMAGED),
            HandoverType.RETURN, EnumSet.of(BelongingStatus.HELD, BelongingStatus.IN_USE, BelongingStatus.DAMAGED),
            HandoverType.REPORT_LOST, EnumSet.of(BelongingStatus.HELD, BelongingStatus.IN_USE, BelongingStatus.DAMAGED),
            HandoverType.RECORD_DAMAGE, EnumSet.of(BelongingStatus.HELD, BelongingStatus.IN_USE),
            HandoverType.FOUND, EnumSet.of(BelongingStatus.LOST),
            HandoverType.DISPOSAL, EnumSet.of(BelongingStatus.HELD, BelongingStatus.DAMAGED));

    private static final Set<HandoverType> REASON_REQUIRED =
            EnumSet.of(HandoverType.TRANSFER_CUSTODY, HandoverType.REPORT_LOST, HandoverType.RECORD_DAMAGE);

    private HandoverCommandCheck() {}

    /** Đồ gửi lúc lưu lệnh: trạng thái, người giữ, phiên bản, thứ tự mức tình trạng của bản ghi gần nhất. */
    public record Current(
            BelongingStatus status, HolderType holderType, Long holderId, int version, Integer conditionOrder) {}

    /**
     * Dữ kiện của lệnh trên một đồ. {@code holderAbsenceConfirmed}: người giữ là nhân viên không có ca đang diễn ra hoặc
     * tài khoản không Hoạt động (FR-014); {@code valuable} theo CFG-M12-01 lúc lệnh (FR-009).
     */
    public record Request(
            HandoverType type,
            int version,
            GiverType giverType,
            Long giverId,
            boolean officeStaff,
            boolean holderAbsent,
            boolean holderAbsenceConfirmed,
            Long storageLocationId,
            String reason,
            Integer conditionOrder,
            boolean unusableLevel,
            ConditionHandling conditionHandling,
            int photoCount,
            boolean valuable,
            boolean requiresConsent,
            boolean hasEffectiveConsent,
            boolean residentInFinalState) {

        public static Builder builder(HandoverType type, int version) {
            return new Builder(type, version);
        }

        public static final class Builder {
            private final HandoverType type;
            private final int version;
            private GiverType giverType;
            private Long giverId;
            private boolean officeStaff;
            private boolean holderAbsent;
            private boolean holderAbsenceConfirmed;
            private Long storageLocationId;
            private String reason;
            private Integer conditionOrder;
            private boolean unusableLevel;
            private ConditionHandling conditionHandling;
            private int photoCount;
            private boolean valuable;
            private boolean requiresConsent;
            private boolean hasEffectiveConsent;
            private boolean residentInFinalState;

            private Builder(HandoverType type, int version) {
                this.type = Objects.requireNonNull(type);
                this.version = version;
            }

            public Builder giver(GiverType value, Long id) {
                this.giverType = value;
                this.giverId = id;
                return this;
            }

            public Builder officeStaff(boolean value) {
                this.officeStaff = value;
                return this;
            }

            public Builder holderAbsent(boolean claimed, boolean confirmed) {
                this.holderAbsent = claimed;
                this.holderAbsenceConfirmed = confirmed;
                return this;
            }

            public Builder storageLocationId(Long value) {
                this.storageLocationId = value;
                return this;
            }

            public Builder reason(String value) {
                this.reason = value;
                return this;
            }

            public Builder conditionOrder(Integer value) {
                this.conditionOrder = value;
                return this;
            }

            public Builder unusableLevel(boolean value) {
                this.unusableLevel = value;
                return this;
            }

            public Builder conditionHandling(ConditionHandling value) {
                this.conditionHandling = value;
                return this;
            }

            public Builder photoCount(int value) {
                this.photoCount = value;
                return this;
            }

            public Builder valuable(boolean value) {
                this.valuable = value;
                return this;
            }

            public Builder requiresConsent(boolean value) {
                this.requiresConsent = value;
                return this;
            }

            public Builder hasEffectiveConsent(boolean value) {
                this.hasEffectiveConsent = value;
                return this;
            }

            public Builder residentInFinalState(boolean value) {
                this.residentInFinalState = value;
                return this;
            }

            public Request build() {
                return new Request(
                        type,
                        version,
                        giverType,
                        giverId,
                        officeStaff,
                        holderAbsent,
                        holderAbsenceConfirmed,
                        storageLocationId,
                        reason,
                        conditionOrder,
                        unusableLevel,
                        conditionHandling,
                        photoCount,
                        valuable,
                        requiresConsent,
                        hasEffectiveConsent,
                        residentInFinalState);
            }
        }
    }

    public static HandoverCheckResult check(Current current, Request request) {
        if (request.version() != current.version()) {
            return blocked(request.type(), "STATE_CHANGED");
        }
        // FR-015: chọn "Ghi hư hỏng" khi tình trạng xấu đi thì lệnh thực hiện là Ghi hư hỏng (plan R-15 điểm 6)
        HandoverType type = request.conditionHandling() == ConditionHandling.RECORD_DAMAGE
                ? HandoverType.RECORD_DAMAGE
                : request.type();
        Set<BelongingStatus> from = FROM.get(type);
        if (from == null || !from.contains(current.status())) {
            return blocked(type, "INVALID_STATE");
        }
        BelongingStatus target = target(type, current.status());
        List<String> violations = new ArrayList<>();
        if (type == HandoverType.GIVE_FOR_USE && request.residentInFinalState()) {
            violations.add("RESIDENT_FINAL_STATE");
        }
        giver(current, request, type, violations);
        if ((REASON_REQUIRED.contains(type) || request.holderAbsent())
                && (request.reason() == null || request.reason().isBlank())) {
            violations.add("REASON_REQUIRED");
        }
        if ((target == BelongingStatus.HELD || target == BelongingStatus.DAMAGED)
                && request.storageLocationId() == null) {
            violations.add("STORAGE_LOCATION_REQUIRED");
        }
        boolean photoNeeded = type == HandoverType.RECORD_DAMAGE
                || (request.valuable() && (type == HandoverType.RETURN || type == HandoverType.FOUND));
        if (photoNeeded && request.photoCount() < 1) {
            violations.add("PHOTO_REQUIRED");
        }
        if (type == HandoverType.GIVE_FOR_USE && request.requiresConsent() && !request.hasEffectiveConsent()) {
            violations.add("SELF_CUSTODY_CONSENT_REQUIRED");
        }
        condition(current, request, type, violations);
        return new HandoverCheckResult(type, violations.isEmpty() ? target : null, violations);
    }

    private static void giver(Current current, Request request, HandoverType type, List<String> violations) {
        if (request.holderAbsent()) {
            boolean valid = request.officeStaff()
                    && current.holderType() == HolderType.STAFF
                    && request.holderAbsenceConfirmed();
            if (!valid) {
                violations.add("HOLDER_NOT_ABSENT");
            }
            return;
        }
        boolean exempt = (type == HandoverType.RETURN && current.status() == BelongingStatus.IN_USE)
                || type == HandoverType.REPORT_LOST
                || type == HandoverType.FOUND
                || type == HandoverType.DISPOSAL;
        if (exempt) {
            return;
        }
        GiverType holderAsGiver = current.holderType() == HolderType.RESIDENT ? GiverType.RESIDENT : GiverType.STAFF;
        if (current.holderType() == null
                || request.giverType() != holderAsGiver
                || !Objects.equals(request.giverId(), current.holderId())) {
            violations.add("GIVER_NOT_HOLDER");
        }
    }

    private static void condition(Current current, Request request, HandoverType type, List<String> violations) {
        if (request.conditionOrder() == null || type == HandoverType.RECORD_DAMAGE) {
            return;
        }
        if (request.unusableLevel()) {
            violations.add("UNUSABLE_LEVEL_ONLY_VIA_RECORD_DAMAGE");
            return;
        }
        boolean worse = current.conditionOrder() != null && request.conditionOrder() > current.conditionOrder();
        if (worse && request.conditionHandling() == null) {
            violations.add("CONDITION_HANDLING_REQUIRED");
        }
    }

    private static BelongingStatus target(HandoverType type, BelongingStatus from) {
        return switch (type) {
            case INTAKE, TAKE_BACK, FOUND -> BelongingStatus.HELD;
            case GIVE_FOR_USE -> BelongingStatus.IN_USE;
            case TRANSFER_CUSTODY, RETURN_BLOCKED -> from;
            case RETURN -> BelongingStatus.RETURNED;
            case REPORT_LOST -> BelongingStatus.LOST;
            case RECORD_DAMAGE -> BelongingStatus.DAMAGED;
            case DISPOSAL -> BelongingStatus.DISPOSED;
        };
    }

    private static HandoverCheckResult blocked(HandoverType type, String code) {
        return new HandoverCheckResult(type, null, List.of(code));
    }
}

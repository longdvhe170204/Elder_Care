package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.HolderType;
import com.eldercare.backend.belongings.internal.belonging.domain.ConditionHandling;
import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCommandCheck;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCommandCheck.Current;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCommandCheck.Request;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverType;
import org.junit.jupiter.api.Test;

/**
 * 013 T004 (research K-04; bảng trạng thái đồ gửi, FR-008, FR-011, FR-011b, FR-014, FR-015, FR-015a, FR-027): hàm thuần
 * kiểm một lệnh bàn giao trên một đồ.
 */
class HandoverCommandCheckTest {

    private static final long NURSE = 11L;
    private static final long OTHER_NURSE = 12L;
    private static final long RESIDENT = 7L;

    private static Current heldBy(long staffId) {
        return new Current(BelongingStatus.HELD, HolderType.STAFF, staffId, 3, 1);
    }

    private static Current inUse() {
        return new Current(BelongingStatus.IN_USE, HolderType.RESIDENT, RESIDENT, 3, 1);
    }

    private static Request.Builder request(HandoverType type) {
        return Request.builder(type, 3)
                .giver(GiverType.STAFF, NURSE)
                .conditionOrder(1)
                .storageLocationId(5L);
    }

    @Test
    void FR_014_officeStaffMayActForAbsentStaffHolder_withReason() {
        var ok = HandoverCommandCheck.check(
                heldBy(OTHER_NURSE),
                request(HandoverType.TRANSFER_CUSTODY)
                        .officeStaff(true)
                        .holderAbsent(true, true)
                        .reason("Người giữ vắng mặt")
                        .build());
        assertThat(ok.violations()).isEmpty();

        var notAbsent = HandoverCommandCheck.check(
                heldBy(OTHER_NURSE),
                request(HandoverType.TRANSFER_CUSTODY)
                        .officeStaff(true)
                        .holderAbsent(true, false)
                        .reason("Người giữ vắng mặt")
                        .build());
        assertThat(notAbsent.violations()).containsExactly("HOLDER_NOT_ABSENT");

        var nurseClaimsAbsence = HandoverCommandCheck.check(
                heldBy(OTHER_NURSE),
                request(HandoverType.TRANSFER_CUSTODY)
                        .holderAbsent(true, true)
                        .reason("Người giữ vắng mặt")
                        .build());
        assertThat(nurseClaimsAbsence.violations()).containsExactly("HOLDER_NOT_ABSENT");
    }

    @Test
    void FR_011_storageLocationRequired_whenTargetHeldOrDamaged() {
        var result = HandoverCommandCheck.check(
                inUse(),
                Request.builder(HandoverType.TAKE_BACK, 3)
                        .giver(GiverType.RESIDENT, RESIDENT)
                        .conditionOrder(1)
                        .build());

        assertThat(result.violations()).containsExactly("STORAGE_LOCATION_REQUIRED");
    }

    @Test
    void FR_015_choosingRecordDamage_turnsCommandIntoRecordDamage() {
        var result = HandoverCommandCheck.check(
                inUse(),
                Request.builder(HandoverType.TAKE_BACK, 3)
                        .giver(GiverType.RESIDENT, RESIDENT)
                        .conditionOrder(2)
                        .storageLocationId(5L)
                        .conditionHandling(ConditionHandling.RECORD_DAMAGE)
                        .reason("Gãy gọng")
                        .photoCount(1)
                        .build());

        assertThat(result.violations()).isEmpty();
        assertThat(result.effectiveType()).isEqualTo(HandoverType.RECORD_DAMAGE);
        assertThat(result.target()).isEqualTo(BelongingStatus.DAMAGED);
    }
}

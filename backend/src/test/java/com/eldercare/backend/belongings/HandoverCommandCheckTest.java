package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.HolderType;
import com.eldercare.backend.belongings.internal.belonging.domain.ConditionHandling;
import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCheckResult;
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
    void FR_011b_staleVersion_isStateChanged() {
        var result = HandoverCommandCheck.check(
                heldBy(NURSE),
                Request.builder(HandoverType.GIVE_FOR_USE, 2)
                        .giver(GiverType.STAFF, NURSE)
                        .build());

        assertThat(result.violations()).containsExactly("STATE_CHANGED");
    }

    @Test
    void stateTable_rejectsTransitionNotInTable() {
        assertThat(HandoverCommandCheck.check(
                                heldBy(NURSE), request(HandoverType.TAKE_BACK).build())
                        .violations())
                .containsExactly("INVALID_STATE");
        assertThat(HandoverCommandCheck.check(
                                heldBy(NURSE), request(HandoverType.FOUND).build())
                        .violations())
                .containsExactly("INVALID_STATE");
    }

    @Test
    void stateTable_giveForUse_takeBack_transferCustody_targets() {
        var give = HandoverCommandCheck.check(
                heldBy(NURSE), request(HandoverType.GIVE_FOR_USE).build());
        assertThat(give.violations()).isEmpty();
        assertThat(give.target()).isEqualTo(BelongingStatus.IN_USE);

        var takeBack = HandoverCommandCheck.check(
                inUse(),
                Request.builder(HandoverType.TAKE_BACK, 3)
                        .giver(GiverType.RESIDENT, RESIDENT)
                        .conditionOrder(1)
                        .storageLocationId(5L)
                        .build());
        assertThat(takeBack.violations()).isEmpty();
        assertThat(takeBack.target()).isEqualTo(BelongingStatus.HELD);

        var damaged = new Current(BelongingStatus.DAMAGED, HolderType.STAFF, NURSE, 3, 3);
        var transfer = HandoverCommandCheck.check(
                damaged,
                request(HandoverType.TRANSFER_CUSTODY)
                        .conditionOrder(3)
                        .reason("Đổi tủ")
                        .build());
        assertThat(transfer.violations()).isEmpty();
        assertThat(transfer.target()).isEqualTo(BelongingStatus.DAMAGED);
    }

    @Test
    void FR_014_giverMustBeCurrentHolder() {
        var result = HandoverCommandCheck.check(
                heldBy(OTHER_NURSE), request(HandoverType.GIVE_FOR_USE).build());

        assertThat(result.violations()).containsExactly("GIVER_NOT_HOLDER");
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
    void FR_008_photoRequired_forValuableReturnAndFound_andAnyDamage() {
        assertThat(HandoverCommandCheck.check(
                                heldBy(NURSE),
                                request(HandoverType.RETURN).valuable(true).build())
                        .violations())
                .containsExactly("PHOTO_REQUIRED");
        var lostCurrent = new Current(BelongingStatus.LOST, null, null, 3, null);
        assertThat(HandoverCommandCheck.check(
                                lostCurrent,
                                Request.builder(HandoverType.FOUND, 3)
                                        .conditionOrder(1)
                                        .storageLocationId(5L)
                                        .valuable(true)
                                        .build())
                        .violations())
                .containsExactly("PHOTO_REQUIRED");
        assertThat(HandoverCommandCheck.check(
                                heldBy(NURSE),
                                request(HandoverType.RECORD_DAMAGE)
                                        .conditionOrder(3)
                                        .reason("Nứt màn hình")
                                        .build())
                        .violations())
                .containsExactly("PHOTO_REQUIRED");
        assertThat(HandoverCommandCheck.check(
                                heldBy(NURSE),
                                request(HandoverType.RETURN)
                                        .valuable(true)
                                        .photoCount(1)
                                        .build())
                        .violations())
                .isEmpty();
    }

    @Test
    void FR_011_reasonRequired_forTransferLostDamage() {
        assertThat(HandoverCommandCheck.check(
                                heldBy(NURSE),
                                request(HandoverType.TRANSFER_CUSTODY).build())
                        .violations())
                .containsExactly("REASON_REQUIRED");
        assertThat(HandoverCommandCheck.check(
                                heldBy(NURSE),
                                request(HandoverType.RECORD_DAMAGE)
                                        .conditionOrder(3)
                                        .photoCount(1)
                                        .reason("  ")
                                        .build())
                        .violations())
                .containsExactly("REASON_REQUIRED");
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
    void FR_015a_giveForUse_ofConsentType_needsEffectiveConsent() {
        assertThat(HandoverCommandCheck.check(
                                heldBy(NURSE),
                                request(HandoverType.GIVE_FOR_USE)
                                        .requiresConsent(true)
                                        .build())
                        .violations())
                .containsExactly("SELF_CUSTODY_CONSENT_REQUIRED");
        assertThat(HandoverCommandCheck.check(
                                heldBy(NURSE),
                                request(HandoverType.GIVE_FOR_USE)
                                        .requiresConsent(true)
                                        .hasEffectiveConsent(true)
                                        .build())
                        .violations())
                .isEmpty();
    }

    @Test
    void FR_027_giveForUse_blockedAfterFinalStatus() {
        assertThat(HandoverCommandCheck.check(
                                heldBy(NURSE),
                                request(HandoverType.GIVE_FOR_USE)
                                        .residentInFinalState(true)
                                        .build())
                        .violations())
                .containsExactly("RESIDENT_FINAL_STATE");
    }

    @Test
    void FR_015_worseCondition_requiresChoice_acknowledgeKeepsCommand() {
        var noChoice = HandoverCommandCheck.check(
                inUse(),
                Request.builder(HandoverType.TAKE_BACK, 3)
                        .giver(GiverType.RESIDENT, RESIDENT)
                        .conditionOrder(2)
                        .storageLocationId(5L)
                        .build());
        assertThat(noChoice.violations()).containsExactly("CONDITION_HANDLING_REQUIRED");

        var acknowledged = HandoverCommandCheck.check(
                inUse(),
                Request.builder(HandoverType.TAKE_BACK, 3)
                        .giver(GiverType.RESIDENT, RESIDENT)
                        .conditionOrder(2)
                        .storageLocationId(5L)
                        .conditionHandling(ConditionHandling.ACKNOWLEDGE)
                        .build());
        assertThat(acknowledged.violations()).isEmpty();
        assertThat(acknowledged.effectiveType()).isEqualTo(HandoverType.TAKE_BACK);
        assertThat(acknowledged.target()).isEqualTo(BelongingStatus.HELD);
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

    @Test
    void resultCarriesNoTarget_whenBlocked() {
        HandoverCheckResult result = HandoverCommandCheck.check(
                heldBy(OTHER_NURSE), request(HandoverType.GIVE_FOR_USE).build());

        assertThat(result.allowed()).isFalse();
    }
}

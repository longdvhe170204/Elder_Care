package com.eldercare.backend.family.internal.right.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.Type;
import com.eldercare.backend.family.internal.right.domain.RightRequestCheck.ActiveWish;
import com.eldercare.backend.family.internal.right.domain.RightRequestCheck.ConflictingRight;
import com.eldercare.backend.family.internal.right.domain.RightRequestCheck.HeldRights;
import com.eldercare.backend.family.internal.right.domain.RightRequestCheck.Person;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * 012 T014 ({@code KiemTraYeuCauTest}; research F-06, F-06a; FR-014, FR-017, FR-020b, FR-020c): áp ngay khi lập, yêu cầu
 * tự bỏ của người thân, trái mong muốn theo loại và chiều bật/tắt, ngoại lệ tắt đón, người đại diện theo pháp luật,
 * người xác nhận đặt/thôi người đại diện.
 */
class RightRequestCheckTest {

    private static final Person X = new Person(10L, "079000000010");

    @Test
    void FR_017_turningOffPickupOrSelfReturn_appliesOnCreation() {
        assertThat(RightRequestCheck.appliesOnCreation(Type.RIGHT, FamilyRight.PICKUP_ALLOWED, false))
                .isTrue();
        assertThat(RightRequestCheck.appliesOnCreation(Type.SELF_RETURN, null, false))
                .isTrue();
        assertThat(RightRequestCheck.appliesOnCreation(Type.RIGHT, FamilyRight.PICKUP_ALLOWED, true))
                .isFalse();
        assertThat(RightRequestCheck.appliesOnCreation(Type.RIGHT, FamilyRight.VIEW_COSTS, false))
                .isFalse();
        assertThat(RightRequestCheck.appliesOnCreation(Type.SELF_RETURN, null, true))
                .isFalse();
    }

    @ParameterizedTest
    @EnumSource(FamilyRight.class)
    void FR_014_anyFamilyMember_mayTurnOffOwnRight(FamilyRight right) {
        assertThat(RightRequestCheck.selfRestriction(Type.RIGHT, right, false, true))
                .isTrue();
        assertThat(RightRequestCheck.selfRestriction(Type.RIGHT, right, true, true))
                .isFalse();
        assertThat(RightRequestCheck.selfRestriction(Type.RIGHT, right, false, false))
                .isFalse();
    }

    @Test
    void FR_014_representativeAndSelfReturnRequests_areNotSelfRestrictions() {
        assertThat(RightRequestCheck.selfRestriction(Type.REMOVE_REPRESENTATIVE, null, false, true))
                .isFalse();
        assertThat(RightRequestCheck.selfRestriction(Type.SELF_RETURN, null, false, true))
                .isFalse();
    }

    @Test
    void FR_020b_grantingAgainstUnwantedWish_ofSameType_conflicts() {
        ActiveWish noPickup = wish(1, 10L, null, VisitPickupWish.Type.PICKUP, false, true);
        ActiveWish noVisit = wish(2, 10L, null, VisitPickupWish.Type.VISIT, false, true);

        assertThat(RightRequestCheck.conflictingWish(
                        Type.RIGHT, FamilyRight.PICKUP_ALLOWED, true, X, List.of(noPickup, noVisit), false))
                .contains(noPickup);
        assertThat(RightRequestCheck.conflictingWish(
                        Type.RIGHT, FamilyRight.REGISTER_VISITS, true, X, List.of(noPickup, noVisit), false))
                .contains(noVisit);
        assertThat(RightRequestCheck.conflictingWish(
                        Type.RIGHT, FamilyRight.VIEW_COSTS, true, X, List.of(noPickup, noVisit), false))
                .isEmpty();
    }

    @Test
    void FR_020b_turningOffRightOfWantedPerson_conflicts_grantingDoesNot() {
        ActiveWish wantsVisit = wish(3, 10L, null, VisitPickupWish.Type.VISIT, true, true);

        assertThat(RightRequestCheck.conflictingWish(
                        Type.RIGHT, FamilyRight.REGISTER_VISITS, false, X, List.of(wantsVisit), false))
                .contains(wantsVisit);
        assertThat(RightRequestCheck.conflictingWish(
                        Type.RIGHT, FamilyRight.REGISTER_VISITS, true, X, List.of(wantsVisit), false))
                .isEmpty();
    }

    @Test
    void FR_020b_samePerson_byRelationship_orIdDocument_notByOtherPeople() {
        ActiveWish byDocument = wish(4, null, "079000000010", VisitPickupWish.Type.PICKUP, false, true);
        ActiveWish otherPerson = wish(5, 11L, "079000000099", VisitPickupWish.Type.PICKUP, false, true);

        assertThat(RightRequestCheck.conflictingWish(
                        Type.RIGHT, FamilyRight.PICKUP_ALLOWED, true, X, List.of(byDocument), false))
                .contains(byDocument);
        assertThat(RightRequestCheck.conflictingWish(
                        Type.RIGHT, FamilyRight.PICKUP_ALLOWED, true, X, List.of(otherPerson), false))
                .isEmpty();
    }

    @Test
    void FR_020b_onlyWishesFromTheResident_count() {
        ActiveWish fromFamily = wish(6, 10L, null, VisitPickupWish.Type.PICKUP, false, false);

        assertThat(RightRequestCheck.conflictingWish(
                        Type.RIGHT, FamilyRight.PICKUP_ALLOWED, true, X, List.of(fromFamily), false))
                .isEmpty();
    }

    @Test
    void FR_020c_requestByLegalRepresentative_isNotConflicting() {
        ActiveWish noPickup = wish(7, 10L, null, VisitPickupWish.Type.PICKUP, false, true);

        assertThat(RightRequestCheck.conflictingWish(
                        Type.RIGHT, FamilyRight.PICKUP_ALLOWED, true, X, List.of(noPickup), true))
                .isEmpty();
    }

    @Test
    void FR_020b_removingPickup_isRecordedAsConflicting_butNeverWaitsForManager() {
        ActiveWish wantsPickup = wish(8, 10L, null, VisitPickupWish.Type.PICKUP, true, true);
        var conflict = RightRequestCheck.conflictingWish(
                Type.RIGHT, FamilyRight.PICKUP_ALLOWED, false, X, List.of(wantsPickup), false);

        assertThat(conflict).contains(wantsPickup);
        assertThat(RightRequestCheck.needsManagerApproval(Type.RIGHT, FamilyRight.PICKUP_ALLOWED, false, conflict))
                .isFalse();
        assertThat(RightRequestCheck.needsManagerApproval(
                        Type.RIGHT,
                        FamilyRight.REGISTER_VISITS,
                        true,
                        RightRequestCheck.conflictingWish(
                                Type.RIGHT,
                                FamilyRight.REGISTER_VISITS,
                                true,
                                X,
                                List.of(wish(9, 10L, null, VisitPickupWish.Type.VISIT, false, true)),
                                false)))
                .isTrue();
    }

    @Test
    void FR_020b_selfReturnAndRepresentativeRequests_haveNoWishCheck() {
        ActiveWish noPickup = wish(1, 10L, null, VisitPickupWish.Type.PICKUP, false, true);

        assertThat(RightRequestCheck.conflictingWish(Type.SELF_RETURN, null, true, X, List.of(noPickup), false))
                .isEmpty();
        assertThat(RightRequestCheck.conflictingWish(Type.SET_REPRESENTATIVE, null, true, X, List.of(noPickup), false))
                .isEmpty();
    }

    @Test
    void FR_020b_conflictingRightsAtRead_areEnabledRightsAgainstUnwantedWishes() {
        HeldRights x = new HeldRights(10L, "079000000010", Set.of(FamilyRight.PICKUP_ALLOWED));
        HeldRights y = new HeldRights(11L, null, Set.of());
        List<ActiveWish> wishes = List.of(
                wish(1, 10L, null, VisitPickupWish.Type.PICKUP, false, true),
                wish(2, 10L, null, VisitPickupWish.Type.VISIT, false, true),
                wish(3, 11L, null, VisitPickupWish.Type.PICKUP, false, true));

        assertThat(RightRequestCheck.conflictingRights(List.of(x, y), wishes))
                .containsExactly(new ConflictingRight(10L, FamilyRight.PICKUP_ALLOWED, 1L));
    }

    @Test
    void FR_005_representativeChange_isConfirmedOnlyByAnotherRepresentative() {
        assertThat(RightRequestCheck.mayConfirm(Type.SET_REPRESENTATIVE, 20L, 10L))
                .isTrue();
        assertThat(RightRequestCheck.mayConfirm(Type.SET_REPRESENTATIVE, 10L, 10L))
                .isFalse();
        assertThat(RightRequestCheck.mayConfirm(Type.REMOVE_REPRESENTATIVE, 10L, 10L))
                .isFalse();
        // FR-016 (Edge Cases): người đại diện xác nhận cả yêu cầu tác động tới chính mình
        assertThat(RightRequestCheck.mayConfirm(Type.RIGHT, 10L, 10L)).isTrue();
    }

    private static ActiveWish wish(
            long id,
            Long relationshipId,
            String idDocumentNumber,
            VisitPickupWish.Type type,
            boolean wanted,
            boolean fromResident) {
        return new ActiveWish(id, relationshipId, idDocumentNumber, type, wanted, fromResident);
    }
}

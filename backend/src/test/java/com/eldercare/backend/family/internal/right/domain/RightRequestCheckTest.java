package com.eldercare.backend.family.internal.right.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.Type;
import com.eldercare.backend.family.internal.right.domain.RightRequestCheck.Person;
import org.junit.jupiter.api.Test;

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

    @Test
    void FR_014_representativeAndSelfReturnRequests_areNotSelfRestrictions() {
        assertThat(RightRequestCheck.selfRestriction(Type.REMOVE_REPRESENTATIVE, null, false, true))
                .isFalse();
        assertThat(RightRequestCheck.selfRestriction(Type.SELF_RETURN, null, false, true))
                .isFalse();
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
}

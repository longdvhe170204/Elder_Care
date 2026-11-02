package com.eldercare.backend.common.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * FR-007, FR-010, FR-020, research B-04, B-05 (009 T004, T041): khuôn người nhận có đủ các nhóm (a) → (f); mỗi
 * nhóm loại (a) mang cách thay; danh mục trường sức khỏe cấm ở nội dung rút gọn.
 */
class RecipientGroupTest {

    @Test
    void FR_007_recipientGroup_coversGroupsAToF() {
        assertThat(leaves(RecipientGroup.class))
                .containsExactlyInAnyOrder(
                        // (a) nhiệm vụ
                        "AssignedNurse",
                        "AssignedStaff",
                        "OnShiftStaff",
                        "ShiftLead",
                        "OnDutyDoctor",
                        "FloorManager",
                        "TripLeader",
                        "DepartmentLead",
                        // (b), (c)
                        "FacilityRole",
                        "Approvers",
                        // (d) gắn bản ghi nguồn
                        "Staff",
                        "FamilyMember",
                        // (e) quan hệ người thân
                        "PrimaryContact",
                        "Representative",
                        "FamilyWithRight",
                        // (f) chính người cao tuổi (009 T041, Q-366)
                        "Resident");
    }

    @Test
    void FR_010_dutyGroups_carrySubstitution() {
        assertThat(leaves(RecipientGroup.DutyGroup.class))
                .containsExactlyInAnyOrder(
                        "AssignedNurse",
                        "AssignedStaff",
                        "OnShiftStaff",
                        "ShiftLead",
                        "OnDutyDoctor",
                        "FloorManager",
                        "TripLeader",
                        "DepartmentLead");
        RecipientGroup.DutyGroup g = new RecipientGroup.FloorManager(3L, new Substitution.NoSubstitute());
        assertThat(g.substitution()).isInstanceOf(Substitution.NoSubstitute.class);
    }

    @Test
    void FR_013_familyRights() {
        assertThat(FamilyRight.values())
                .containsExactly(
                        FamilyRight.VIEW_HEALTH,
                        FamilyRight.VIEW_COSTS,
                        FamilyRight.REGISTER_VISITS,
                        FamilyRight.RECEIVE_URGENT);
    }

    @Test
    void FR_020_healthFieldCatalog() {
        assertThat(HealthFields.KEYS)
                .containsExactlyInAnyOrder(
                        "diagnosis",
                        "allergies",
                        "underlyingConditions",
                        "vitalSigns",
                        "medication",
                        "assessmentResult",
                        "wishes",
                        "incidentContent");
    }

    /** Các record cụ thể của một kiểu đóng, kể cả qua interface trung gian. */
    private static Stream<String> leaves(Class<?> sealed) {
        return Arrays.stream(sealed.getPermittedSubclasses())
                .flatMap(c -> c.isSealed() ? leaves(c) : Stream.of(c.getSimpleName()));
    }
}

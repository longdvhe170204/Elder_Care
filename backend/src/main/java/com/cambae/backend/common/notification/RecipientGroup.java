package com.cambae.backend.common.notification;

import com.cambae.backend.common.Role;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Nhóm người nhận ({@code NhomNhan}; 009 research B-04, contracts/module-interfaces.md mục 1.1). 009 phân giải nhóm
 * thành người nhận tại thời điểm gửi theo phân công hiện tại (BR-M13-04).
 */
@com.fasterxml.jackson.annotation.JsonTypeInfo(
        use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME,
        property = "kind")
@com.fasterxml.jackson.annotation.JsonSubTypes({
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value = RecipientGroup.AssignedNurse.class,
            name = "ASSIGNED_NURSE"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value = RecipientGroup.AssignedStaff.class,
            name = "ASSIGNED_STAFF"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value = RecipientGroup.OnShiftStaff.class,
            name = "ON_SHIFT_STAFF"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = RecipientGroup.ShiftLead.class, name = "SHIFT_LEAD"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value = RecipientGroup.OnDutyDoctor.class,
            name = "ON_DUTY_DOCTOR"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value = RecipientGroup.FloorManager.class,
            name = "FLOOR_MANAGER"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = RecipientGroup.TripLeader.class, name = "TRIP_LEADER"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value = RecipientGroup.DepartmentLead.class,
            name = "DEPARTMENT_LEAD"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value = RecipientGroup.FacilityRole.class,
            name = "FACILITY_ROLE"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = RecipientGroup.Approvers.class, name = "APPROVERS"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = RecipientGroup.Staff.class, name = "STAFF"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value = RecipientGroup.FamilyMember.class,
            name = "FAMILY_MEMBER"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value = RecipientGroup.PrimaryContact.class,
            name = "PRIMARY_CONTACT"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value = RecipientGroup.Representative.class,
            name = "REPRESENTATIVE"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value = RecipientGroup.FamilyWithRight.class,
            name = "FAMILY_WITH_RIGHT"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = RecipientGroup.Resident.class, name = "RESIDENT")
})
public sealed interface RecipientGroup {

    /** Nhóm loại (a) theo nhiệm vụ: luôn kèm cách thay khi không có người (FR-010, Q-110). */
    sealed interface DutyGroup extends RecipientGroup {
        Substitution substitution();
    }

    /** (a) Điều dưỡng phụ trách người cao tuổi. */
    record AssignedNurse(long residentId, Substitution substitution) implements DutyGroup {}

    /** (a) Nhân viên được phân công cho người cao tuổi hoặc phòng, có thể giới hạn vai trò. */
    record AssignedStaff(
            Optional<Long> residentId, Optional<Long> roomId, Optional<Role> role, Substitution substitution)
            implements DutyGroup {}

    /** (a) Nhân viên đang có ca ở tầng/khu vực, lọc theo vai trò và chứng chỉ. */
    record OnShiftStaff(long floorAreaId, Set<Role> roles, Set<String> certificates, Substitution substitution)
            implements DutyGroup {}

    /** (a) Người phụ trách ca đang diễn ra của tầng/khu vực. */
    record ShiftLead(long floorAreaId, Substitution substitution) implements DutyGroup {}

    /** (a) Bác sĩ trực. */
    record OnDutyDoctor(Substitution substitution) implements DutyGroup {}

    /** (a) Trưởng tầng được giao tầng/khu vực. */
    record FloorManager(long floorAreaId, Substitution substitution) implements DutyGroup {}

    /** (a) Trưởng đoàn của chuyến. */
    record TripLeader(long tripId, Substitution substitution) implements DutyGroup {}

    /** (a) Phụ trách bộ phận. */
    record DepartmentLead(long departmentId, Substitution substitution) implements DutyGroup {}

    /** (b) Mọi tài khoản đang hoạt động có vai trò toàn viện. */
    record FacilityRole(Role role) implements RecipientGroup {}

    /**
     * (c) Người có quyền duyệt một loại yêu cầu: 000 tính sẵn danh sách tài khoản đã lọc theo giới hạn vai trò duyệt
     * (sửa theo plan 004, Q-338).
     */
    record Approvers(String requestType, List<Long> accountIds) implements RecipientGroup {}

    /** (d) Nhân viên gắn với bản ghi nguồn, kể cả người tạm nhận (căn cứ {@code TAM_NHAN}). */
    record Staff(long staffId, String basis) implements RecipientGroup {}

    /** (d) Người thân gắn với bản ghi nguồn; không có quan hệ thì chỉ phần {@code GENERAL} (Q-112). */
    record FamilyMember(long familyMemberId, String basis) implements RecipientGroup {}

    /** (e) Người liên hệ chính của người cao tuổi. */
    record PrimaryContact(long residentId) implements RecipientGroup {}

    /** (e) Người đại diện của người cao tuổi. */
    record Representative(long residentId) implements RecipientGroup {}

    /** (e) Người thân có một quyền với người cao tuổi. */
    record FamilyWithRight(long residentId, FamilyRight right) implements RecipientGroup {}

    /**
     * (f) Chính người cao tuổi qua tài khoản của mình (Q-366): chỉ mức Nhẹ, không có phần sức khỏe, chi phí (FR-022a);
     * không có tài khoản thì không có người nhận.
     */
    record Resident(long residentId) implements RecipientGroup {}
}

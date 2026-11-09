package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.billing.FakeProcurementPrescriptions;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 019 T028 (US7 kịch bản 8; FR-008d, BR-M11-17, Q-337; quickstart Q34, Q35): Điều dưỡng (không là Phụ trách bộ phận) lập,
 * gửi yêu cầu loại thuốc cho người cao tuổi trong phạm vi phân công, dòng gắn đơn nguồn viện chưa kết thúc của đúng người
 * đó; người lập phải xem được thông tin sức khỏe; ghi đã mua vẫn chỉ Phụ trách bộ phận. Đơn thuốc qua bản giả của
 * {@code ProcurementPrescriptionPort} (M07 cài, chủ dự án duyệt đảo chiều 2026-11-06).
 */
@Import(FakeProcurementPrescriptions.Config.class)
class FR_008d_MedicationRequestIT extends ProcurementTestBase {

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeProcurementPrescriptions prescriptions;

    long medical, residentA, residentB, facilityA, familyA, stoppedA, facilityB;
    TestUsers.TestUser nurse, doctorLead;

    @BeforeEach
    void medicationFixture() {
        var users = new TestUsers(jdbc, time);
        medical = TestUsers.nextId();
        residentA = TestUsers.nextId();
        residentB = TestUsers.nextId();
        nurse = users.staff(Role.NURSING_STAFF);
        doctorLead = users.staff(Role.DOCTOR);
        duties.departmentLead(medical, doctorLead.staffId()).department(nurse.staffId(), medical);
        assignments.assign(
                nurse.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        residentA,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
        facilityA = TestUsers.nextId();
        familyA = TestUsers.nextId();
        stoppedA = TestUsers.nextId();
        facilityB = TestUsers.nextId();
        // Đơn gia đình gửi, đơn Đã ngừng không phải đơn nguồn viện chưa kết thúc nên không đặt vào bản giả
        prescriptions.reset();
        prescriptions.facility(residentA, facilityA).facility(residentB, facilityB);
        as(nurse);
    }

    String medication(long department, Long resident, Long prescription, boolean withPriority) {
        return "{\"departmentId\":" + department + ",\"type\":\"MEDICATION\",\"reason\":\"Amlodipin sắp hết\""
                + (withPriority ? ",\"priority\":\"URGENT\",\"neededBy\":\"2026-11-12T08:00:00+07:00\"" : "")
                + ",\"lines\":[{\"name\":\"Amlodipin\",\"quantity\":30,\"estimatedUnitPrice\":2000"
                + (resident == null ? "" : ",\"residentId\":" + resident)
                + (prescription == null ? "" : ",\"prescriptionId\":" + prescription)
                + ",\"activeIngredient\":\"Amlodipin\",\"strength\":\"5 mg\",\"dosageForm\":\"Viên nén\","
                + "\"unit\":\"viên\"}]}";
    }

    @Test
    void Q34_nurseDraftsAndSubmitsMedicationRequestForResidentInScope() throws Exception {
        long id = created(medication(medical, residentA, facilityA, true));
        assertThat(jdbc.queryForMap(
                        "SELECT department_id, created_by_staff_id, priority FROM procurement_request WHERE id=?", id))
                .containsEntry("department_id", medical)
                .containsEntry("created_by_staff_id", nurse.staffId())
                .containsEntry("priority", "URGENT");
        command(id, "submit").andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("PENDING_APPROVAL");
        assertThat(recipients(id, "PENDING_APPROVAL"))
                .containsExactly(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + id + ")].lines[0].prescriptionId")
                        .value((int) facilityA));
        command(id, "withdraw").andExpect(status().isOk());
        command(id, "cancel").andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("CANCELLED");
    }
}

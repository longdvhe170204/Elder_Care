package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.billing.api.ProcurementMedicationLineApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.medication.api.PrescriptionApi;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.dto.MedicationSummary;
import com.eldercare.backend.medication.api.dto.PrescriptionView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.billing.FakeProcurementIntakes;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * 019 T028 (US7 kịch bản 8; FR-008d, BR-M11-17, Q-337; quickstart Q34, Q35): Điều dưỡng (không là Phụ trách bộ phận) lập,
 * gửi yêu cầu loại thuốc cho người cao tuổi trong phạm vi phân công, dòng gắn đơn nguồn viện chưa kết thúc của đúng người
 * đó; người lập phải xem được thông tin sức khỏe; dòng đã ghi đã mua đọc được theo id dòng cho 006; "Đã nhận" theo
 * {@code ProcurementIntakePort} (bản giả chung, D cài ở 006). Đơn thuốc qua bean thật {@link PrescriptionApi} của 006
 * bọc spy: 006 chỉ lập đơn qua repository nội bộ, không có đường công khai gọn, nên spy trả đơn nguồn viện chưa kết thúc
 * của người A, B (lọc nguồn, trạng thái là truy vấn của 006); đơn gia đình gửi, đơn Đã ngừng không có trong danh sách đó.
 * Nhánh 422, Điều dưỡng lập loại khác thuốc và ghi đã mua ở {@code FR_008d_MedicationRequestRulesTest}.
 */
@Import(FakeProcurementIntakes.Config.class)
class FR_008d_MedicationRequestIT extends ProcurementTestBase {

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeProcurementIntakes intakes;

    @Autowired
    ProcurementMedicationLineApi medicationLines;

    @Autowired
    FieldVisibilityApi visibility;

    @MockitoSpyBean
    PrescriptionApi prescriptions;

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
        doReturn(List.of(facility(facilityA, residentA))).when(prescriptions).unendedFacilityPrescriptions(residentA);
        doReturn(List.of(facility(facilityB, residentB))).when(prescriptions).unendedFacilityPrescriptions(residentB);
        intakes.reset();
        as(nurse);
    }

    static PrescriptionView facility(long id, long resident) {
        return new PrescriptionView(
                id,
                resident,
                "DINH_KY",
                "HIEU_LUC",
                MedicationSource.FACILITY,
                null,
                null,
                new MedicationSummary(id, "Amlodipin", "Amlodipin", "5 mg", "Viên nén"),
                null,
                null);
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

    /** Id dòng đầu và "Đã nhận" của yêu cầu {@code id} trong danh sách (api-019 GET /procurement-requests). */
    long lineId(long id) throws Exception {
        String body = mvc.perform(get(URL)).andReturn().getResponse().getContentAsString();
        var node = JSON.readTree(body)
                .valueStream()
                .filter(n -> n.get("id").asLong() == id)
                .findFirst()
                .orElseThrow();
        assertThat(node.get("lines").get(0).get("id").isIntegralNumber())
                .as("lines[0].id")
                .isTrue();
        return node.get("lines").get(0).get("id").asLong();
    }

    Object received(long id) throws Exception {
        String body = mvc.perform(get(URL)).andReturn().getResponse().getContentAsString();
        var node = JSON.readTree(body)
                .valueStream()
                .filter(n -> n.get("id").asLong() == id)
                .findFirst()
                .orElseThrow()
                .get("received");
        return node == null || node.isNull() ? null : node.asBoolean();
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
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + id + ")].lines[0].prescriptionId")
                        .value((int) facilityA));
        command(id, "withdraw").andExpect(status().isOk());
        command(id, "cancel").andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("CANCELLED");
    }

    @Test
    void T028_prescriptionMustBeFacilityUnendedOfTheLineResidentAndAuthorMustSeeHealth() throws Exception {
        // Đơn gia đình gửi, đơn Đã ngừng, đơn của người khác → 409 CONDITION_NOT_MET
        for (long prescription : List.of(familyA, stoppedA, facilityB))
            create(medication(medical, residentA, prescription, true))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.unmetConditions[0].code").value("PRESCRIPTION_NOT_FACILITY_UNENDED"));
        // Người cao tuổi ngoài phạm vi phân công của Điều dưỡng
        create(medication(medical, residentB, facilityB, true)).andExpect(status().isForbidden());
        // Phụ trách bộ phận Bếp không xem được HEALTH: 403 và không được cấp thêm quyền xem
        as(kitchenLead);
        var resident = new Target("M01.RESIDENT", Long.toString(residentA), residentA, null);
        create(medication(kitchen, residentA, facilityA, true)).andExpect(status().isForbidden());
        assertThat(visibility.canView(user, InfoGroup.HEALTH, resident)).isNotEqualTo(Visibility.FULL);
        // Phụ trách bộ phận Y tế xem được thì lập được
        as(doctorLead);
        created(medication(medical, residentB, facilityB, true));
    }

    @Test
    void T028_purchasedLineIsReadByLineIdAndReceivedFollowsIntake() throws Exception {
        long id = created(medication(medical, residentA, facilityA, true));
        long line = lineId(id);
        assertThat(received(id)).isEqualTo(false);
        command(id, "submit").andExpect(status().isOk());
        as(manager);
        command(id, "approve", "{\"approvedAmount\":60000}").andExpect(status().isOk());
        assertThat(lineId(id)).as("id dòng ổn định sau Nháp").isEqualTo(line);
        assertThat(medicationLines.lineRecordedPurchased(line))
                .as("trước khi ghi đã mua")
                .isEmpty();

        as(doctorLead);
        command(id, "record-purchased", purchase(60000, doctorLead)).andExpect(status().isOk());
        // Người lập chuyển bộ phận: dòng vẫn mang bộ phận của yêu cầu
        duties.department(nurse.staffId(), kitchen);
        var view = medicationLines.lineRecordedPurchased(line).orElseThrow();
        assertThat(view.procurementLineId()).isEqualTo(line);
        assertThat(view.procurementRequestId()).isEqualTo(id);
        assertThat(view.residentId()).isEqualTo(residentA);
        assertThat(view.prescriptionId()).isEqualTo(facilityA);
        assertThat(view.medicationName()).isEqualTo("Amlodipin");
        assertThat(view.strength()).isEqualTo("5 mg");
        assertThat(view.quantity()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(view.unit()).isEqualTo("viên");
        assertThat(view.creatorStaffId()).isEqualTo(nurse.staffId());
        assertThat(view.departmentId()).isEqualTo(medical);
        assertThat(medicationLines.lineRecordedPurchased(line + 1))
                .as("không có dòng thứ hai")
                .isEmpty();

        // "Đã nhận": mọi dòng có lần tiếp nhận Đạt (006 cài ProcurementIntakePort)
        assertThat(received(id)).isEqualTo(false);
        intakes.passed(id, line);
        assertThat(received(id)).isEqualTo(true);
        // Yêu cầu loại khác thuốc không có "Đã nhận"
        as(kitchenLead);
        assertThat(received(created(ingredientBody(kitchen)))).isNull();
        // Mua thuốc theo yêu cầu không sinh khoản chi phí của người cao tuổi (Q-337)
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE resident_id=?", Integer.class, residentA))
                .isZero();
    }
}

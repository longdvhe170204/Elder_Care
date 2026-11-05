package com.eldercare.backend.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.medication.api.PrescriptionApi;
import com.eldercare.backend.medication.api.dto.ActiveMedicationView;
import com.eldercare.backend.medication.api.dto.MedicationSummary;
import com.eldercare.backend.resident.api.AllergenApi;
import com.eldercare.backend.resident.api.AssessmentApi;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.AllergenGroup;
import com.eldercare.backend.resident.api.dto.AllergenView;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.HealthItemView;
import com.eldercare.backend.resident.api.dto.IdentityView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.json.JsonMapper;

/**
 * Nền cho IT quy trình khẩn cấp (US5; T038, T039, T103): như {@link CriticalCareTestBase}, thêm bản giả Mockito của
 * nguồn thẻ khẩn cấp (001 mục sức khỏe, dị nguyên, lưu ý chăm sóc, lệnh trạng thái; 006 thuốc đang dùng), quyền xem
 * nhóm sức khỏe của 002 và nơi lưu tệp giả. Người cao tuổi A: dị ứng Penicillin, bệnh nền tăng huyết áp, hai thuốc,
 * lưu ý chăm sóc, người liên hệ chính.
 */
@Import(FakeFileConfig.class)
abstract class EmergencyTestBase extends CriticalCareTestBase {
    static final String INCIDENTS = "/api/v1/incidents";

    @MockitoSpyBean
    HealthItemApi healthItems;

    @MockitoSpyBean
    AllergenApi allergens;

    @MockitoSpyBean
    AssessmentApi assessments;

    @MockitoSpyBean
    PrescriptionApi prescriptions;

    @MockitoSpyBean
    ResidentStatusApi statuses;

    @MockitoSpyBean
    FieldVisibilityApi visibility;

    long penicillin;

    @BeforeEach
    void prepareEmergencySources() {
        penicillin = TestUsers.nextId();
        when(residents.identity(resident))
                .thenReturn(new IdentityView(resident, "NCT-" + resident, "Nguyễn Thị An", Optional.empty()));
        doReturn(List.of(
                        new HealthItemView(
                                1,
                                resident,
                                HealthItemType.ALLERGY,
                                Optional.of(penicillin),
                                Optional.empty(),
                                false,
                                false),
                        new HealthItemView(
                                2,
                                resident,
                                HealthItemType.CHRONIC_CONDITION,
                                Optional.empty(),
                                Optional.of("Tăng huyết áp"),
                                false,
                                false)))
                .when(healthItems)
                .active(eq(resident), any(HealthItemType[].class));
        doReturn(Optional.of(new AllergenView(penicillin, "Penicillin", AllergenGroup.values()[0])))
                .when(allergens)
                .allergen(penicillin);
        doAnswer(call -> Map.of(resident, Optional.of("Nguy cơ ngã cao, hỗ trợ khi di chuyển")))
                .when(assessments)
                .currentCareNote(any());
        doReturn(List.of(medication("Amlodipin"), medication("Metformin")))
                .when(prescriptions)
                .activeMedications(resident);
        doReturn(Visibility.FULL).when(visibility).canView(any(), any(), any());
    }

    private static ActiveMedicationView medication(String name) {
        return new ActiveMedicationView(
                TestUsers.nextId(),
                new MedicationSummary(TestUsers.nextId(), name, name, "5 mg", "Viên"),
                "1 viên",
                "Uống",
                List.of(),
                null,
                null,
                "ACTIVE");
    }

    /** Sự cố Khẩn cấp ghi qua HTTP; trả mã sự cố. */
    long criticalIncident() throws Exception {
        String body = "{\"residentId\":" + resident + ",\"incidentTypeId\":" + typeId("FALL") + ",\"discoveredAt\":\""
                + time.now() + "\",\"description\":\"Khó thở\",\"level\":\"CRITICAL\"}";
        var response = postNew(nurse, INCIDENTS, body)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return JsonMapper.builder()
                .build()
                .readTree(response.getContentAsString())
                .path("incident")
                .path("id")
                .asLong();
    }
}

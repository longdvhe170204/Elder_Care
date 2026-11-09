package com.eldercare.backend.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.incident.api.dto.EmergencyFamilyContactView;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import com.eldercare.backend.resident.api.EndOfLifeWishApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.EndOfLifeWishView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Nền cho IT nguy kịch (US11; T043): 001 (trạng thái, nguyện vọng), 008 (Bác sĩ trực) và 012 (người đại diện) là bản
 * giả Mockito chung cho ba lớp để dùng một context; người cao tuổi Đang lưu trú, chưa có nguyện vọng, có Bác sĩ trực.
 */
abstract class CriticalCareTestBase extends IncidentHttpTestBase {
    static final String FLAGS = "/api/v1/residents/{residentId}/critical-flags";
    static final String CONFIRM = "/api/v1/critical-flags/{id}/confirm";
    static final String REMOVE = "/api/v1/critical-flags/{id}/remove";
    static final String RECONFIRM = "/api/v1/alerts/{id}/wish-reconfirmation";
    static final String CHOICE = "/api/v1/alerts/{id}/execute-choice";

    @MockitoBean
    ResidentApi residents;

    @MockitoBean
    EndOfLifeWishApi wishes;

    @MockitoBean
    AlertStaffPort staff;

    @MockitoBean
    EmergencyFamilyPort family;

    @MockitoBean
    ResidentLocationApi locations;

    TestUser doctor;
    long onDutyDoctorStaffId;
    long representativeId;

    @BeforeEach
    void prepareCriticalCare() {
        doctor = users.staff(Role.DOCTOR);
        onDutyDoctorStaffId = doctor.staffId();
        representativeId = TestUsers.nextId();
        when(residents.status(resident)).thenReturn(ResidentStatus.IN_RESIDENCE);
        when(locations.current(resident))
                .thenReturn(Optional.of(new LocationView(null, null, floor, "Tầng", 1L, "P1", 1L, "G1")));
        when(wishes.current(anyLong())).thenReturn(Optional.empty());
        when(staff.onDutyDoctors(any())).thenReturn(List.of(onDutyDoctorStaffId));
        when(staff.assignedNurse(anyLong(), any())).thenReturn(Optional.of(nurse.staffId()));
        when(family.representatives(resident))
                .thenReturn(List.of(new EmergencyFamilyContactView(
                        representativeId, "Trần Thị Đại Diện", Optional.of("0901000001"))));
        when(family.primaryContact(resident))
                .thenReturn(Optional.of(
                        new EmergencyFamilyContactView(TestUsers.nextId(), "Lê Văn Liên Hệ", Optional.empty())));
    }

    void noOnDutyDoctor() {
        when(staff.onDutyDoctors(any())).thenReturn(List.of());
    }

    void activeWish(WishChoice choice) {
        when(wishes.current(resident))
                .thenReturn(Optional.of(new EndOfLifeWishView(
                        TestUsers.nextId(),
                        resident,
                        choice,
                        "Con gái",
                        null,
                        WishSignerType.RESIDENT,
                        time.now(),
                        WishRecordSource.ADMISSION)));
    }

    record Flag(long id, long alertId) {}

    /** Ghi dấu chính thức; 201 không có thân theo api-007, nên đọc mã dấu và cảnh báo vừa tạo từ bảng. */
    Flag flag(TestUser as) throws Exception {
        post(as, FLAGS, resident, "{\"type\":\"OFFICIAL\",\"assessment\":\"Suy hô hấp nặng\"}")
                .andExpect(status().isCreated());
        return latestFlag();
    }

    Flag latestFlag() {
        long id = jdbc.queryForObject(
                "SELECT MAX(id) FROM simple_record WHERE kind = 'M05.CRITICAL_FLAG' AND resident_id = ?",
                Long.class,
                resident);
        long alertId = jdbc.queryForObject("SELECT id FROM alert WHERE critical_flag_id = ?", Long.class, id);
        return new Flag(id, alertId);
    }

    /** Cảnh báo nguy kịch sang Đang xử lý bằng hai lệnh của Điều dưỡng phụ trách. */
    void startHandling(long alertId) throws Exception {
        post(nurse, "/api/v1/alerts/{id}/receive", alertId, "{}").andExpect(status().isOk());
        post(nurse, "/api/v1/alerts/{id}/start-handling", alertId, "{}").andExpect(status().isOk());
    }

    String reconfirmation(String result) {
        return "{\"contactedPerson\":{\"familyMemberId\":" + representativeId
                + ",\"fullName\":\"Trần Thị Đại Diện\"},\"channel\":\"PHONE_CALL\",\"contactedAt\":\""
                + time.now().minusSeconds(60) + "\",\"result\":\"" + result + "\"}";
    }

    String flagStatus(long flagId) {
        return jdbc.queryForObject("SELECT status FROM simple_record WHERE id = ?", String.class, flagId);
    }

    int count(String kind, long alertId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM record_event WHERE kind = ? AND entity_id = ?", Integer.class, kind, alertId);
    }
}

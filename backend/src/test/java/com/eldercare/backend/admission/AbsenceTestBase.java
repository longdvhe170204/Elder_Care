package com.eldercare.backend.admission;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.dto.PickupRecordView;
import com.eldercare.backend.admission.api.events.LongAbsenceManagerNotice;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.internal.status.service.ResidentStatusService;
import com.eldercare.backend.support.fake.admission.FakeDayCareAttendance;
import com.eldercare.backend.support.fake.admission.FakePickupRecords;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Nền chung cho test lượt vắng của 004 US6 (T049, T050): người cao tuổi nội trú dài hạn có hợp đồng Hiệu lực thật bắt đầu
 * hôm nay, lệnh trạng thái của 001 đi qua bean tham gia thật của M02, bản ghi đón lấy từ bản giả 012, đồng hồ giả lập. Lớp con
 * khai {@code @AutoConfigureMockMvc} và {@code @WithMockUser}. Việc hằng ngày dùng chung dấu {@code M02.DAILY_RUN} nên được dọn
 * trước và sau mỗi test.
 */
abstract class AbsenceTestBase extends ContractTestBase {

    private static final AtomicLong RECORD_IDS = new AtomicLong(700_000);
    protected static final long PICKER = 7;

    @Autowired
    protected FakePickupRecords pickups;

    @Autowired
    protected FakeDayCareAttendance attendance;

    protected LocalDate d0;

    @BeforeEach
    void absenceSetup() {
        pickups.reset();
        attendance.reset();
        d0 = today();
    }

    /** Người cao tuổi Đang lưu trú nội trú dài hạn, hợp đồng Hiệu lực bắt đầu hôm nay. */
    protected long inResidence() throws Exception {
        long residentId = residentWithRepresentative();
        activeContract(residentId, body(residentId));
        setStatus(residentId, ResidentStatus.IN_RESIDENCE.name());
        user.set(office.user());
        return residentId;
    }

    protected static Instant at(LocalDate day, int hour, int minute) {
        return day.atTime(hour, minute).atZone(BusinessTime.ZONE).toInstant();
    }

    protected void clockAt(LocalDate day, int hour, int minute) {
        clock.setTo(at(day, hour, minute));
    }

    protected ObjectNode grantDetails(long residentId, String type, Instant expectedReturnAt) {
        long recordId = RECORD_IDS.incrementAndGet();
        pickups.valid(residentId, PICKER, new PickupRecordView(recordId, residentId, "FAMILY"));
        ObjectNode details = JSON.createObjectNode();
        details.put("absenceType", type);
        details.put("expectedReturnAt", expectedReturnAt.toString());
        details.put("pickedUpById", PICKER);
        details.put("pickupRecordId", recordId);
        details.put("handoverStaffId", office.staffId());
        details.put("takesMedication", false);
        return details;
    }

    /** Cho tạm vắng lúc đồng hồ đang chỉ; trả mã lượt vắng. */
    protected long grant(long residentId, String type, Instant expectedReturnAt) {
        user.set(office.user());
        statusService.grantTemporaryAbsence(
                ResidentStatusService.target(residentId),
                new Reason("Gia đình đón"),
                grantDetails(residentId, type, expectedReturnAt));
        return latestAbsence(residentId);
    }

    /** Chuyển viện do Bác sĩ thực hiện (Phụ lục 27: Bác sĩ, Điều dưỡng phụ trách). */
    protected long transferOut(long residentId) {
        user.set(doctor.user());
        ObjectNode details = JSON.createObjectNode();
        details.put("transferUnit", "Bệnh viện Đa khoa");
        details.put("handoverStaffId", doctor.staffId());
        details.putObject("basis").put("type", "FAMILY_INFORMATION").put("note", "Gia đình báo");
        statusService.transferOut(ResidentStatusService.target(residentId), new Reason("Chuyển viện thử"), details);
        return latestAbsence(residentId);
    }

    protected void recordReturn(long residentId, Instant returnedAt) {
        user.set(office.user());
        ObjectNode details = JSON.createObjectNode();
        if (returnedAt != null) {
            details.put("returnedAt", returnedAt.toString());
        }
        statusService.recordReturn(ResidentStatusService.target(residentId), new Reason("Về viện"), details);
    }

    protected long latestAbsence(long residentId) {
        return jdbc.queryForObject(
                "SELECT id FROM absence WHERE kind = 'ABSENCE' AND resident_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                residentId);
    }

    protected void runJobOn(LocalDate day) {
        noonOf(day);
        dailyRunner.catchUp();
    }

    protected List<LongAbsenceManagerNotice> managerNotices(long absenceId) {
        return events.stream(LongAbsenceManagerNotice.class)
                .filter(event -> event.absenceId() == absenceId)
                .toList();
    }

    // ---- HTTP -----------------------------------------------------------------------------------------------------

    protected ResultActions releaseBed(long absenceId, String reason) throws Exception {
        user.set(manager.user());
        return mvc.perform(post("/api/v1/absences/" + absenceId + "/release-bed")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"" + reason + "\"}"));
    }

    protected ResultActions postAs(com.eldercare.backend.support.TestUsers.TestUser who, String url, String body)
            throws Exception {
        user.set(who.user());
        return mvc.perform(
                post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected JsonNode detail(long absenceId) throws Exception {
        user.set(office.user());
        return JSON.readTree(mvc.perform(get("/api/v1/absences/" + absenceId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }
}

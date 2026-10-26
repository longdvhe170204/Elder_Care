package com.cambae.backend.m02;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.m02.FakeFinalPeriods;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Nền chung cho test kết thúc lưu trú của 004 US7 (T057): người cao tuổi nội trú dài hạn có hợp đồng Hiệu lực (từ {@link
 * AbsenceTestBase}), lệnh Kết thúc lưu trú của 001 đi qua bean tham gia thật của M02; 010, 013, 006, 007, 017 là bản giả
 * {@code otherModules} (mã điều kiện theo research T-09), 010 {@code KyCuoiPort} là {@link FakeFinalPeriods}. Lớp con khai
 * {@code @AutoConfigureMockMvc} và {@code @WithMockUser}.
 */
abstract class EndingTestBase extends AbsenceTestBase {

    protected static final String B_BELONGINGS = "END_B_BELONGINGS";
    protected static final String D_ALERTS = "END_D_ALERTS_INCIDENTS";
    protected static final String H_SETTLEMENT = "END_H_SETTLEMENT";
    protected static final String H_CONFIRMATION = "END_H_SETTLEMENT_CONFIRMATION";

    @Autowired
    protected FakeFinalPeriods finalPeriods;

    protected TestUser accountant;

    @BeforeEach
    void endingSetup() {
        finalPeriods.reset();
        accountant = users.staff(Role.ACCOUNTANT);
        otherModules.interestedIn(
                StatusCommand.COMPLETE_ADMISSION, StatusCommand.CANCEL_ADMISSION, StatusCommand.END_RESIDENCY);
    }

    @Override
    protected void cleanDailyState() throws SQLException {
        WaitingListTestBase.admin(
                "DELETE FROM daily_run WHERE task IN ('CONTRACTS', 'REMINDERS', 'ABSENCE_DAYS', 'ENDINGS')");
    }

    // ---- Dữ liệu ----------------------------------------------------------------------------------------------------

    protected ObjectNode endingBody(long residentId, String endCase, LocalDate expectedEndDate) {
        ObjectNode body = JSON.createObjectNode();
        body.put("residentId", residentId);
        body.put("endCase", endCase);
        body.put("expectedEndDate", expectedEndDate.toString());
        body.put("requestedBy", "Con gái ông A");
        body.put("reason", "Gia đình đưa về chăm sóc tại nhà");
        return body;
    }

    protected ResultActions createEndingRaw(TestUser who, ObjectNode body) throws Exception {
        user.set(who.user());
        return mvc.perform(post("/api/v1/residency-endings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()));
    }

    /** Hồ sơ kết thúc Xuất viện do Hành chính lập (201); trả mã hồ sơ. */
    protected long createEnding(long residentId, LocalDate expectedEndDate) throws Exception {
        return JSON.readTree(createEndingRaw(office, endingBody(residentId, "DISCHARGE", expectedEndDate))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("id")
                .asLong();
    }

    protected JsonNode ending(long endingId) throws Exception {
        user.set(office.user());
        return JSON.readTree(mvc.perform(get("/api/v1/residency-endings/" + endingId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    protected JsonNode conditions(long endingId) throws Exception {
        user.set(office.user());
        return JSON.readTree(mvc.perform(get("/api/v1/residency-endings/" + endingId + "/conditions"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    /** Dòng điều kiện theo mã trong danh sách. */
    protected static JsonNode line(JsonNode conditions, String code) {
        for (JsonNode line : conditions.get("conditions")) {
            if (code.equals(line.get("code").asString())) {
                return line;
            }
        }
        throw new AssertionError("Không có dòng " + code + " trong " + conditions);
    }

    protected ObjectNode handoverBody() {
        ObjectNode body = JSON.createObjectNode();
        body.put("recipientType", "FAMILY_MEMBER");
        body.put("familyMemberId", 9_100_001);
        body.put("handoverAt", time.now().toString());
        body.put("handoverStaffId", office.staffId());
        return body;
    }

    protected ResultActions handoverRaw(TestUser who, long endingId, ObjectNode body) throws Exception {
        return postAs(who, "/api/v1/residency-endings/" + endingId + "/handover", body.toString());
    }

    protected void handover(long endingId) throws Exception {
        handoverRaw(office, endingId, handoverBody()).andExpect(status().isCreated());
    }

    protected ResultActions changeDateRaw(long endingId, LocalDate date, String reason) throws Exception {
        return postAs(
                office,
                "/api/v1/residency-endings/" + endingId + "/change-date",
                "{\"expectedEndDate\":\"" + date + "\",\"reason\":\"" + reason + "\"}");
    }

    protected ResultActions cancelRaw(TestUser who, long endingId, String reason) throws Exception {
        return postAs(who, "/api/v1/residency-endings/" + endingId + "/cancel", "{\"reason\":\"" + reason + "\"}");
    }

    protected ResultActions requestExceptionRaw(TestUser who, long endingId, String conditionCode) throws Exception {
        return postAs(
                who,
                "/api/v1/residency-endings/" + endingId + "/request-exception",
                "{\"conditionCode\":\"" + conditionCode + "\",\"reason\":\"Đã xử lý ngoài hệ thống\"}");
    }

    /** Quản lý viện duyệt yêu cầu; ngoại lệ có ngày hiệu lực là ngày duyệt nên áp dụng ngay. */
    protected void approve(long requestId) throws Exception {
        user.set(manager.user());
        String etag = mvc.perform(get("/api/v1/approval-requests/" + requestId))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        mvc.perform(post("/api/v1/approval-requests/" + requestId + "/approve")
                        .with(csrf())
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Đồng ý ngoại lệ\"}"))
                .andExpect(status().isOk());
        user.set(office.user());
    }

    /** Lập ngoại lệ bằng Hành chính rồi Quản lý viện duyệt; trả mã yêu cầu. */
    protected long approvedException(long endingId, String conditionCode) throws Exception {
        long requestId = JSON.readTree(requestExceptionRaw(office, endingId, conditionCode)
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("approvalRequestId")
                .asLong();
        approve(requestId);
        return requestId;
    }

    // ---- Bản giả các module khác
    // -------------------------------------------------------------------------------------

    /** Module khác báo điều kiện chưa thỏa của lệnh Kết thúc lưu trú. */
    protected void otherUnmet(String code, boolean waivable) {
        otherModules.unmet(new Condition(code, "Chưa đạt " + code, "FR-063", waivable));
    }

    // ---- Lệnh Kết thúc lưu trú (001)
    // ----------------------------------------------------------------------------------

    protected ObjectNode endDetails(long endingId, Instant effectiveAt) {
        ObjectNode details = JSON.createObjectNode();
        details.put("residencyEndingId", endingId);
        details.put("effectiveAt", effectiveAt.toString());
        return details;
    }

    protected ResidentStatusService.Outcome endResidency(long residentId, long endingId, Instant effectiveAt) {
        user.set(office.user());
        return statusService.endResidency(
                ResidentStatusService.target(residentId),
                new Reason("Kết thúc lưu trú"),
                endDetails(endingId, effectiveAt));
    }

    protected BusinessException endResidencyFailure(long residentId, long endingId, Instant effectiveAt) {
        return failure(() -> endResidency(residentId, endingId, effectiveAt));
    }

    /** Người cao tuổi Đang lưu trú có hồ sơ kết thúc dự kiến {@code expectedEndDate}, đã bàn giao, đồng hồ ở trưa ngày đó. */
    protected record ReadyEnding(long residentId, long endingId) {}

    protected ReadyEnding readyToEnd(LocalDate expectedEndDate) throws Exception {
        long residentId = inResidence();
        long endingId = createEnding(residentId, expectedEndDate);
        handover(endingId);
        noonOf(expectedEndDate);
        return new ReadyEnding(residentId, endingId);
    }

    protected String endingStatus(long endingId) {
        return jdbc.queryForObject("SELECT status FROM residency_ending WHERE id = ?", String.class, endingId);
    }

    protected ResidentStatus residentStatus(long residentId) {
        return currentStatus(residentId);
    }

    protected List<String> calls() {
        return finalPeriods.calls();
    }
}

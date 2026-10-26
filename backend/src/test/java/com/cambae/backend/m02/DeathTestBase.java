package com.cambae.backend.m02;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService;
import com.cambae.backend.m02.api.dto.PostDeathItemCode;
import com.cambae.backend.m02.api.dto.PostDeathItemStatus;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.m02.FakePostDeathItem;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Nền chung cho test qua đời của 004 US8 (T062): người cao tuổi nội trú dài hạn có hợp đồng Hiệu lực (từ {@link
 * EndingTestBase}), lệnh Ghi nhận qua đời của 001 đi qua bean tham gia thật của M02; năm mục của danh sách việc là năm bản
 * giả {@link FakePostDeathItem} (010, 013, 006, 007, 017 chưa cài). Lớp con khai {@code @AutoConfigureMockMvc} và {@code
 * @WithMockUser}.
 */
abstract class DeathTestBase extends EndingTestBase {

    @Autowired
    protected List<FakePostDeathItem> postDeathItems;

    @BeforeEach
    void deathSetup() {
        postDeathItems.forEach(FakePostDeathItem::reset);
        otherModules.interestedIn(
                StatusCommand.COMPLETE_ADMISSION,
                StatusCommand.CANCEL_ADMISSION,
                StatusCommand.END_RESIDENCY,
                StatusCommand.RECORD_DEATH);
    }

    @Override
    protected void cleanDailyState() throws SQLException {
        WaitingListTestBase.admin(
                "DELETE FROM daily_run WHERE task IN ('CONTRACTS', 'REMINDERS', 'ABSENCE_DAYS', 'ENDINGS')");
    }

    // ---- Dữ liệu ----------------------------------------------------------------------------------------------------

    protected FakePostDeathItem item(PostDeathItemCode code) {
        return postDeathItems.stream().filter(i -> i.code() == code).findFirst().orElseThrow();
    }

    /** Đặt trạng thái cho cả năm mục của người đó. */
    protected void allItems(long residentId, PostDeathItemStatus status) {
        postDeathItems.forEach(i -> i.status(residentId, status));
    }

    protected ObjectNode deathDetails(Instant deathAt, String cause, Long evidenceFileId) {
        ObjectNode details = JSON.createObjectNode();
        details.put("deathAt", deathAt.toString());
        details.put("place", "Phòng P201");
        details.put("discoveredBy", "Nhân viên chăm sóc trực");
        details.put("confirmedById", doctor.staffId());
        if (cause != null) {
            details.put("cause", cause);
        }
        if (evidenceFileId != null) {
            details.put("evidenceFileId", evidenceFileId);
        }
        return details;
    }

    /** Tệp bằng chứng qua đời vừa tải lên, chưa gắn. */
    protected long evidenceFile() {
        return files.upload(
                "giay-bao-tu.pdf",
                FileTypes.PDF,
                "%PDF-1.4 giấy báo tử".getBytes(StandardCharsets.UTF_8),
                office.accountId());
    }

    protected ResidentStatusService.Outcome recordDeath(TestUser who, long residentId, ObjectNode details) {
        user.set(who.user());
        return statusService.recordDeath(
                ResidentStatusService.target(residentId), new Reason("Ghi nhận qua đời"), details);
    }

    protected BusinessException recordDeathFailure(TestUser who, long residentId, ObjectNode details) {
        return failure(() -> recordDeath(who, residentId, details));
    }

    /** Bác sĩ ghi nhận qua đời tại viện lúc đồng hồ đang chỉ; trả thời điểm qua đời. */
    protected Instant doctorRecordsDeath(long residentId, String cause) {
        Instant deathAt = time.now();
        recordDeath(doctor, residentId, deathDetails(deathAt, cause, null));
        return deathAt;
    }

    protected ResultActions getAs(TestUser who, String url) throws Exception {
        user.set(who.user());
        return mvc.perform(get(url));
    }

    protected JsonNode json(ResultActions result) throws Exception {
        return JSON.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected JsonNode postDeathTasks(TestUser who, long residentId) throws Exception {
        return json(getAs(who, "/api/v1/residents/" + residentId + "/post-death-tasks")
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk()));
    }

    protected static JsonNode task(JsonNode list, String code) {
        for (JsonNode item : list.get("items")) {
            if (code.equals(item.get("code").asString())) {
                return item;
            }
        }
        throw new AssertionError("Không có mục " + code + " trong " + list);
    }

    /** Hồ sơ lưu trú Đã đóng (FR-072). */
    protected boolean residencyClosed(long residentId) {
        return jdbc.queryForObject(
                        "SELECT closed_at IS NOT NULL FROM resident_residency WHERE resident_id = ?",
                        Boolean.class,
                        residentId)
                .booleanValue();
    }
}

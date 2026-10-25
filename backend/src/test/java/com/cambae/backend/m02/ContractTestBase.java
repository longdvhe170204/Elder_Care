package com.cambae.backend.m02;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.file.FakeFileConfig;
import com.cambae.backend.support.fake.m02.FakeRepresentatives;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Nền chung cho test hợp đồng của 004 US3 (T029 → T031): hợp đồng lập qua HTTP bằng Hành chính, người đại diện lấy từ bản
 * giả 012 ({@link FakeRepresentatives}), bản scan lưu ở bản giả S3. Mỗi test dựng danh mục dịch vụ riêng vì mỗi mức chỉ có
 * một dịch vụ chăm sóc Hiệu lực (Q-297) và các test dùng chung schema. Lớp con khai {@code @AutoConfigureMockMvc} và
 * {@code @WithMockUser}.
 */
@Import(FakeFileConfig.class)
abstract class ContractTestBase extends M02TestBase {

    protected static final JsonMapper JSON = JsonMapper.builder().build();
    protected static final String SIGN_PDF = "%PDF-1.4 hợp đồng đã ký";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected TestClockControl clock;

    @Autowired
    protected FakeRepresentatives representatives;

    @Autowired
    protected FileApi files;

    protected TestUser manager;
    protected long basicCare;
    protected long regularCare;
    protected long meal;

    @BeforeEach
    void contractSetup() throws SQLException {
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        representatives.reset();
        clock.reset();
        cleanDailyState();
        jdbc.update("UPDATE service SET status = 'INACTIVE' WHERE type = 'CARE'");
        basicCare = service("CARE", "Chăm sóc cơ bản", "BASIC", 5_000_000);
        regularCare = service("CARE", "Chăm sóc thường xuyên", "REGULAR", 8_000_000);
        meal = service("MEAL", "Suất ăn thêm", null, 600_000);
    }

    @AfterEach
    void contractCleanup() throws SQLException {
        clock.reset();
        cleanDailyState();
    }

    /** Việc CONTRACTS, REMINDERS dùng chung bảng {@code daily_run}: dọn trước và sau mỗi test (tài khoản ứng dụng không xóa được). */
    protected void cleanDailyState() throws SQLException {
        WaitingListTestBase.admin("DELETE FROM daily_run WHERE task IN ('CONTRACTS', 'REMINDERS')");
    }

    /** Dịch vụ cùng một phiên bản đơn giá mở từ 2020-01-01; trả mã dịch vụ. */
    protected long service(String type, String name, String careLevel, int price) {
        return service(type, name, careLevel, price, LocalDate.of(2020, 1, 1));
    }

    /** Dịch vụ có phiên bản đơn giá mở từ {@code effectiveFrom} (đơn giá không sửa, xóa được sau khi tạo). */
    protected long service(String type, String name, String careLevel, int price, LocalDate effectiveFrom) {
        String unique = name + " " + System.nanoTime();
        jdbc.update(
                "INSERT INTO service (type, name, unit, billing_period, care_level_code) VALUES (?, ?, 'tháng', 'MONTH', ?)",
                type,
                unique,
                careLevel);
        long id = jdbc.queryForObject("SELECT id FROM service WHERE name = ?", Long.class, unique);
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from) VALUES (?, ?, ?)",
                id,
                price,
                effectiveFrom);
        return id;
    }

    protected static ObjectNode line(long serviceId) {
        ObjectNode node = JSON.createObjectNode();
        node.put("serviceId", serviceId);
        node.put("inPackage", true);
        node.put("quantity", 1);
        return node;
    }

    /** Thân hợp đồng nội trú dài hạn hợp lệ, mức Cơ bản, bắt đầu hôm nay, không yêu cầu đặt cọc. */
    protected ObjectNode body(long residentId) {
        ObjectNode body = JSON.createObjectNode();
        body.put("residentId", residentId);
        body.put("residenceType", "LONG_TERM_RESIDENTIAL");
        body.put("startDate", today().toString());
        body.put("careLevel", "BASIC");
        body.put("roomTypeId", 101);
        body.put("residencyFeeBillingPeriod", "MONTH");
        ArrayNode services = body.putArray("services");
        services.add(line(basicCare));
        body.put("noDepositRequired", true);
        body.put("bedHoldPolicy", "Giữ giường theo bảng cơ sở");
        body.put("terminationCondition", "Báo trước 30 ngày");
        return body;
    }

    /** Người cao tuổi Đang tiếp nhận đã có người đại diện; trả mã người cao tuổi. */
    protected long residentWithRepresentative() {
        long residentId = admissionResident();
        representatives.representative(residentId, representativeOf(residentId));
        return residentId;
    }

    /** Mã người thân dùng làm người đại diện của người cao tuổi (cố định theo người cao tuổi). */
    protected static long representativeOf(long residentId) {
        return 9_000_000L + residentId;
    }

    protected ResultActions createRaw(ObjectNode body) throws Exception {
        user.set(office.user());
        return mvc.perform(post("/api/v1/contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()));
    }

    /** Lập hợp đồng Nháp (201); trả {@code ContractResult}. */
    protected JsonNode create(ObjectNode body) throws Exception {
        MvcResult result = createRaw(body).andExpect(status().isCreated()).andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    protected long createDraft(long residentId) throws Exception {
        return create(body(residentId)).get("contract").get("id").asLong();
    }

    protected JsonNode contract(long id) throws Exception {
        return JSON.readTree(mvc.perform(get("/api/v1/contracts/" + id))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    protected String etag(long id) throws Exception {
        return mvc.perform(get("/api/v1/contracts/" + id))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
    }

    protected ResultActions updateRaw(long id, String ifMatch, ObjectNode body) throws Exception {
        user.set(office.user());
        return mvc.perform(put("/api/v1/contracts/" + id)
                .header("If-Match", ifMatch)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()));
    }

    protected ResultActions command(long id, String name) throws Exception {
        user.set(office.user());
        return mvc.perform(post("/api/v1/contracts/" + id + "/" + name)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
    }

    protected ResultActions commandWithReason(long id, String name, String reason) throws Exception {
        user.set(office.user());
        return mvc.perform(post("/api/v1/contracts/" + id + "/" + name)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"" + reason + "\"}"));
    }

    protected void send(long id) throws Exception {
        command(id, "send-for-signature").andExpect(status().isOk());
    }

    /** Tệp bản scan hợp đồng vừa tải lên, chưa gắn. */
    protected long scan() {
        return files.upload(
                "hop-dong.pdf", FileTypes.PDF, SIGN_PDF.getBytes(StandardCharsets.UTF_8), office.accountId());
    }

    protected ObjectNode signedBody(long residentId, LocalDate signedDate) {
        ObjectNode body = JSON.createObjectNode();
        body.put("signedDate", signedDate.toString());
        body.put("signingRepresentativeId", representativeOf(residentId));
        body.put("scanFileId", scan());
        return body;
    }

    protected ResultActions recordSignedRaw(long id, ObjectNode body) throws Exception {
        user.set(office.user());
        return mvc.perform(post("/api/v1/contracts/" + id + "/record-signed")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()));
    }

    /** Hợp đồng đi hết Nháp → Chờ ký → Hiệu lực; trả mã hợp đồng. */
    protected long activeContract(long residentId, ObjectNode body) throws Exception {
        long id = create(body).get("contract").get("id").asLong();
        send(id);
        recordSignedRaw(id, signedBody(residentId, today())).andExpect(status().isOk());
        return id;
    }

    /** Đặt đồng hồ giả lập vào 12:00 ngày {@code day} (sau 00:15 nên job hằng ngày được phép chạy). */
    protected void noonOf(LocalDate day) {
        clock.setTo(day.atTime(12, 0)
                .atZone(com.cambae.backend.common.time.BusinessTime.ZONE)
                .toInstant());
    }

    protected String contractStatus(long id) {
        return jdbc.queryForObject("SELECT status FROM contract WHERE id = ?", String.class, id);
    }

    /** Mã điều kiện chưa thỏa trong thân lỗi 409 {@code CONDITION_NOT_MET}. */
    protected static java.util.List<String> unmetCodes(MvcResult result) throws Exception {
        JsonNode body = JSON.readTree(result.getResponse().getContentAsString());
        java.util.List<String> codes = new java.util.ArrayList<>();
        body.get("unmetConditions").forEach(node -> codes.add(node.get("code").asString()));
        return codes;
    }
}

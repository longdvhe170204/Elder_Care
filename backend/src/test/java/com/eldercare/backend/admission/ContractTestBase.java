package com.eldercare.backend.admission;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.family.RelationshipFixtures;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Nền chung cho test hợp đồng của 004 US3 (T029 → T031): hợp đồng lập qua HTTP bằng Hành chính, người đại diện lấy từ bản
 * thật của 012 (quan hệ lập bằng {@link RelationshipFixtures}), bản scan lưu ở bản giả S3. Mỗi test dựng danh mục dịch vụ riêng vì mỗi mức chỉ có
 * một dịch vụ chăm sóc Hiệu lực (Q-297) và các test dùng chung schema. Lớp con khai {@code @AutoConfigureMockMvc} và
 * {@code @WithMockUser}.
 */
@org.springframework.context.annotation.Import({FakeFileConfig.class, ContractTestBase.CapacityFake.class})
abstract class ContractTestBase extends M02TestBase {

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class CapacityFake {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        com.eldercare.backend.support.fake.roombedallocation.FakeDayCareCapacity fakeDayCareCapacity() {
            return new com.eldercare.backend.support.fake.roombedallocation.FakeDayCareCapacity();
        }
    }

    protected static final JsonMapper JSON = JsonMapper.builder().build();
    protected static final String SIGN_PDF = "%PDF-1.4 hợp đồng đã ký";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected TestClockControl clock;

    @Autowired
    private RelationshipHistoryStore relationshipHistory;

    @Autowired
    private TransactionTemplate transactions;

    private RelationshipFixtures family;
    private final Map<Long, Long> representatives = new HashMap<>();

    @Autowired
    protected FileApi files;

    /**
     * Quản lý viện dùng chung giữa các test: mỗi tài khoản Quản lý viện tạo thêm đều vào danh sách người duyệt của mọi sự kiện
     * {@code RequestSubmitted} (cột {@code SERIALIZED_EVENT} dài 4000), nên tạo mới ở mỗi test làm sự kiện vượt cột khi bộ test
     * đủ lớn (phát hiện ở Phase 9). Tạo lại khi schema test đã dựng lại.
     */
    private static TestUser sharedManager;

    /** Sức chứa khu nghỉ là của 003; test của 004 không dựng khu nghỉ. */
    @Autowired
    protected com.eldercare.backend.support.fake.roombedallocation.FakeDayCareCapacity dayCareCapacity;

    protected TestUser manager;
    protected long basicCare;
    protected long regularCare;
    protected long meal;

    @BeforeEach
    void contractSetup() throws SQLException {
        dayCareCapacity.reset();
        manager = sharedManager();
        clock.reset();
        cleanDailyState();
        jdbc.update(M02Sql.DEACTIVATE_CARE_SERVICES);
        basicCare = service("CARE", "Chăm sóc cơ bản", "BASIC", 5_000_000);
        regularCare = service("CARE", "Chăm sóc thường xuyên", "REGULAR", 8_000_000);
        meal = service("MEAL", "Suất ăn thêm", null, 600_000);
    }

    private TestUser sharedManager() {
        TestUser cached = sharedManager;
        if (cached != null
                && jdbc.queryForObject(
                                "SELECT COUNT(*) FROM account WHERE id = ? AND staff_id = ?",
                                Integer.class,
                                cached.accountId(),
                                cached.staffId())
                        == 1) {
            return cached;
        }
        sharedManager = users.staff(Role.NURSING_HOME_MANAGER);
        return sharedManager;
    }

    @AfterEach
    void contractCleanup() throws SQLException {
        clock.reset();
        cleanDailyState();
    }

    /** Việc hằng ngày dùng chung dấu {@code M02.DAILY_RUN}: dọn trước và sau mỗi test (tài khoản ứng dụng không xóa được). */
    protected void cleanDailyState() throws SQLException {
        WaitingListTestBase.admin("DELETE FROM reminder_marker WHERE kind IN ('M02.DAILY_RUN', 'M02.REMINDER')");
    }

    /** Dịch vụ cùng một phiên bản đơn giá mở từ 2020-01-01; trả mã dịch vụ. */
    protected long service(String type, String name, String careLevel, int price) {
        return service(type, name, careLevel, price, LocalDate.of(2020, 1, 1));
    }

    /** Dịch vụ có phiên bản đơn giá mở từ {@code effectiveFrom} (đơn giá không sửa, xóa được sau khi tạo). */
    protected long service(String type, String name, String careLevel, int price, LocalDate effectiveFrom) {
        String unique = name + " " + System.nanoTime();
        jdbc.update(M02Sql.INSERT_SERVICE, unique, M02Sql.attributes(type, "tháng", "MONTH", careLevel));
        long id = jdbc.queryForObject(M02Sql.SERVICE_ID, Long.class, unique);
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
        long member = family().familyMember("Người đại diện " + residentId, "0903" + residentId % 1_000_000);
        family().relate(member, residentId, 1, true, true);
        representatives.put(residentId, member);
        return residentId;
    }

    /** Dữ liệu người thân, quan hệ thật của 012; quan hệ bắt đầu từ lâu trước mọi ngày test dùng. */
    protected RelationshipFixtures family() {
        if (family == null) {
            family = new RelationshipFixtures(
                    jdbc,
                    transactions,
                    relationshipHistory,
                    time.now().minus(Duration.ofDays(1500)).truncatedTo(ChronoUnit.MICROS));
        }
        return family;
    }

    /** Mã người thân là người đại diện của người cao tuổi, lập bởi {@link #residentWithRepresentative()}. */
    protected long representativeOf(long residentId) {
        Long member = representatives.get(residentId);
        if (member == null) {
            throw new IllegalStateException("Người cao tuổi " + residentId + " chưa lập người đại diện trong test");
        }
        return member;
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

    /** Hợp đồng đi hết Nháp → Chờ ký → Hiệu lực; trả mã hợp đồng. Nội trú: có giường trước khi ký (FR-021). */
    protected long activeContract(long residentId, ObjectNode body) throws Exception {
        long id = create(body).get("contract").get("id").asLong();
        send(id);
        if (!"DAY_CARE".equals(body.path("residenceType").asString(""))) {
            String start = body.path("startDate").asString("");
            bedForSigning(residentId, start.isEmpty() ? today() : LocalDate.parse(start));
        }
        recordSignedRaw(id, signedBody(residentId, today())).andExpect(status().isOk());
        return id;
    }

    /**
     * FR-021: phân bổ Hiệu lực thật của 003 (bắt đầu 30 ngày trước, hoặc từ ngày bắt đầu hợp đồng nếu sớm hơn) ở một giường riêng, để "Ghi nhận đã ký" hợp đồng nội trú
     * được phép; đã có phân bổ Hiệu lực hoặc Tương lai thì giữ nguyên. Test cần phân bổ ở giường khác thì gọi
     * {@link #cancelFixtureAllocation} trước khi dựng.
     */
    protected void bedForSigning(long residentId) {
        bedForSigning(residentId, today());
    }

    protected void bedForSigning(long residentId, LocalDate contractStart) {
        Integer open = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=? AND status IN ('ACTIVE','FUTURE')",
                Integer.class,
                residentId);
        if (open != null && open > 0) return;
        long area = com.eldercare.backend.support.TestUsers.nextId();
        long floor = com.eldercare.backend.support.TestUsers.nextId();
        long type = com.eldercare.backend.support.TestUsers.nextId();
        long room = com.eldercare.backend.support.TestUsers.nextId();
        long bed = com.eldercare.backend.support.TestUsers.nextId();
        jdbc.update("INSERT INTO area(id,code,name) VALUES(?,?,?)", area, "A" + area, "Khu " + area);
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)", floor, area, "F" + floor, "Tầng " + floor);
        jdbc.update("INSERT INTO room_type(id,name,standard_beds) VALUES(?,?,2)", type, "Loại " + type);
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(?,?,?,?,?,'UNRESTRICTED')",
                room,
                floor,
                type,
                "R" + room,
                "Phòng " + room);
        jdbc.update("INSERT INTO bed(id,room_id,code,state) VALUES(?,?,'G1','OCCUPIED')", bed, room);
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source)"
                        + " VALUES(?,?,'ACTIVE',?,'BASIC','CURRENT')",
                residentId,
                bed,
                java.sql.Timestamp.from(
                        java.util.Collections.min(java.util.List.of(today().minusDays(30), contractStart))
                                .atStartOfDay(com.eldercare.backend.common.time.BusinessTime.ZONE)
                                .toInstant()));
    }

    /** Hủy phân bổ do {@link #bedForSigning} dựng, để test tự dựng phân bổ ở giường của mình. */
    protected void cancelFixtureAllocation(long residentId) {
        jdbc.update(
                "UPDATE bed_allocation SET status='CANCELLED',cancellation_reason='Kiểm thử: bỏ phân bổ dựng sẵn'"
                        + " WHERE resident_id=? AND status IN ('ACTIVE','FUTURE')",
                residentId);
    }

    /** Đặt đồng hồ giả lập vào 12:00 ngày {@code day} (sau 00:15 nên job hằng ngày được phép chạy). */
    protected void noonOf(LocalDate day) {
        clock.setTo(day.atTime(12, 0)
                .atZone(com.eldercare.backend.common.time.BusinessTime.ZONE)
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

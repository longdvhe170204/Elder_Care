package com.eldercare.backend.family;

import static com.eldercare.backend.support.TestUsers.bearer;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.resident.FakeConsentApi;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lệnh US1 của 012 qua REST với phiên Hành chính thật (lớp quyền của 002). Cùng cấu hình context với
 * {@code T003_FamilyRightsApiIT} ({@link FakeConsentApi.Config}); MockMvc dựng từ context để không thêm context Spring.
 */
@Import(FakeConsentApi.Config.class)
@RecordApplicationEvents
abstract class FamilyCommandTestBase extends MySqlIntegrationTestBase {

    static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    RelationshipHistoryStore history;

    @Autowired
    FakeConsentApi consents;

    @Autowired
    List<StatusTransitionParticipant> participants;

    @Autowired
    ApplicationEvents events;

    MockMvc mvc;
    TestUsers users;
    String office;
    RelationshipFixtures fixtures;

    @BeforeEach
    void setUpFamilyCommands() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        users = new TestUsers(jdbc, time);
        office = bearer(users.session(users.staff(Role.OFFICE_STAFF).accountId()));
        // DATETIME(6) làm tròn phần dưới micro giây
        Instant start = time.now().minus(Duration.ofDays(10)).truncatedTo(ChronoUnit.MICROS);
        fixtures = new RelationshipFixtures(jdbc, tx, history, start);
    }

    @AfterEach
    void resetConsents() {
        consents.reset();
    }

    /** Đọc id theo mã hồ sơ: {@code LAST_INSERT_ID()} ngoài transaction có thể lấy kết nối khác của pool. */
    long resident(ResidentStatus status) {
        String code = UUID.randomUUID().toString().substring(0, 20);
        jdbc.update(
                "INSERT INTO resident (profile_code, full_name, date_of_birth, gender, status, status_since,"
                        + " created_at, created_by) VALUES (?, 'Người cao tuổi thử', '1940-05-01', 'FEMALE', ?,"
                        + " NOW(6), NOW(6), 1)",
                code,
                status.name());
        return jdbc.queryForObject("SELECT id FROM resident WHERE profile_code = ?", Long.class, code);
    }

    long member(String name) throws Exception {
        return member(name, null);
    }

    long member(String name, String idDocumentNumber) throws Exception {
        String document = idDocumentNumber == null
                ? ""
                : ", \"idDocumentType\": \"CCCD\", \"idDocumentNumber\": \"" + idDocumentNumber + "\"";
        return id(post(
                        "/api/v1/family-members",
                        "{\"fullName\": \"" + name + "\", \"phoneNumber\": \"0901000" + (TestUsers.nextId() % 1000)
                                + "\"" + document + "}")
                .andExpect(status().isCreated()));
    }

    /** Lập quan hệ; người đại diện đầu tiên kèm bằng chứng. */
    long relate(long residentId, long familyMemberId, boolean primaryContact, boolean firstRepresentative)
            throws Exception {
        return id(createRelationship(residentId, familyMemberId, primaryContact, firstRepresentative)
                .andExpect(status().isCreated()));
    }

    ResultActions createRelationship(
            long residentId, long familyMemberId, boolean primaryContact, boolean firstRepresentative)
            throws Exception {
        String representative = firstRepresentative
                ? ", \"setFirstRepresentative\": true, \"evidenceFileId\": " + file()
                        + ", \"reason\": \"Người ký hợp đồng\""
                : "";
        return post(
                "/api/v1/residents/" + residentId + "/relationships",
                "{\"familyMemberId\": " + familyMemberId + ", \"relationship\": \"CHILD\", \"primaryContact\": "
                        + primaryContact + representative + "}");
    }

    ResultActions command(long relationshipId, String command, String body) throws Exception {
        return post("/api/v1/relationships/" + relationshipId + "/" + command, body);
    }

    ResultActions post(String path, String body) throws Exception {
        return postAs(office, path, body);
    }

    ResultActions postAs(String authorization, String path, String body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(path)
                .header("Authorization", authorization)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    JsonNode getJson(String path) throws Exception {
        return getJsonAs(office, path);
    }

    JsonNode getJsonAs(String authorization, String path) throws Exception {
        String body = mvc.perform(get(path).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JSON.readTree(body);
    }

    /** Phiên của tài khoản người thân gắn hồ sơ {@code familyMemberId} (cổng). */
    String familySession(long familyMemberId) {
        return bearer(users.session(users.family(familyMemberId).accountId()));
    }

    String staffSession(Role role) {
        return bearer(users.session(users.staff(role).accountId()));
    }

    static long id(ResultActions result) throws Exception {
        return json(result).get("id").asLong();
    }

    static JsonNode json(ResultActions result) throws Exception {
        return JSON.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** Tệp đã tải lên, chưa gắn chủ sở hữu (bằng chứng, bản scan). */
    long file() {
        String uuid = UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO file (uuid, original_name, mime_type, size_bytes, sha256, uploaded_by, uploaded_at)"
                        + " VALUES (?, 'scan.pdf', 'application/pdf', 10, REPEAT('0', 64), 1, ?)",
                uuid,
                LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC));
        return jdbc.queryForObject("SELECT id FROM file WHERE uuid = ?", Long.class, uuid);
    }

    boolean column(long relationshipId, String column) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT " + column + " FROM family_relationship WHERE id = ?", Boolean.class, relationshipId));
    }

    String relationshipStatus(long relationshipId) {
        return jdbc.queryForObject("SELECT status FROM family_relationship WHERE id = ?", String.class, relationshipId);
    }

    /** Bean {@code ThamGiaChuyenTrangThai} của 012 (T012). */
    StatusTransitionParticipant familyParticipant() {
        return participants.stream()
                .filter(p -> p.getClass().getPackageName().startsWith("com.eldercare.backend.family"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Chưa có bean StatusTransitionParticipant của 012"));
    }

    static String reason(String value) {
        return "{\"reason\": \"" + value + "\"}";
    }

    /** Lập yêu cầu thay đổi quyền (US3); {@code fields} là các trường JSON ngoài người cao tuổi và lý do. */
    ResultActions createRequest(String authorization, long residentId, String fields) throws Exception {
        return postAs(
                authorization,
                "/api/v1/right-change-requests",
                "{\"residentId\": " + residentId + ", " + fields + ", \"reason\": \"Gia đình đề nghị\"}");
    }

    /** Yêu cầu đổi một quyền của quan hệ. */
    ResultActions requestRight(String authorization, long residentId, long relationshipId, String right, boolean value)
            throws Exception {
        return createRequest(
                authorization,
                residentId,
                "\"type\": \"RIGHT\", \"relationshipId\": " + relationshipId + ", \"right\": \"" + right
                        + "\", \"newValue\": " + value);
    }

    ResultActions requestCommand(String authorization, long requestId, String command, String body) throws Exception {
        return postAs(authorization, "/api/v1/right-change-requests/" + requestId + "/" + command, body);
    }

    String requestStatus(long requestId) {
        return jdbc.queryForObject("SELECT status FROM simple_record WHERE id = ?", String.class, requestId);
    }
}

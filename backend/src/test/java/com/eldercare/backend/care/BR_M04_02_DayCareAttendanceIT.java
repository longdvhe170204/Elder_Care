package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.dto.Attendance;
import com.eldercare.backend.admission.api.dto.DayCareSessionView;
import com.eldercare.backend.admission.api.events.AddendumApplied;
import com.eldercare.backend.care.api.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.internal.attendance.service.AttendanceClockWork;
import com.eldercare.backend.care.internal.scheduling.service.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.family.RelationshipFixtures;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
/**
 * BR-M04-02, FR-012 → FR-014 (T027, T029, T030): hợp đồng bán trú Hiệu lực thật của 004 lập qua HTTP (dịch vụ, giá, lập,
 * gửi ký, ghi đã ký), lịch đến qua {@code ContractReadApi.attendanceOn}, loại lưu trú qua {@code ContractApi} của 004,
 * {@code AddendumApplied} phát trên hợp đồng đó; ngày nghỉ, báo vắng qua lệnh của 004; người đại
 * diện qua {@code RelationshipFixtures} công khai của 012; bản ghi đón qua {@code PickupPorts} của 012. Còn bản giả: trạng
 * thái hồ sơ 001 ({@code FakeResidents}: Đang lưu trú cần Hoàn tất tiếp nhận nhiều nguồn), kho S3 ({@code
 * FakeFileConfig}). Khu vực, tầng, khu nghỉ lập qua HTTP của 003, CFG-M03-01 qua lệnh tham số của 000. Mọi dữ liệu nằm
 * trong transaction của test.
 */
@Import({SwitchableUserConfig.class, FakeFileConfig.class, BR_M04_02_DayCareAttendanceIT.Sources.class})
class BR_M04_02_DayCareAttendanceIT extends MySqlIntegrationTestBase {
    @TestConfiguration
    static class Sources {
        @Bean
        @Primary
        FakeResidents attendanceResidents() {
            return new FakeResidents();
        }
    }

    private static final String SIGN_PDF = "%PDF-1.4 hợp đồng bán trú đã ký";

    @Autowired
    org.springframework.context.ApplicationContext context;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakeResidents residents;

    @Autowired
    TestClockControl clock;

    @Autowired
    JsonMapper json;

    @Autowired
    ApplicationEvents events;

    @Autowired
    FileApi files;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    RelationshipHistoryStore relationshipHistory;

    @Autowired
    ApplicationEventPublisher publisher;

    /** CFG-M03-01 (000) đọc trong transaction mới nên không thấy lệnh đổi chưa commit; đặt như FR_038_DayCareHttpIT (003). */
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    ParameterApi parameters;

    long resident, floor, contract, representative;
    Instant at;
    LocalDate date;
    TestUsers.TestUser office, manager;

    @BeforeEach
    void setup() throws Exception {
        for (long old : residents.byStatus(Set.of(ResidentStatus.IN_RESIDENCE)))
            residents.resident(old, "[Hồ sơ test cũ]", Gender.MALE, ResidentStatus.RESIDENCY_ENDED);
        resident = TestUsers.nextId();
        date = time.today();
        // Giữa ngày: sau giờ đến 08:00 quá CFG-M02-07, trước giờ về 17:00; không phụ thuộc giờ chạy test
        clock.setTo(date.atTime(12, 0).atZone(BusinessTime.ZONE).toInstant());
        at = time.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var users = new TestUsers(jdbc, time);
        office = users.staff(Role.OFFICE_STAFF);
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        residents.resident(resident, "[Người cao tuổi kiểm thử]", Gender.MALE, ResidentStatus.IN_RESIDENCE);
        var family = new RelationshipFixtures(
                jdbc,
                tx,
                relationshipHistory,
                date.minusDays(30).atStartOfDay(BusinessTime.ZONE).toInstant());
        representative = family.familyMember("[Người đại diện kiểm thử]", "0900" + (resident % 1_000_000));
        family.relate(representative, resident, 1, true, true);
        contract = dayCareContract(schedule(
                line(date.getDayOfWeek(), "08:00", "17:00"),
                line(date.plusDays(1).getDayOfWeek(), "08:00", "17:00")));
        user.set(office.user());
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    // ---- Hợp đồng bán trú thật của 004 qua HTTP
    // -----------------------------------------------------------------------

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static String line(DayOfWeek day, String from, String to) {
        return "{\"dayOfWeek\":%d,\"arrivalTime\":\"%s\",\"departureTime\":\"%s\"}".formatted(day.getValue(), from, to);
    }

    private static String schedule(String... lines) {
        return "[" + String.join(",", lines) + "]";
    }

    /** Dịch vụ chăm sóc mức Cơ bản đang Hiệu lực có giá hôm nay (Q-297: mỗi mức một dịch vụ), lập nếu chưa có. */
    private long basicCareService() throws Exception {
        user.set(manager.user());
        for (JsonNode s :
                body(mvc.perform(get("/api/v1/services").param("type", "CARE")).andExpect(status().isOk())))
            if ("BASIC".equals(s.path("careLevelCode").asString(""))
                    && "ACTIVE".equals(s.path("status").asString(""))) {
                long id = s.get("id").asLong();
                boolean priced = false;
                for (JsonNode v : body(mvc.perform(get("/api/v1/services/" + id + "/price-versions"))
                        .andExpect(status().isOk())))
                    priced |= !LocalDate.parse(v.get("effectiveFrom").asString())
                                    .isAfter(date)
                            && (v.path("effectiveTo").isNull()
                                    || v.path("effectiveTo").isMissingNode()
                                    || !LocalDate.parse(v.get("effectiveTo").asString())
                                            .isBefore(date));
                if (!priced) price(id);
                return id;
            }
        long id = body(mvc.perform(
                                post("/api/v1/services")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"type\":\"CARE\",\"name\":\"Chăm sóc cơ bản " + resident
                                                        + "\",\"unit\":\"tháng\",\"billingPeriod\":\"MONTH\",\"careLevelCode\":\"BASIC\"}"))
                        .andExpect(status().isCreated()))
                .get("id")
                .asLong();
        price(id);
        return id;
    }

    private void price(long serviceId) throws Exception {
        mvc.perform(post("/api/v1/services/" + serviceId + "/price-versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\":5000000,\"effectiveFrom\":\"" + date.minusDays(1) + "\"}"))
                .andExpect(status().isCreated());
    }

    /** Sức chứa khu nghỉ (CFG-M03-01) và khu vực, tầng, khu nghỉ Hiệu lực của 003 để 004 kiểm sức chứa khi ghi đã ký. */
    private void dayCareConfigured() throws Exception {
        user.set(manager.user());
        long area = body(mvc.perform(post("/api/v1/areas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"A" + (resident % 1_000_000) + "\",\"name\":\"Khu kiểm thử\"}"))
                        .andExpect(status().isCreated()))
                .get("id")
                .asLong();
        floor = body(mvc.perform(post("/api/v1/floors")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"areaId\":" + area + ",\"code\":\"F" + (resident % 1_000_000)
                                        + "\",\"name\":\"Tầng kiểm thử\"}"))
                        .andExpect(status().isCreated()))
                .get("id")
                .asLong();
        org.mockito.Mockito.doReturn(new com.eldercare.backend.administration.parameter.api.dto.ParameterValue(
                        ParameterCode.CFG_M03_01,
                        com.eldercare.backend.administration.parameter.api.dto.ParameterKind.INTEGER,
                        json.readTree("30")))
                .when(parameters)
                .read(ParameterCode.CFG_M03_01);
        int created = mvc.perform(post("/api/v1/day-care-rest-areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"K" + (floor % 100_000) + "\",\"name\":\"Khu nghỉ kiểm thử\",\"floorId\":"
                                + floor + "}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(created).as("lập khu nghỉ hoặc đã có khu nghỉ Hiệu lực").isIn(201, 409);
    }

    /** Hợp đồng bán trú Nháp → Chờ ký → Hiệu lực, bắt đầu hôm nay; trả mã hợp đồng. */
    private long dayCareContract(String arrivalSchedule) throws Exception {
        dayCareConfigured();
        long care = basicCareService();
        user.set(office.user());
        long id = body(mvc.perform(post("/api/v1/contracts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"residentId\":" + resident
                                        + ",\"residenceType\":\"DAY_CARE\",\"startDate\":\"" + date
                                        + "\",\"careLevel\":\"BASIC\",\"residencyFeeBillingPeriod\":\"SESSION\","
                                        + "\"services\":[{\"serviceId\":" + care
                                        + ",\"inPackage\":true,\"quantity\":1}],\"noDepositRequired\":true,"
                                        + "\"bedHoldPolicy\":\"Theo bảng cơ sở\",\"terminationCondition\":\"Báo trước 30"
                                        + " ngày\",\"arrivalSchedule\":" + arrivalSchedule + "}"))
                        .andExpect(status().isCreated()))
                .get("contract")
                .get("id")
                .asLong();
        mvc.perform(post("/api/v1/contracts/" + id + "/send-for-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        long scan = files.upload(
                "hop-dong.pdf", FileTypes.PDF, SIGN_PDF.getBytes(StandardCharsets.UTF_8), office.accountId());
        mvc.perform(post("/api/v1/contracts/" + id + "/record-signed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"signedDate\":\"" + date + "\",\"signingRepresentativeId\":" + representative
                                + ",\"scanFileId\":" + scan + "}"))
                .andExpect(status().isOk());
        return id;
    }

    private Optional<AttendanceView> onDate(LocalDate day) {
        return api().onDate(resident, day);
    }

    private int attendanceVersion(LocalDate day) {
        return jdbc.queryForObject(
                "SELECT version FROM simple_record WHERE unique_key=?",
                Integer.class,
                "M04.DAY_CARE_ATTENDANCE:" + resident + ":" + day);
    }

    @Test
    void FR_012_arrivalScheduleAddendum_determinesDaysFromEffectiveDateToToday_earlierDayUnchanged() {
        // Sự kiện 004 phát khi áp dụng phụ lục (luồng job áp dụng chạy REQUIRES_NEW sau commit, không kiểm được trong
        // transaction của test: chủ dự án chọn 2026-11-09). Lịch hiện hành của hợp đồng thật có hôm nay và ngày mai.
        daily("DAY_CARE_DAY_START").run(date, time.now());
        int before = attendanceVersion(date);
        LocalDate tomorrow = date.plusDays(1);
        clock.setTo(tomorrow.atTime(12, 0).atZone(BusinessTime.ZONE).toInstant());
        publisher.publishEvent(
                new AddendumApplied(TestUsers.nextId(), contract, resident, Set.of("ARRIVAL_SCHEDULE"), tomorrow));
        var added = onDate(tomorrow).orElseThrow();
        assertThat(added.status()).isEqualTo(AttendanceStatus.NOT_ARRIVED);
        assertThat(added.expectedArrival()).contains(LocalTime.of(8, 0));
        assertThat(attendanceVersion(date))
                .as("ngày trước ngày hiệu lực không đổi")
                .isEqualTo(before);
    }

    /**
     * T029 (2026-11-11): luồng thật của 004 — yêu cầu thay đổi {@code ARRIVAL_SCHEDULE} lập, gửi, duyệt qua HTTP, job áp
     * dụng của 000 ({@code ApplyDueRequestsJob}, REQUIRES_NEW) chạy {@code ResidencyChangeHandler.apply} và phát {@code
     * AddendumApplied} thật. Job đọc dữ liệu đã commit nên ca này không chạy trong transaction của test: hợp đồng, yêu cầu
     * và trạng thái có mặt được commit, id mới theo {@link TestUsers#nextId()}.
     */
    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void FR_012_arrivalScheduleAddendumAppliedByTheRealJob_determinesEffectiveDayByNewSchedule() throws Exception {
        LocalDate tomorrow = date.plusDays(1);
        user.set(office.user());
        long request = body(mvc.perform(post("/api/v1/residency-change-requests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"contractId\":" + contract
                                        + ",\"changeTypes\":[\"ARRIVAL_SCHEDULE\"],\"newValue\":{\"arrivalSchedule\":"
                                        + schedule(
                                                line(date.getDayOfWeek(), "08:00", "17:00"),
                                                line(tomorrow.getDayOfWeek(), "09:30", "16:00"))
                                        + "},\"requestedEffectiveDate\":\"" + tomorrow
                                        + "\",\"reason\":\"Gia đình đề nghị đổi giờ đến\"}"))
                        .andExpect(status().isCreated()))
                .get("approvalRequestId")
                .asLong();
        approval(request, "submit", "{}");
        user.set(manager.user());
        approval(request, "approve", "{\"comment\":\"Đồng ý\"}");
        assertThat(onDate(tomorrow)).isEmpty();

        clock.setTo(tomorrow.atTime(12, 0).atZone(BusinessTime.ZONE).toInstant());
        context.getBean(com.eldercare.backend.administration.approval.scheduling.ApplyDueRequestsJob.class)
                .run();

        assertThat(jdbc.queryForObject("SELECT status FROM approval_request WHERE id=?", String.class, request))
                .isEqualTo("APPLIED");
        var applied =
                onDate(tomorrow).orElseThrow(() -> new AssertionError("listener 005 chưa xác định ngày hiệu lực"));
        assertThat(applied.status()).isEqualTo(AttendanceStatus.NOT_ARRIVED);
        assertThat(applied.expectedArrival()).as("giờ đến theo lịch mới").contains(LocalTime.of(9, 30));
    }

    private void approval(long request, String command, String content) throws Exception {
        String etag = mvc.perform(get("/api/v1/approval-requests/" + request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        mvc.perform(
                        post("/api/v1/approval-requests/" + request + "/" + command)
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.csrf())
                                .header("If-Match", etag)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content))
                .andExpect(status().isOk());
    }

    @Test
    void FR_012_onlyArrivalScheduleChangesAct_andARepeatedEventChangesNothing() {
        // Mô phỏng sự kiện 004 phát trong lệnh áp dụng phụ lục: lịch hiện hành của hợp đồng đã có hôm nay
        publisher.publishEvent(new AddendumApplied(TestUsers.nextId(), contract, resident, Set.of("SERVICE"), date));
        assertThat(onDate(date)).isEmpty();
        long addendum = TestUsers.nextId();
        publisher.publishEvent(new AddendumApplied(addendum, contract, resident, Set.of("ARRIVAL_SCHEDULE"), date));
        assertThat(onDate(date).orElseThrow().status()).isEqualTo(AttendanceStatus.NOT_ARRIVED);
        int once = attendanceVersion(date);
        publisher.publishEvent(new AddendumApplied(addendum, contract, resident, Set.of("ARRIVAL_SCHEDULE"), date));
        assertThat(attendanceVersion(date)).isEqualTo(once);
    }

    @Test
    void FR_012_addendumEffectiveLater_leavesTodayToTheDayStartWork() {
        publisher.publishEvent(new AddendumApplied(
                TestUsers.nextId(), contract, resident, Set.of("ARRIVAL_SCHEDULE"), date.plusDays(3)));
        assertThat(onDate(date)).isEmpty();
        assertThat(onDate(date.plusDays(3))).isEmpty();
    }

    // ---- Điểm danh, báo vắng, ngày nghỉ, việc theo lịch -----------------------------------------------------------

    private org.springframework.test.web.servlet.ResultActions arrive() throws Exception {
        return mvc.perform(post("/api/v1/day-care-attendances/check-in")
                .header("Idempotency-Key", "arrival-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":" + resident + ",\"at\":\"" + at + "\"}"));
    }

    private long arriveId() throws Exception {
        return json.readTree(arrive().andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("id")
                .asLong();
    }

    private DayCareAttendanceApi api() {
        return context.getBean(DayCareAttendanceApi.class);
    }

    private org.springframework.test.web.servlet.ResultActions leave(long id, long pickup, Instant when)
            throws Exception {
        return mvc.perform(post("/api/v1/day-care-attendances/" + id + "/check-out")
                .header("Idempotency-Key", "leave-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pickupRecordId\":" + pickup + ",\"at\":\"" + when + "\"}"));
    }

    @Test
    void BR_M04_02_arrivalPersistsConfirmedRecordAndPublishesPlanningAndAttendance() throws Exception {
        long id = arriveId();
        assertThat(api().onDate(resident, date).orElseThrow().status()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(api().onDate(resident, date).orElseThrow().id()).isEqualTo(id);
        assertThat(events.stream(DayCareCheckedIn.class)
                        .filter(e -> e.residentId() == resident)
                        .count())
                .isEqualTo(1);
        assertThat(events.stream(TaskPlanningNeeded.class)
                        .filter(e -> e.residentId() == resident)
                        .count())
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.ATTENDANCE_RECORD' AND entity_type='M01.RESIDENT' AND entity_id=? AND confirmed_at IS NOT NULL",
                        Integer.class,
                        resident))
                .isEqualTo(1);
    }

    @Test
    void BR_M04_02_onlyValidPickupCanCheckOutAndItsRecordIsMarkedUsed() throws Exception {
        long id = arriveId(), pickup = TestUsers.nextId();
        leave(id, pickup, at.plusSeconds(60)).andExpect(status().isConflict());
        selfReturnPickup(pickup, at);
        leave(id, pickup, at.plusSeconds(60))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendance.checkoutType").value("SELF_RETURN"));
        assertThat(session().attendance())
                .as("về sớm vẫn là Có mặt, hệ số 100%")
                .isEqualTo(Attendance.PRESENT);
        assertThat(session().coefficientPercent()).isEqualTo(100);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M10.PICKUP_RECORD_EVENT' AND entity_type='M10.PICKUP_RECORD' AND entity_id=?",
                        Integer.class,
                        pickup))
                .isEqualTo(1);
        assertThat(api().onDate(resident, date).orElseThrow().status()).isEqualTo(AttendanceStatus.LEFT);
    }

    /** Bản ghi đón Tự về Chưa dùng của 012 (bảng {@code pickup_record}), còn hiệu lực một giờ. */
    private void selfReturnPickup(long id, Instant recordedAt) {
        jdbc.update(
                "INSERT INTO pickup_record(id,resident_id,type,basis,at,handover_staff_id,recorded_by,expires_at,created_at) VALUES(?,?,'SELF_RETURN','SELF_RETURN',?,?,?,?,?)",
                id,
                resident,
                LocalDateTime.ofInstant(recordedAt, ZoneOffset.UTC),
                user.accountId(),
                user.accountId(),
                LocalDateTime.ofInstant(recordedAt.plusSeconds(3600), ZoneOffset.UTC),
                LocalDateTime.ofInstant(recordedAt, ZoneOffset.UTC));
    }

    private org.springframework.test.web.servlet.ResultActions notice(Instant familyNotifiedAt) throws Exception {
        return mvc.perform(post("/api/v1/day-care-absence-notices")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":" + resident + ",\"date\":\"" + date + "\",\"familyNotifiedAt\":\""
                        + familyNotifiedAt + "\"}"));
    }

    private Instant timely() {
        return date.atTime(8, 0).atZone(BusinessTime.ZONE).toInstant().minus(Duration.ofDays(2));
    }

    private AttendanceStatus attendance() {
        return api().onDate(resident, date).orElseThrow().status();
    }

    /** Buổi hôm nay như 004 đọc qua {@code DayCareAttendancePort} của 005 (FR-060; hợp đồng có một khung mỗi ngày). */
    private DayCareSessionView session() {
        var sessions = context.getBean(ResidencyDayApi.class).dayCareSessions(resident, date, date);
        assertThat(sessions).hasSize(1);
        assertThat(sessions.getFirst().sessionId()).isEqualTo(1);
        return sessions.getFirst();
    }

    private Long stored(String field) {
        String value = jdbc.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(content, ?)) FROM simple_record WHERE unique_key=?",
                String.class,
                "$." + field,
                "M04.DAY_CARE_ATTENDANCE:" + resident + ":" + date);
        return value == null || value.equals("null") ? null : Long.valueOf(value);
    }

    private List<Long> notices() {
        return jdbc.queryForList(
                "SELECT id FROM absence WHERE kind='DAY_CARE_NOTICE' AND resident_id=? ORDER BY id",
                Long.class,
                resident);
    }

    @Test
    void FR_013_dayStartReadsCalendarOf004_withoutNoticeNotArrived() {
        daily("DAY_CARE_DAY_START").run(date, time.now());
        assertThat(attendance()).isEqualTo(AttendanceStatus.NOT_ARRIVED);
        assertThat(session().attendance()).as("Chưa đến: buổi chưa xác định").isNull();
        assertThat(session().coefficientPercent()).isNull();
    }

    @Test
    void FR_013_timelyNoticeRecordedBy004_makesReportedAbsentAndBlocksArrival() throws Exception {
        notice(timely())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.withinNoticePeriod").value(true));
        assertThat(attendance()).isEqualTo(AttendanceStatus.REPORTED_ABSENT);
        assertThat(stored("timelyAbsenceId")).isEqualTo(notices().getFirst());
        arrive().andExpect(status().isConflict());
        assertThat(session().attendance()).isEqualTo(Attendance.ABSENT_NOTIFIED);
    }

    @Test
    void FR_014_lateNoticeKeepsStateAndIsStored_laterTimelyNoticeStillCounts() throws Exception {
        daily("DAY_CARE_DAY_START").run(date, time.now());
        notice(time.now())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.withinNoticePeriod").value(false));
        assertThat(attendance()).isEqualTo(AttendanceStatus.NOT_ARRIVED);
        assertThat(stored("lateAbsenceId")).isEqualTo(notices().getFirst());
        assertThat(stored("timelyAbsenceId")).isNull();
        notice(timely()).andExpect(status().isCreated());
        assertThat(attendance()).isEqualTo(AttendanceStatus.REPORTED_ABSENT);
        assertThat(stored("timelyAbsenceId")).isEqualTo(notices().get(1));
        assertThat(stored("lateAbsenceId")).isEqualTo(notices().getFirst());
    }

    @Test
    void FR_013_closureDayDeclaredBy004_cancelsTodaysAttendance() throws Exception {
        daily("DAY_CARE_DAY_START").run(date, time.now());
        mvc.perform(post("/api/v1/day-care-closure-days")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"" + date + "\",\"reason\":\"Nghỉ lễ\"}"))
                .andExpect(status().isCreated());
        assertThat(attendance()).isEqualTo(AttendanceStatus.HOLIDAY_CANCELLED);
        context.getBean(AttendanceClockWork.class).run(time.now());
        assertThat(attendance()).isEqualTo(AttendanceStatus.HOLIDAY_CANCELLED);
    }

    @Test
    void CFG_M02_07_noShowPastLimitBecomesUnreportedAbsent_withCalendarOf004() {
        daily("DAY_CARE_DAY_START").run(date, time.now());
        context.getBean(AttendanceClockWork.class).run(time.now());
        assertThat(attendance()).isEqualTo(AttendanceStatus.UNREPORTED_ABSENT);
        assertThat(session().attendance()).isEqualTo(Attendance.ABSENT_UNNOTIFIED);
        assertThat(events.stream(UnreportedAbsence.class)
                        .filter(e -> e.residentId() == resident)
                        .count())
                .isEqualTo(1);
    }

    @Test
    void FR_019_stillPresentAtDayEndNotifiesOnce() throws Exception {
        arriveId();
        Instant nextMorning =
                date.plusDays(1).atTime(0, 5).atZone(BusinessTime.ZONE).toInstant();
        assertThat(daily("DAY_CARE_DAY_END").run(date, nextMorning)).isEqualTo(1);
        assertThat(daily("DAY_CARE_DAY_END").run(date, nextMorning)).isZero();
        assertThat(events.stream(DayCarePresentAtDayEnd.class)
                        .filter(e -> e.residentId() == resident)
                        .count())
                .isEqualTo(1);
        assertThat(attendance()).isEqualTo(AttendanceStatus.PRESENT);
    }

    private long task(long owner, Instant planned, String state) {
        long id = TestUsers.nextId();
        String data = json.writeValueAsString(new com.eldercare.backend.care.internal.task.domain.CareTaskData(
                1, "Loại test", "Công việc test", "PERFORMED", false, null, Set.of(), null, null, true));
        jdbc.update(
                "INSERT INTO care_task(id,resident_id,task_type_id,basis_type,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,status,data,created_by,created_at) VALUES(?,?,1,'AD_HOC',?,?, ?,?,?,'NORMAL','CAREGIVER',?,?,?,?)",
                id,
                owner,
                TestUsers.nextId(),
                floor,
                LocalDateTime.ofInstant(planned, BusinessTime.ZONE),
                LocalDateTime.ofInstant(planned, BusinessTime.ZONE),
                LocalDateTime.ofInstant(planned.plusSeconds(600), BusinessTime.ZONE),
                state,
                data,
                user.accountId(),
                LocalDateTime.ofInstant(at, BusinessTime.ZONE));
        return id;
    }

    private CareDailyWork daily(String name) {
        return context.getBeansOfType(CareDailyWork.class).values().stream()
                .filter(w -> w.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Chưa cài việc " + name));
    }
}

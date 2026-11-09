package com.eldercare.backend.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.billing.api.events.ProcurementRequestChanged;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * 019 Phase 3 (US7): lệnh thật, MySQL và kiểm quyền thật của 002; nhiệm vụ Phụ trách bộ phận đặt bằng {@link FakeDuties}
 * (008). Bộ phận Bếp có Phụ trách là Nhân viên bếp; bộ phận Kỹ thuật mới chưa có Phụ trách (Q-292).
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
@Import({SwitchableUserConfig.class, FakeM15Config.class, FakeFileConfig.class})
abstract class ProcurementTestBase extends MySqlIntegrationTestBase {
    static final String URL = "/api/v1/procurement-requests";
    static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakeDuties duties;

    @Autowired
    FileApi files;

    @Autowired
    ApplicationEvents events;

    long kitchen, technical;
    TestUsers.TestUser kitchenLead, kitchenStaff, technicalStaff, manager, accountant, otherAccountant;

    @BeforeEach
    void procurementFixture() {
        var users = new TestUsers(jdbc, time);
        kitchen = TestUsers.nextId();
        technical = TestUsers.nextId();
        kitchenLead = users.staff(Role.KITCHEN_STAFF);
        kitchenStaff = users.staff(Role.KITCHEN_STAFF);
        technicalStaff = users.staff(Role.OFFICE_STAFF);
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        accountant = users.staff(Role.ACCOUNTANT);
        otherAccountant = users.staff(Role.ACCOUNTANT);
        duties.departmentLead(kitchen, kitchenLead.staffId())
                .department(kitchenStaff.staffId(), kitchen)
                .department(technicalStaff.staffId(), technical);
        user.set(kitchenLead.user());
    }

    void as(TestUsers.TestUser who) {
        user.set(who.user());
    }

    /** Thân lập yêu cầu nguyên liệu: 50 kg gạo × 30.000, 20 kg thịt × 25.000 = 2.000.000 đồng (US7 kịch bản 1). */
    String ingredientBody(long department) {
        return body(department, "INGREDIENT", "Hết gạo cho tuần sau");
    }

    String body(long department, String type, String reason) {
        return "{\"departmentId\":" + department + ",\"type\":\"" + type + "\",\"reason\":\"" + reason
                + "\",\"neededOn\":\"2026-11-10\",\"lines\":["
                + "{\"name\":\"Gạo\",\"quantity\":50,\"estimatedUnitPrice\":30000},"
                + "{\"name\":\"Thịt\",\"quantity\":20,\"estimatedUnitPrice\":25000}]}";
    }

    ResultActions create(String body) throws Exception {
        return mvc.perform(post(URL)
                .header("Idempotency-Key", "ps-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    long created(String body) throws Exception {
        String location = create(body)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    ResultActions command(long id, String command, String body) throws Exception {
        return mvc.perform(post(URL + "/" + id + "/" + command)
                .header("Idempotency-Key", "pc-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    ResultActions command(long id, String command) throws Exception {
        return command(id, command, "{}");
    }

    /** Yêu cầu nguyên liệu Bếp đã Đã duyệt với số được duyệt đã cho. */
    long approved(long approvedAmount) throws Exception {
        as(kitchenLead);
        long id = created(ingredientBody(kitchen));
        command(id, "submit").andExpect(status().isOk());
        as(manager);
        command(id, "approve", "{\"approvedAmount\":" + approvedAmount + ",\"reason\":\"Giảm theo giá chợ\"}")
                .andExpect(status().isOk());
        return id;
    }

    long receipt(TestUsers.TestUser uploader) {
        return files.upload(
                "hoa-don.pdf",
                FileTypes.PDF,
                "%PDF-1.4 hoa don".getBytes(StandardCharsets.UTF_8),
                uploader.accountId());
    }

    String purchase(long actual, TestUsers.TestUser uploader) {
        return "{\"actualAmount\":" + actual + ",\"purchasedOn\":\"2026-11-05\",\"supplier\":\"Chợ Bến Thành\","
                + "\"receiptFileIds\":[" + receipt(uploader) + "]}";
    }

    /** Đã ghi đã mua (Chờ đối chiếu) với số thực tế đã cho. */
    long purchased(long approvedAmount, long actual) throws Exception {
        long id = approved(approvedAmount);
        as(kitchenLead);
        command(id, "record-purchased", purchase(actual, kitchenLead)).andExpect(status().isOk());
        return id;
    }

    String stateOf(long id) {
        return jdbc.queryForObject("SELECT status FROM procurement_request WHERE id=?", String.class, id);
    }

    List<ProcurementRequestChanged> changes(long id) {
        return events.stream(ProcurementRequestChanged.class)
                .filter(e -> e.requestId() == id)
                .toList();
    }

    /** Nhóm người nhận của thông báo sinh ở lần chuyển tới {@code to}. */
    List<RecipientGroup> recipients(long id, String to) {
        return changes(id).stream()
                .filter(e -> e.to().name().equals(to))
                .flatMap(e -> e.notificationRequests().stream())
                .flatMap(n -> n.recipientGroups().stream())
                .toList();
    }
}

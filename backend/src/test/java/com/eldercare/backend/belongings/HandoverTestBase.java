package com.eldercare.backend.belongings;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.staffing.api.dto.ShiftView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.family.FakeFamilyRights;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import com.eldercare.backend.support.fake.staffing.FakeShifts;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Nền US2 của 013 (bàn giao, tách, đính chính): đồ tạo bằng lệnh Tiếp nhận thật; ca của 008 giả ({@code FakeShifts},
 * chỉ bản giả tra cứu ca, không thay {@code HeldBelongingsHandoverPort} của 013).
 */
@Import({FakeFamilyRights.Config.class, FakeFileConfig.class, HandoverTestBase.Shifts.class})
abstract class HandoverTestBase extends IntakeTestBase {

    @TestConfiguration(proxyBeanMethods = false)
    static class Shifts {
        @Bean
        @Primary
        FakeShifts belongingsFakeShifts() {
            return new FakeShifts();
        }
    }

    @Autowired
    FakeShifts shifts;

    TestUsers.TestUser officeA;
    TestUsers.TestUser officeB;
    String tokenA;
    String tokenB;

    @BeforeEach
    void setUpHandover() {
        officeA = users.staff(Role.OFFICE_STAFF);
        officeB = users.staff(Role.OFFICE_STAFF);
        tokenA = TestUsers.bearer(users.session(officeA.accountId()));
        tokenB = TestUsers.bearer(users.session(officeB.accountId()));
    }

    @AfterEach
    void resetShifts() {
        shifts.reset();
    }

    /** Ca đang diễn ra quanh đồng hồ test có các nhân viên này. */
    long onShift(Long lead, long... staffIds) {
        long id = TestUsers.nextId();
        var now = time.now();
        shifts.shift(
                new ShiftView(
                        id,
                        TestUsers.nextId(),
                        now.minus(Duration.ofHours(1)),
                        now.plus(Duration.ofHours(1)),
                        "IN_PROGRESS",
                        lead),
                java.util.Arrays.stream(staffIds).boxed().toList());
        return id;
    }

    /** Tiếp nhận bằng lệnh thật của US1 do Hành chính A; trả mã đồ. */
    long intake(long resident, String typeCode, String amount, long location) throws Exception {
        String body = "{\"typeId\": " + typeId(typeCode) + ", \"description\": \"Đồ thử\", " + amount + ","
                + " \"condition\": {\"levelId\": " + levelId("SIGNS_OF_USE") + "}, \"giver\": {\"type\": \"RESIDENT\"},"
                + " \"at\": \"2026-11-05T08:00:00+07:00\", \"storageLocationId\": " + location
                + ", \"photoFileIds\": [" + photo() + "]}";
        return id(postJson(tokenA, "/api/v1/residents/" + resident + "/belongings", body)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    int version(long belongingId) {
        return jdbc.queryForObject("SELECT version FROM belonging WHERE id = ?", Integer.class, belongingId);
    }

    String statusOf(long belongingId) {
        return jdbc.queryForObject("SELECT status FROM belonging WHERE id = ?", String.class, belongingId);
    }

    BigDecimal quantityOf(long belongingId) {
        return jdbc.queryForObject("SELECT quantity FROM belonging WHERE id = ?", BigDecimal.class, belongingId);
    }

    /** Các đồ cùng lần tiếp nhận của đồ (gồm đồ tách), không gồm đồ bị loại. */
    List<Long> sameIntake(long belongingId) {
        return jdbc.queryForList(
                "SELECT b.id FROM belonging b JOIN belonging o ON o.intake_handover_id = b.intake_handover_id"
                        + " WHERE o.id = ? AND b.excluded = FALSE ORDER BY b.id",
                Long.class,
                belongingId);
    }

    BigDecimal intakeTotal(long belongingId) {
        return jdbc.queryForObject(
                "SELECT SUM(COALESCE(b.quantity, b.cash_amount)) FROM belonging b JOIN belonging o"
                        + " ON o.intake_handover_id = b.intake_handover_id WHERE o.id = ? AND b.excluded = FALSE",
                BigDecimal.class,
                belongingId);
    }

    long latestHandover(long belongingId) {
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM belonging_handover WHERE belonging_id = ?", Long.class, belongingId);
    }

    ResultActions handover(String token, String body) throws Exception {
        return postJson(token, "/api/v1/belongings/handovers", body);
    }

    String item(long belongingId, String extra) {
        return "{\"belongingId\": " + belongingId + ", \"version\": " + version(belongingId) + extra + "}";
    }

    String command(String type, String items, String extra) {
        return "{\"type\": \"" + type + "\", \"items\": [" + items + "]" + extra + "}";
    }
}

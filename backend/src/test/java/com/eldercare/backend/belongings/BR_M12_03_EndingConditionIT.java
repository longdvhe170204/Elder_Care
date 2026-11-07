package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.belongings.internal.ending.service.BelongingPostDeathItem;
import com.eldercare.backend.belongings.internal.ending.service.FinalStateReminders;
import com.eldercare.backend.incident.api.dto.IncidentView;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.incident.FakeIncidents;
import com.eldercare.backend.support.fake.incident.FakeM05Config;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 013 T022 (US5; BR-M12-03, FR-025 → FR-027, Q-148; quickstart Q29 → Q32): điều kiện (b) kết thúc lưu trú, tham gia
 * chuyển trạng thái {@code END_B_BELONGINGS} (miễn được), mục "xử lý đồ gửi" sau qua đời, đồ Tìm thấy sau khi mục Hoàn
 * thành, nhắc CFG-M12-02.
 */
@Import(FakeM05Config.class)
class BR_M12_03_EndingConditionIT extends HandoverTestBase {

    @Autowired
    ResidentStatusApi residentStatus;

    @Autowired
    BelongingPostDeathItem postDeathItem;

    @Autowired
    FinalStateReminders reminders;

    @Autowired
    FakeIncidents incidents;

    private void setStatus(long resident, String status, int daysAgo) {
        jdbc.update(
                "UPDATE resident SET status = ?, status_since = ? WHERE id = ?",
                status,
                LocalDateTime.ofInstant(time.now().minus(Duration.ofDays(daysAgo)), ZoneOffset.UTC),
                resident);
    }

    @Test
    void Q29_heldAndInUseBelongingsMakeTheConditionNotMet_andReturningThemMeetsIt() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long a = intake(resident, "CLOTHING", "\"quantity\": 1", location());
        long b = intake(resident, "GLASSES", "\"quantity\": 1", location());
        long c = intake(resident, "PERSONAL_ITEMS", "\"quantity\": 1", location());
        jdbc.update(
                "UPDATE belonging SET status = 'IN_USE', holder_type = 'RESIDENT', holder_id = ?, storage_location_id = NULL WHERE id = ?",
                resident,
                c);

        getJson(tokenA, "/api/v1/residents/" + resident + "/belongings/ending-condition")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.met").value(false))
                .andExpect(jsonPath("$.blocking.length()").value(3));
        var conditions = residentStatus.previewConditions(resident, StatusCommand.END_RESIDENCY);
        assertThat(conditions)
                .filteredOn(x -> "END_B_BELONGINGS".equals(x.code()))
                .singleElement()
                .satisfies(x -> assertThat(x.waivable()).isTrue());

        jdbc.update(
                "UPDATE belonging SET status = 'RETURNED', holder_type = NULL, holder_id = NULL WHERE id IN (?, ?)",
                a,
                b);
        getJson(tokenA, "/api/v1/residents/" + resident + "/belongings/ending-condition")
                .andExpect(jsonPath("$.blocking.length()").value(1));
        jdbc.update("UPDATE belonging SET status = 'RETURNED', holder_type = NULL, holder_id = NULL WHERE id = ?", c);
        getJson(tokenA, "/api/v1/residents/" + resident + "/belongings/ending-condition")
                .andExpect(jsonPath("$.met").value(true));
        assertThat(residentStatus.previewConditions(resident, StatusCommand.END_RESIDENCY))
                .noneMatch(x -> "END_B_BELONGINGS".equals(x.code()));
    }

    @Test
    void Q30_lostItemDoesNotBlock_butItsOpenIncidentIsListedForCondition_d() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        long incident = TestUsers.nextId();
        incidents.put(new IncidentView(incident, resident, "BELONGINGS_LOST", "MEDIUM", "NEW", Instant.now(), null));
        handover(
                        tokenA,
                        command(
                                "REPORT_LOST",
                                item(phone, ", \"incident\": {\"method\": \"LINK\", \"incidentId\": " + incident + "}"),
                                ", \"reason\": \"Mất\""))
                .andExpect(status().isOk());

        getJson(tokenA, "/api/v1/residents/" + resident + "/belongings/ending-condition")
                .andExpect(jsonPath("$.met").value(true))
                .andExpect(jsonPath("$.lostWithOpenIncident.length()").value(1));
    }

    @Test
    void Q31_postDeathItem_isNotApplicable_completed_andNotReopenedByAFoundItem() throws Exception {
        long none = resident("IN_RESIDENCE");
        setStatus(none, "DECEASED", 1);
        assertThat(postDeathItem.status(none).status()).isEqualTo(PostDeathItemStatus.NOT_APPLICABLE);

        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 1", location());
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location());
        setStatus(resident, "DECEASED", -1);
        assertThat(postDeathItem.status(resident).status()).isEqualTo(PostDeathItemStatus.NOT_COMPLETED);

        handover(tokenA, command("REPORT_LOST", item(phone, ""), ", \"reason\": \"Mất\""))
                .andExpect(status().isOk());
        jdbc.update(
                "UPDATE belonging SET status = 'RETURNED', holder_type = NULL, holder_id = NULL WHERE id = ?", clothes);
        assertThat(postDeathItem.status(resident).status()).isEqualTo(PostDeathItemStatus.COMPLETED);

        long photo = photo();
        handover(
                        tokenA,
                        command(
                                "FOUND",
                                item(phone, ", \"photoFileIds\": [" + photo + "]"),
                                ", \"storageLocationId\": " + location()))
                .andExpect(status().isOk());

        assertThat(statusOf(phone)).isEqualTo("HELD");
        assertThat(postDeathItem.status(resident).status()).isEqualTo(PostDeathItemStatus.COMPLETED);
        assertThat(jdbc.queryForObject(
                        "SELECT disposal_clock_start_at IS NOT NULL FROM belonging WHERE id = ?", Boolean.class, phone))
                .isTrue();
    }

    @Test
    void Q32_remindsOfficeStaffEveryCycle_forUnreturnedItemsAfterTheFinalState() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 1", location());
        setStatus(resident, "RESIDENCY_ENDED", 8);

        reminders.remindUnreturned();
        long first = remindersFor(resident);
        reminders.remindUnreturned();

        assertThat(first).isEqualTo(1);
        assertThat(remindersFor(resident)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT last_reminded_at IS NOT NULL FROM belonging WHERE id = ?", Boolean.class, clothes))
                .isTrue();
    }

    private long remindersFor(long resident) {
        return notices().stream()
                .filter(n -> "BELONGING_UNRETURNED_AFTER_FINAL".equals(n.eventType())
                        && n.residentId().filter(id -> id == resident).isPresent())
                .count();
    }
}

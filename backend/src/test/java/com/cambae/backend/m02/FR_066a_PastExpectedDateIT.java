package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.events.M02Reminder;
import com.cambae.backend.m02.api.events.ResidencyDaysChanged;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T057 (US7; FR-066a, FR-077, BR-M02-19): hết ngày kết thúc dự kiến mà hồ sơ vẫn Đang chuẩn bị thì Bộ lập lịch gắn dấu
 * "quá ngày dự kiến" từ ngày liền sau (đúng ngày kể cả chạy bù), các ngày sau lại là sự kiện tính phí, Hành chính được nhắc,
 * lệnh Kết thúc lưu trú bị chặn tới khi có ngày mới không sớm hơn hôm nay.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_066a_PastExpectedDateIT extends EndingTestBase {

    private static final String REMINDER = "PAST_EXPECTED_DATE";

    private LocalDate overdueSince(long id) {
        return jdbc.queryForObject("SELECT overdue_since FROM residency_ending WHERE id = ?", LocalDate.class, id);
    }

    private List<M02Reminder> reminders(long id) {
        return events.stream(M02Reminder.class)
                .filter(r -> REMINDER.equals(r.type()) && r.subjectId() == id)
                .toList();
    }

    @Test
    void FR_066a_onTheExpectedDayItself_thereIsNoMark_theDayAfterTheMarkIsSet() throws Exception {
        long residentId = inResidence();
        LocalDate expected = d0.plusDays(2);
        long id = createEnding(residentId, expected);

        runJobOn(expected);
        assertThat(overdueSince(id)).isNull();

        runJobOn(expected.plusDays(1));

        assertThat(overdueSince(id)).isEqualTo(expected.plusDays(1));
        assertThat(ending(id).get("overdueSince").asString())
                .isEqualTo(expected.plusDays(1).toString());
        assertThat(events.stream(ResidencyDaysChanged.class)
                        .filter(e -> e.residentId() == residentId && e.cause().equals("RESIDENCY_ENDING_OVERDUE")))
                .as("ngày liền sau lại là sự kiện tính phí (010 tính lại)")
                .anySatisfy(e -> assertThat(e.fromDate()).isEqualTo(expected.plusDays(1)));
        assertThat(reminders(id)).as("Hành chính được nhắc đặt ngày mới").hasSize(1);
    }

    @Test
    void FR_077_catchUp_marksTheExactDay_andMergesTheRemindersIntoOne() throws Exception {
        long residentId = inResidence();
        LocalDate expected = d0.plusDays(2);
        long id = createEnding(residentId, expected);
        runJobOn(expected);

        runJobOn(expected.plusDays(4));

        assertThat(overdueSince(id)).as("đúng ngày lẽ ra phải gắn").isEqualTo(expected.plusDays(1));
        assertThat(reminders(id)).hasSize(1);
        runJobOn(expected.plusDays(4));
        assertThat(reminders(id)).as("chạy lại không tạo trùng").hasSize(1);
        assertThat(overdueSince(id)).isEqualTo(expected.plusDays(1));
    }

    @Test
    void FR_066a_remindsEveryDay_whileItStaysPast() throws Exception {
        long residentId = inResidence();
        LocalDate expected = d0.plusDays(1);
        long id = createEnding(residentId, expected);
        runJobOn(expected);

        runJobOn(expected.plusDays(1));
        runJobOn(expected.plusDays(2));
        runJobOn(expected.plusDays(3));

        assertThat(reminders(id)).hasSize(3);
    }

    @Test
    void FR_066a_thePastMark_blocksTheCommand_untilANewDateNotEarlierThanTodayIsSet() throws Exception {
        long residentId = inResidence();
        LocalDate expected = d0.plusDays(1);
        long id = createEnding(residentId, expected);
        handover(id);
        runJobOn(expected);
        runJobOn(expected.plusDays(2));
        assertThat(overdueSince(id)).isEqualTo(expected.plusDays(1));

        BusinessException blocked = endResidencyFailure(residentId, id, at(expected.plusDays(2), 12, 0));
        assertThat(unmetCodes(blocked)).containsExactly("END_PAST_EXPECTED_DATE");

        LocalDate newDate = expected.plusDays(4);
        changeDateRaw(id, newDate, "Gia đình hẹn ngày mới").andExpect(status().isOk());

        assertThat(overdueSince(id)).as("dấu được gỡ").isNull();
        assertThat(calls()).last().isEqualTo("recalculate:" + residentId + ":" + newDate);
        noonOf(newDate);
        endResidency(residentId, id, at(newDate, 12, 0));
        assertThat(residentStatus(residentId)).isEqualTo(ResidentStatus.RESIDENCY_ENDED);
    }

    @Test
    void FR_066a_aNewDateEarlierThanToday_keepsTheMark_fromTheDayAfterTheNewDate() throws Exception {
        long residentId = inResidence();
        LocalDate expected = d0.plusDays(1);
        long id = createEnding(residentId, expected);
        runJobOn(expected);
        runJobOn(expected.plusDays(3));

        changeDateRaw(id, expected.plusDays(1), "Ngày mới đã qua").andExpect(status().isOk());
        assertThat(overdueSince(id)).isEqualTo(expected.plusDays(2));
        runJobOn(expected.plusDays(4));

        assertThat(overdueSince(id)).isEqualTo(expected.plusDays(2));
    }

    @Test
    void FR_066a_endingsThatAreNoLongerPreparing_areNotMarked() throws Exception {
        long residentId = inResidence();
        LocalDate expected = d0.plusDays(1);
        long id = createEnding(residentId, expected);
        cancelRaw(office, id, "Đổi ý").andExpect(status().isOk());
        runJobOn(expected);

        runJobOn(expected.plusDays(2));

        assertThat(overdueSince(id)).isNull();
        assertThat(reminders(id)).isEmpty();
    }
}

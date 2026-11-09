package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.events.M02Reminder;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
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
        return jdbc.queryForObject(
                "SELECT overdue_since FROM residency_ending WHERE kind = 'ENDING' AND id = ?", LocalDate.class, id);
    }

    private List<M02Reminder> reminders(long id) {
        return events.stream(M02Reminder.class)
                .filter(r -> REMINDER.equals(r.type()) && r.subjectId() == id)
                .toList();
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
}

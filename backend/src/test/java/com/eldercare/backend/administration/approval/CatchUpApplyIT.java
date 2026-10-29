package com.eldercare.backend.administration.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * US5 #9; BR-M15-10, FR-037a (000 T085; quickstart K10): Bộ lập lịch gián đoạn qua ngày hiệu lực thì lần chạy sau áp
 * dụng bù theo thứ tự (ngày hiệu lực thực tế, thời điểm duyệt), đánh dấu "áp dụng bù", nhật ký có cả ngày hiệu lực và
 * thời điểm áp dụng.
 */
class CatchUpApplyIT extends ApprovalTestBase {

    @Test
    void US5_9_missedDays_applyInOrder_markedCatchUp() throws Exception {
        LocalDate first = time.today().plusDays(10);
        LocalDate second = first.plusDays(1);
        long laterDate = approved(second);
        long earlierDate = approved(first);
        long sameDateDecidedLater = approved(second);

        moveTo(second.plusDays(2));
        runApplyJob();

        for (long id : List.of(laterDate, earlierDate, sameDateDecidedLater)) {
            assertThat(read(id).get("status").asString()).isEqualTo("APPLIED");
            assertThat(read(id).get("catchUpApplied").asBoolean()).isTrue();
        }
        List<Long> order = jdbc.queryForList(
                "SELECT CAST(entity_id AS UNSIGNED) FROM audit_log WHERE action = 'REQUEST_APPLIED'"
                        + " AND entity_id IN (?, ?, ?) ORDER BY id",
                Long.class,
                String.valueOf(laterDate),
                String.valueOf(earlierDate),
                String.valueOf(sameDateDecidedLater));
        assertThat(order).containsExactly(earlierDate, laterDate, sameDateDecidedLater);
        assertThat(lastAudit(
                        earlierDate, "REQUEST_APPLIED", "JSON_UNQUOTE(JSON_EXTRACT(details, '$.actualEffectiveDate'))"))
                .isEqualTo(first.toString());
        assertThat(lastAudit(earlierDate, "REQUEST_APPLIED", "JSON_EXTRACT(details, '$.catchUpApplied')"))
                .isEqualTo("true");
        assertThat(lastAudit(earlierDate, "REQUEST_APPLIED", "JSON_EXTRACT(details, '$.appliedAt')"))
                .isNotNull();
    }
}

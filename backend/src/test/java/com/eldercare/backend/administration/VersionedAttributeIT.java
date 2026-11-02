package com.eldercare.backend.administration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.sample.SampleService;
import com.eldercare.backend.administration.sample.SampleUsers;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FR-004 (000 T047; chủ dự án chốt 2026-10-19): đổi thuộc tính theo phiên bản bằng phiên bản mới có ngày hiệu lực.
 * Phiên bản đang mở được đóng ở ngày trước ngày hiệu lực mới, chỉ khi ngày đó sau hôm nay; phần đã áp dụng không đổi.
 * Tài khoản ứng dụng chỉ sửa được cột {@code effective_to}.
 */
@Import(SampleUsers.class)
class VersionedAttributeIT extends MySqlIntegrationTestBase {

    @Autowired
    SampleService samples;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    private final Reason reason = new Reason("đổi đơn giá");

    private long newCase() {
        return samples.createCase(samples.createCatalogItem("Đơn giá " + UUID.randomUUID()), 1);
    }

    private List<Map<String, Object>> versions(long caseId) {
        return jdbc.queryForList(
                "SELECT effective_from, effective_to FROM sample_case_version WHERE case_id = ? ORDER BY effective_from",
                caseId);
    }

    private static Map<String, Object> range(LocalDate from, LocalDate to) {
        HashMap<String, Object> row = new HashMap<>();
        row.put("effective_from", Date.valueOf(from));
        row.put("effective_to", to == null ? null : Date.valueOf(to));
        return row;
    }

    @Test
    void FR_004_newVersionFromFutureDate_closesOpenVersion_keepsAppliedPart() {
        LocalDate today = time.today();
        long caseId = newCase();
        samples.addCaseVersion(caseId, today.minusDays(30), null, reason);

        samples.addCaseVersionFrom(caseId, today.plusDays(10), reason);

        assertThat(versions(caseId))
                .containsExactly(range(today.minusDays(30), today.plusDays(9)), range(today.plusDays(10), null));
    }

    @Test
    void FR_004_newVersionNotAfterToday_isRejected_openVersionUnchanged() {
        LocalDate today = time.today();
        long caseId = newCase();
        samples.addCaseVersion(caseId, today.minusDays(30), null, reason);

        assertThatThrownBy(() -> samples.addCaseVersionFrom(caseId, today, reason))
                .isInstanceOf(ConditionNotMetException.class);

        assertThat(versions(caseId)).containsExactly(range(today.minusDays(30), null));
    }

    @Test
    void FR_004_appAccount_canOnlyCloseEndDate() {
        long caseId = newCase();
        samples.addCaseVersion(caseId, time.today().minusDays(30), null, reason);

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE sample_case_version SET effective_from = effective_from - INTERVAL 1 DAY WHERE case_id = ?",
                        caseId))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("UPDATE command denied");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM sample_case_version WHERE case_id = ?", caseId))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("DELETE command denied");
    }
}

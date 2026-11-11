package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.admission.api.ResidencyStatisticsApi;
import com.eldercare.backend.admission.api.dto.ResidenceTypeChangeView;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 016 T014 (FR-023 "đổi loại lưu trú", chủ dự án duyệt 2026-11-11): bản thật
 * {@code ResidencyStatisticsApi.residenceTypeChanges} trả phiên bản nội dung hợp đồng đã ký có loại lưu trú khác phiên bản
 * liền trước, theo ngày hiệu lực trong khoảng (hai đầu), lọc theo người cao tuổi của phạm vi.
 */
@Transactional
class T014_ResidenceTypeChangeStatisticsIT extends MySqlIntegrationTestBase {

    private static final LocalDate START = LocalDate.parse("1990-03-01");
    private static final Instant CREATED = Instant.parse("1990-03-01T00:00:00Z");

    @Autowired
    ResidencyStatisticsApi statistics;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_023_versionWithDifferentResidenceType_inRange_isAChange() {
        long a = TestUsers.nextId();
        long b = TestUsers.nextId();
        long other = TestUsers.nextId();
        long contractA = contract(a, "ACTIVE");
        version(contractA, 0, START, "LONG_TERM_RESIDENTIAL", "BASIC");
        version(contractA, 1, START.plusDays(5), "DAY_CARE", "BASIC");
        version(contractA, 2, START.plusDays(8), "DAY_CARE", "SPECIAL");
        long contractB = contract(b, "ACTIVE");
        version(contractB, 0, START, "DAY_CARE", "BASIC");
        version(contractB, 1, START.plusDays(40), "LONG_TERM_RESIDENTIAL", "BASIC");
        long draft = contract(other, "DRAFT");
        version(draft, 0, START, "LONG_TERM_RESIDENTIAL", "BASIC");
        version(draft, 1, START.plusDays(3), "DAY_CARE", "BASIC");

        var rows = statistics
                .residenceTypeChanges(
                        new ScopeFilter(false, Set.of(), Set.of(), Set.of(a, b, other)), START, START.plusDays(30))
                .toList();

        assertThat(rows)
                .extracting(
                        ResidenceTypeChangeView::residentId,
                        ResidenceTypeChangeView::from,
                        ResidenceTypeChangeView::to,
                        ResidenceTypeChangeView::effectiveOn)
                .containsExactly(
                        tuple(a, ResidenceType.LONG_TERM_RESIDENTIAL, ResidenceType.DAY_CARE, START.plusDays(5)));
        assertThat(statistics.residenceTypeChanges(
                        new ScopeFilter(false, Set.of(), Set.of(), Set.of(b)), START, START.plusDays(30)))
                .isEmpty();
    }

    private long contract(long resident, String status) {
        boolean signed = !"DRAFT".equals(status);
        jdbc.update(
                "INSERT INTO contract(resident_id,residence_type,start_date,care_level,residency_fee_billing_period,"
                        + "status,signed_date,signing_representative_id,scan_file_id,created_by,created_at)"
                        + " VALUES(?,'LONG_TERM_RESIDENTIAL',?,'BASIC','MONTH',?,?,?,?,1,?)",
                resident,
                START,
                status,
                signed ? START : null,
                signed ? 1L : null,
                signed ? 1L : null,
                Timestamp.from(CREATED));
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void version(long contract, int sequence, LocalDate from, String type, String level) {
        jdbc.update(
                "INSERT INTO contract_version(kind,contract_id,created_at,sequence_no,effective_from,content,"
                        + "residence_type,care_level) VALUES('CONTENT',?,?,?,?,CAST(? AS JSON),?,?)",
                contract,
                Timestamp.from(CREATED),
                sequence,
                from,
                "{\"residenceType\":\"" + type + "\"}",
                type,
                level);
    }
}

package com.eldercare.backend.common.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.sample.SampleFloorRecord;
import com.eldercare.backend.administration.sample.SampleFloorRecordRepository;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FR-009 (000 T023, T024): nhóm 3 chỉ thêm qua {@link AppendOnlyRepository}; tài khoản {@code eldercare_app} không
 * có quyền DELETE khi migration của bảng không cấp (R-05).
 */
class AppendOnlyRepositoryIT extends MySqlIntegrationTestBase {

    @Autowired
    SampleFloorRecordRepository records;

    @Autowired
    JdbcTemplate jdbc;

    private static SampleFloorRecord record(Long id, String note) {
        return new SampleFloorRecord(id, Instant.parse("2026-10-19T01:00:00Z"), 1, 2, note);
    }

    @Test
    void FR_009_append_rejectsExistingRecord() {
        SampleFloorRecord saved = records.append(record(null, "bản gốc"));

        assertThatThrownBy(() -> records.append(record(saved.getId(), "sửa đè")))
                .isInstanceOf(DataAccessException.class);
        assertThat(records.findById(saved.getId()))
                .get()
                .extracting(SampleFloorRecord::getNote)
                .isEqualTo("bản gốc");
    }
}

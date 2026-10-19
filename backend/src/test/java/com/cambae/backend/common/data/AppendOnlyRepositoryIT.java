package com.cambae.backend.common.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m15.sample.SampleFloorRecord;
import com.cambae.backend.m15.sample.SampleFloorRecordRepository;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
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
    void FR_009_repository_exposesNoSaveOrDelete() {
        List<String> names = Arrays.stream(SampleFloorRecordRepository.class.getMethods())
                .map(Method::getName)
                .toList();

        assertThat(names).contains("append", "findById", "existsById");
        assertThat(names).noneMatch(n -> n.startsWith("save") || n.startsWith("delete"));
    }

    @Test
    void FR_009_append_persistsNewRecord() {
        SampleFloorRecord saved = records.append(record(null, "đo huyết áp"));

        assertThat(records.findById(saved.getId()))
                .get()
                .extracting(SampleFloorRecord::getNote)
                .isEqualTo("đo huyết áp");
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

    @Test
    void R_05_appAccount_cannotDeleteWithoutTableGrant() {
        records.append(record(null, "không được xóa"));

        assertThatThrownBy(() -> jdbc.update("DELETE FROM sample_floor_record"))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("DELETE command denied");
    }
}

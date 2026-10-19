package com.cambae.backend.common.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.lang.reflect.Method;
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
    SampleRecordRepository records;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_009_repository_exposesNoSaveOrDelete() {
        List<String> names = Arrays.stream(SampleRecordRepository.class.getMethods())
                .map(Method::getName)
                .toList();

        assertThat(names).contains("append", "findById", "existsById");
        assertThat(names).noneMatch(n -> n.startsWith("save") || n.startsWith("delete"));
    }

    @Test
    void FR_009_append_persistsNewRecord() {
        SampleRecord saved = records.append(new SampleRecord("đo huyết áp"));

        assertThat(records.findById(saved.getId()))
                .get()
                .extracting(SampleRecord::getNote)
                .isEqualTo("đo huyết áp");
    }

    @Test
    void FR_009_append_rejectsExistingRecord() {
        SampleRecord saved = records.append(new SampleRecord("bản gốc"));
        SampleRecord copy = new SampleRecord(saved.getId(), "sửa đè");

        assertThatThrownBy(() -> records.append(copy)).isInstanceOf(DataAccessException.class);
        assertThat(records.findById(saved.getId()))
                .get()
                .extracting(SampleRecord::getNote)
                .isEqualTo("bản gốc");
    }

    @Test
    void R_05_appAccount_cannotDeleteWithoutTableGrant() {
        records.append(new SampleRecord("không được xóa"));

        assertThatThrownBy(() -> jdbc.update("DELETE FROM sample_record"))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("DELETE command denied");
    }
}

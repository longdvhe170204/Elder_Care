package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.support.TestUsers;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/** T044: đồng hồ chỉ báo quá bắt đầu, không tự bắt đầu phân bổ. */
@Transactional
class FR_023_FutureAllocationPastStartIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Test
    void FR_023_reportsOnlyAdmissionFutureOnceWithoutStartingIt() {
        assertThat(context.containsBean("futureAllocationPastStartJob"))
                .as("Cần job báo quá bắt đầu T044")
                .isTrue();
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE',?,'BASIC','CURRENT')",
                resident,
                bed,
                Timestamp.from(time.now().minusSeconds(3600)));
        Object job = context.getBean("futureAllocationPastStartJob");
        ReflectionTestUtils.invokeMethod(job, "runNow");
        Timestamp notified = jdbc.queryForObject(
                "SELECT notified_planned_start_at FROM bed_allocation WHERE resident_id=?", Timestamp.class, resident);
        assertThat(notified).isNotNull();
        ReflectionTestUtils.invokeMethod(job, "runNow");
        assertThat(jdbc.queryForObject(
                        "SELECT notified_planned_start_at FROM bed_allocation WHERE resident_id=?",
                        Timestamp.class,
                        resident))
                .isEqualTo(notified);
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE resident_id=?", String.class, resident))
                .isEqualTo("FUTURE");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("EMPTY");
    }

    @Test
    void FR_023_doesNotReportFutureTransferOfResidentAlreadyInResidence() {
        assertThat(context.containsBean("futureAllocationPastStartJob")).isTrue();
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE',?,'BASIC','CURRENT')",
                resident,
                bed,
                Timestamp.from(time.now().minusSeconds(3600)));
        ReflectionTestUtils.invokeMethod(context.getBean("futureAllocationPastStartJob"), "runNow");
        assertThat(jdbc.queryForObject(
                        "SELECT notified_planned_start_at FROM bed_allocation WHERE resident_id=?",
                        Timestamp.class,
                        resident))
                .isNull();
    }
}

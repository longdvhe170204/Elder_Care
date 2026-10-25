package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.transaction.annotation.Transactional;

/** T049–T050: ràng buộc dữ liệu thật trên MySQL, không dựa riêng vào validation Java. */
@Transactional
class FR_038_DayCareSchemaIT extends StructureTestSupport {
    @Test
    void FR_038_dayCareTablesExist() {
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('day_care_rest_area','day_care_session')",
                        Integer.class))
                .isEqualTo(2);
    }

    @Test
    void FR_038_oneActiveRestAreaAndExactlyOneLocation() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO day_care_rest_area(code,name) VALUES('D0','Khu nghỉ')"))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasRootCauseMessage("Check constraint 'chk_day_care_location' is violated.");
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO day_care_rest_area(code,name,floor_id,area_id) VALUES('D0','Khu nghỉ',?,?)",
                        floor,
                        area))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasRootCauseMessage("Check constraint 'chk_day_care_location' is violated.");
        jdbc.update("INSERT INTO day_care_rest_area(code,name,floor_id) VALUES('D1','Khu nghỉ',?)", floor);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO day_care_rest_area(code,name,area_id) VALUES('D2','Khu nghỉ khác',?)", area))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("UPDATE day_care_rest_area SET status='INACTIVE' WHERE code='D1'");
        jdbc.update("INSERT INTO day_care_rest_area(code,name,area_id) VALUES('D2','Khu nghỉ khác',?)", area);
    }

    @Test
    void FR_038_activeSessionNameIsUniqueAndTimeIsOrdered() {
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO day_care_session(name,start_time,end_time) VALUES('Khung rỗng','07:00','07:00')"))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasRootCauseMessage("Check constraint 'chk_day_care_session_time' is violated.");
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO day_care_session(name,start_time,end_time) VALUES('Sáng','12:00','07:00')"))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasRootCauseMessage("Check constraint 'chk_day_care_session_time' is violated.");
        jdbc.update("INSERT INTO day_care_session(name,start_time,end_time) VALUES('Sáng','07:00','12:00')");
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO day_care_session(name,start_time,end_time) VALUES('Sáng','13:00','17:00')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("UPDATE day_care_session SET status='INACTIVE' WHERE name='Sáng'");
        jdbc.update("INSERT INTO day_care_session(name,start_time,end_time) VALUES('Sáng','07:00','12:00')");
    }
}

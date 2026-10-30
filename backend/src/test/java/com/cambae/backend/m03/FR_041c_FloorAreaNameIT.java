package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m15.account.api.spi.FloorAreaNameApi;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 FR-041c: M03 cài SPI tên tầng của 002 cho màn bản ghi chờ xem lại; tầng không có thì rỗng. Không nạp bản giả
 * {@code FakeM15Config} để tiêm đúng bản cài thật.
 */
class FR_041c_FloorAreaNameIT extends MySqlIntegrationTestBase {

    @Autowired
    FloorAreaNameApi floorNames;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_041c_floorName_fromStructure_emptyWhenUnknown() {
        long area = TestUsers.nextId();
        long floor = TestUsers.nextId();
        jdbc.update("INSERT INTO area(id,code,name) VALUES(?,?,?)", area, "A" + area, "Khu " + area);
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)", floor, area, "F" + floor, "Tầng " + floor);

        assertThat(floorNames.name(floor)).contains("Tầng " + floor);
        assertThat(floorNames.name(TestUsers.nextId())).isEmpty();
    }
}

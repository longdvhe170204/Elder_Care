package com.cambae.backend.m03;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.internal.structure.service.StructureService;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m03.FakeLocationReferences;
import com.cambae.backend.support.fake.m03.FakeM03Config;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/** Dữ liệu từng test tách bằng id; quyền thật của 002 và fake SPI đúng chủ khai báo. */
@Import({StructureTestSupport.Users.class, FakeM03Config.class, FakeM15Config.class})
abstract class StructureTestSupport extends MySqlIntegrationTestBase {
    @TestConfiguration
    static class Users {
        @Bean
        @Primary
        SwitchableUser structureUser() {
            return new SwitchableUser();
        }
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    StructureService structures;

    @Autowired
    SwitchableUser user;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeLocationReferences references;

    long area, floor, type, room, bed;

    @BeforeEach
    void structureFixture() {
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
        references.reset();
        area = TestUsers.nextId();
        floor = TestUsers.nextId();
        type = TestUsers.nextId();
        room = TestUsers.nextId();
        bed = TestUsers.nextId();
        jdbc.update("INSERT INTO area(id,code,name) VALUES(?,?,?)", area, "A" + area, "Khu " + area);
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)", floor, area, "F" + floor, "Tầng " + floor);
        jdbc.update("INSERT INTO room_type(id,name,standard_beds) VALUES(?,?,2)", type, "Loại " + type);
        jdbc.update("INSERT INTO room_type_care_level VALUES(?,'BASIC')", type);
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(?,?,?,?,?,'UNRESTRICTED')",
                room,
                floor,
                type,
                "R" + room,
                "Phòng " + room);
        jdbc.update("INSERT INTO room_allowed_care_level VALUES(?,'BASIC')", room);
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G1')", bed, room);
    }
}

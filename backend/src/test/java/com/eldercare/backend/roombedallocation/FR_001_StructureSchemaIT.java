package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** T006: ràng buộc cơ cấu phải được MySQL thực thi, không chỉ kiểm ở controller sau này. */
class FR_001_StructureSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_003_structureConstraintsAndFixedTransferReasonAreInstalled() {
        assertThat(jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE()", String.class))
                .contains(
                        "area",
                        "floor",
                        "room_type",
                        "room_type_care_level",
                        "room",
                        "room_allowed_care_level",
                        "bed_transfer_reason");
        jdbc.update(
                "INSERT INTO area(id,code,name,status) VALUES(903001,'M03-A','Khu thử','INACTIVE'),(903002,'M03-B','Khu thử 2','ACTIVE')");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO area(code,name) VALUES('M03-A','Mã trùng')"))
                .isInstanceOf(DataAccessException.class);
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(903001,903001,'T1','Tầng thử'),(903002,903002,'T1','Tầng thử 2')");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO floor(area_id,code,name) VALUES(903001,'T1','Trùng tầng')"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () -> jdbc.update(
                                "INSERT INTO room_type(name,standard_beds,serves_long_term,serves_short_term) VALUES('Sai số giường',0,true,true)"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () -> jdbc.update(
                                "INSERT INTO room_type(name,standard_beds,serves_long_term,serves_short_term) VALUES('Sai loại',2,false,false)"))
                .isInstanceOf(DataAccessException.class);
        jdbc.update("INSERT INTO room_type(id,name,standard_beds) VALUES(903001,'Loại thử',2)");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO room_type(name,standard_beds) VALUES('Loại thử',2)"))
                .isInstanceOf(DataAccessException.class);
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(903001,903001,903001,'M03-P1','Phòng thử','UNRESTRICTED')");
        assertThatThrownBy(
                        () -> jdbc.update(
                                "INSERT INTO room(floor_id,room_type_id,code,name,gender_policy) VALUES(903002,903001,'M03-P1','Trùng phòng','FEMALE')"))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT medical_safety AND fixed FROM bed_transfer_reason WHERE code='INFECTION_CONTROL'",
                        Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT isolated FROM room WHERE id=903001", Boolean.class))
                .isFalse();
    }
}

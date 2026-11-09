package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.dto.ResidencyDayView;
import com.eldercare.backend.admission.api.events.ResidencyDaysChanged;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T049, T052, T054 (FR-050, FR-052, FR-055, BR-M02-06, BR-M02-07; Q-345, Q-395): vắng tính như ngày lưu trú, giữ giường không hạn;
 * tới ngày CFG-M02-05 báo Quản lý viện đúng một lần, không tạo yêu cầu; Quản lý viện giải phóng giường có lý do, từ đó ngày vắng không
 * tính phí lưu trú. Giường, phân bổ là dữ liệu thật của 003 (giường Giữ chỗ vì vắng, phân bổ Hiệu lực),
 * {@code BedAllocationCommandApi} là bản cài thật.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_07_ReleaseBedIT extends AbsenceTestBase {

    @Autowired
    ResidencyDayApi days;

    @Autowired
    com.eldercare.backend.admission.internal.absence.service.AbsenceTransition absenceTransition;

    /** Giường thật ở trạng thái Giữ chỗ vì người này vắng, kèm phân bổ Hiệu lực (điều kiện của lệnh giải phóng). */
    private long heldBedOf(long residentId) {
        long area = TestUsers.nextId();
        long floor = TestUsers.nextId();
        long type = TestUsers.nextId();
        long room = TestUsers.nextId();
        long bed = TestUsers.nextId();
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
        jdbc.update(
                "INSERT INTO bed(id,room_id,code,state,hold_reason,hold_resident_id) VALUES(?,?,'G1','HELD','ABSENT_RESIDENT',?)",
                bed,
                room,
                residentId);
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source)"
                        + " VALUES(?,?,'ACTIVE',?,'BASIC','CURRENT')",
                residentId,
                bed,
                java.sql.Timestamp.from(at(d0.minusDays(30), 9, 0)));
        return bed;
    }

    private long absent(long residentId) {
        clockAt(d0, 10, 0);
        return grant(residentId, "HOME_VISIT", at(d0.plusDays(60), 10, 0));
    }

    private ResidencyDayView on(List<ResidencyDayView> list, LocalDate date) {
        return list.stream().filter(d -> d.date().equals(date)).findFirst().orElseThrow();
    }

    @Test
    void FR_055_releaseBed_stopsResidencyFeeFromThatDay_andNotifiesBilling() throws Exception {
        long residentId = inResidence();
        long absenceId = absent(residentId);
        long bed = heldBedOf(residentId);
        clockAt(d0.plusDays(5), 9, 0);

        releaseBed(absenceId, "Vắng dài, gia đình đồng ý trả giường")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdBed").value(false));

        assertThat(jdbc.queryForObject(
                        "SELECT status FROM bed_allocation WHERE resident_id = ? AND bed_id = ?",
                        String.class,
                        residentId,
                        bed))
                .as("lệnh thật của 003 đóng phân bổ")
                .isEqualTo("CLOSED");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id = ?", String.class, bed))
                .isNotEqualTo("HELD");
        clockAt(d0.plusDays(6), 12, 0);
        List<ResidencyDayView> list = days.daysBetween(residentId, d0, d0.plusDays(6));
        assertThat(on(list, d0.plusDays(4)).bedReleased()).isFalse();
        assertThat(on(list, d0.plusDays(5)).bedReleased()).isTrue();
        assertThat(on(list, d0.plusDays(6)).bedReleased()).isTrue();
        assertThat(events.stream(ResidencyDaysChanged.class)
                        .filter(e -> e.residentId() == residentId && "BED_RELEASED".equals(e.cause())))
                .hasSize(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M02.ABSENCE' AND entity_id = ?"
                                + " AND action = 'M02_ABSENCE_BED_RELEASED' AND reason IS NOT NULL",
                        Integer.class,
                        String.valueOf(absenceId)))
                .isEqualTo(1);
    }
}

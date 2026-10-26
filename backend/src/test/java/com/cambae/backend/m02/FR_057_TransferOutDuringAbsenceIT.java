package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.events.AbsenceClosed;
import com.cambae.backend.m02.api.events.AbsenceOpened;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T049 (FR-057, FR-059): người đang Tạm vắng chuyển viện thì lượt cũ đóng tại thời điểm chuyển, lượt Bệnh viện mở từ cùng
 * thời điểm trỏ {@code previous_absence_id}, số thứ tự ngày đếm lại từ 1 và ngày chuyển thuộc lượt mới; giường đang giữ thì
 * giữ liên tục; Ghi nhận trở về đóng lượt Bệnh viện.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_057_TransferOutDuringAbsenceIT extends AbsenceTestBase {

    @Test
    void FR_057_transferOutWhileAbsent_closesTheOldAbsence_andOpensAHospitalOneFromTheSameInstant() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        long home = grant(residentId, "HOME_VISIT", at(d0.plusDays(9), 10, 0));
        clockAt(d0.plusDays(3), 14, 0);

        long hospital = transferOut(residentId);

        Map<String, Object> old = jdbc.queryForMap("SELECT * FROM absence WHERE id = ?", home);
        Map<String, Object> next = jdbc.queryForMap("SELECT * FROM absence WHERE id = ?", hospital);
        assertThat(old.get("status")).isEqualTo("CLOSED");
        assertThat(next.get("status")).isEqualTo("OPEN");
        assertThat(next.get("absence_type")).isEqualTo("HOSPITAL");
        assertThat(((Number) next.get("previous_absence_id")).longValue()).isEqualTo(home);
        assertThat(next.get("departed_at")).as("cùng thời điểm").isEqualTo(old.get("returned_at"));
        assertThat(next.get("initial_bed_hold"))
                .as("giường đang giữ thì giữ liên tục")
                .isEqualTo(true);
        assertThat(currentStatus(residentId)).isEqualTo(ResidentStatus.HOSPITALIZED);
        assertThat(events.stream(AbsenceClosed.class).filter(e -> e.absenceId() == home))
                .hasSize(1);
        assertThat(events.stream(AbsenceOpened.class).filter(e -> e.absenceId() == hospital))
                .hasSize(1);

        List<Map<String, Object>> oldDays = days(home);
        assertThat(oldDays).as("ngày chuyển viện không tính cho lượt trước").hasSize(3);
        assertThat(oldDays.getLast().get("date")).isEqualTo(Date.valueOf(d0.plusDays(2)));
        List<Map<String, Object>> newDays = days(hospital);
        assertThat(newDays).hasSize(1);
        assertThat(newDays.getFirst().get("sequence_no")).as("đếm lại từ 1").isEqualTo(1);
        assertThat(newDays.getFirst().get("date"))
                .as("ngày chuyển thuộc lượt mới")
                .isEqualTo(Date.valueOf(d0.plusDays(3)));
    }

    @Test
    void FR_057_theHospitalDaysKeepCountingFromOne_overTheFollowingDays() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        grant(residentId, "FAMILY_OUTING", at(d0.plusDays(9), 10, 0));
        clockAt(d0.plusDays(2), 8, 0);
        long hospital = transferOut(residentId);

        runJobOn(d0.plusDays(9));

        List<Map<String, Object>> days = days(hospital);
        assertThat(days).hasSize(8);
        assertThat(days.get(0).get("sequence_no")).isEqualTo(1);
        assertThat(days.get(7).get("sequence_no")).isEqualTo(8);
        assertThat(days.get(7).get("coefficient")).isEqualTo(new java.math.BigDecimal("70.00"));
    }

    @Test
    void FR_059_returnFromTheHospital_closesTheHospitalAbsence_andWarnsWhenTheBedWasReleased() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 9, 0);
        long hospital = transferOut(residentId);
        clockAt(d0.plusDays(1), 16, 0);

        recordReturn(residentId, null);

        assertThat(jdbc.queryForObject("SELECT status FROM absence WHERE id = ?", String.class, hospital))
                .isEqualTo("CLOSED");
        assertThat(days(hospital)).hasSize(2);
        assertThat(currentStatus(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM absence WHERE resident_id = ? AND status = 'OPEN'",
                        Integer.class,
                        residentId))
                .isZero();
    }

    @Test
    void FR_048_aSecondTemporaryAbsence_isRefusedWhileOneIsOpen() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        grant(residentId, "HOME_VISIT", at(d0.plusDays(2), 10, 0));

        var failure = failure(() -> grant(residentId, "FAMILY_OUTING", at(d0.plusDays(3), 10, 0)));

        assertThat(failure).isNotNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM absence WHERE resident_id = ?", Integer.class, residentId))
                .isEqualTo(1);
    }
}

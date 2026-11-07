package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.administration.account.internal.auth.service.SessionAuthentication;
import com.eldercare.backend.incident.internal.critical.service.CriticalFlagStatusParticipant;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * DBR-34 và bảng trạng thái dấu nguy kịch (FR-050a): tối đa một dấu đang mở mỗi người, mỗi dấu đúng một cảnh báo; gỡ
 * dấu không đóng cảnh báo; ghi lại sau khi gỡ tạo dấu và cảnh báo mới; tự gỡ khi chuyển viện hoặc trạng thái cuối.
 */
class DBR_34_CriticalFlagIT extends CriticalCareTestBase {

    @Autowired
    CriticalFlagStatusParticipant participant;

    @Test
    void DBR_34_oneOpenFlagPerResident_oneAlertPerFlag() throws Exception {
        var created = flag(doctor);
        long flagId = created.id();
        long alertId = created.alertId();

        assertThat(flagStatus(flagId)).isEqualTo("OPEN");
        var alert = jdbc.queryForMap(
                "SELECT type, level, status, critical_flag_id, resident_id FROM alert WHERE id = ?", alertId);
        assertThat(alert.get("type")).isEqualTo("CRITICAL_CONDITION");
        assertThat(alert.get("level")).isEqualTo("CRITICAL");
        assertThat(alert.get("critical_flag_id")).isEqualTo(flagId);
        assertThat(alert.get("resident_id")).isEqualTo(resident);

        post(doctor, FLAGS, resident, "{\"type\":\"OFFICIAL\",\"assessment\":\"Lần hai\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CRITICAL_FLAG_EXISTS"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM alert WHERE critical_flag_id IS NOT NULL AND resident_id = ?",
                        Integer.class,
                        resident))
                .isEqualTo(1);
        // Cơ sở dữ liệu cũng giữ DBR-34: dòng thứ hai cùng khóa mở bị từ chối.
        assertThatThrownBy(() -> jdbc.update("""
                        INSERT INTO simple_record (kind, module, resident_id, status, content, unique_key, created_at,
                          created_by)
                        SELECT kind, module, resident_id, status, content, unique_key, created_at, created_by
                        FROM simple_record WHERE id = ?
                        """, flagId)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_050a_removeKeepsAlertOpen_andNewFlagCreatesNewAlert() throws Exception {
        var first = flag(doctor);
        long flagId = first.id();

        post(nurse, REMOVE, flagId, "{\"reason\":\"Ổn định\"}").andExpect(status().isForbidden());
        post(doctor, REMOVE, flagId, "{}").andExpect(status().isUnprocessableContent());
        post(doctor, REMOVE, flagId, "{\"reason\":\"Tình trạng ổn định\"}").andExpect(status().isOk());

        assertThat(flagStatus(flagId)).isEqualTo("REMOVED");
        assertThat(jdbc.queryForObject("SELECT status FROM alert WHERE id = ?", String.class, first.alertId()))
                .isEqualTo("NEW");
        post(doctor, REMOVE, flagId, "{\"reason\":\"Lần hai\"}").andExpect(status().isConflict());

        var second = flag(doctor);
        assertThat(second.id()).isNotEqualTo(flagId);
        assertThat(second.alertId()).isNotEqualTo(first.alertId());
    }

    @Test
    void FR_050a_onlyResidentsInResidenceAbsentOrOffsiteCanBeFlagged() throws Exception {
        when(residents.status(resident)).thenReturn(ResidentStatus.HOSPITALIZED);
        post(doctor, FLAGS, resident, "{\"type\":\"OFFICIAL\",\"assessment\":\"Nặng\"}")
                .andExpect(status().isConflict());

        when(residents.status(resident)).thenReturn(ResidentStatus.OFF_SITE_ACTIVITY);
        flag(doctor);
    }

    @Test
    void FR_050a_transferOutRemovesFlagBySystem() throws Exception {
        long flagId = flag(doctor).id();

        var result = new TransitionResult(
                resident,
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.HOSPITALIZED,
                StatusCommand.TRANSFER_OUT,
                1L,
                time.now(),
                null);
        assertThat(participant.interestedCommands()).contains(StatusCommand.TRANSFER_OUT);
        // Lệnh đổi trạng thái của 001 chạy trong phiên người ra lệnh; dựng phiên đó cho lần gọi trực tiếp.
        var u = doctor.user();
        SecurityContextHolder.getContext()
                .setAuthentication(new SessionAuthentication(new ActiveSession(
                        u.accountId(),
                        u.type(),
                        u.staffId(),
                        u.familyMemberId(),
                        u.roles(),
                        "test",
                        u.channel(),
                        Optional.empty())));
        try {
            participant.applyEffects(result);
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertThat(flagStatus(flagId)).isEqualTo("REMOVED");
        var content = jdbc.queryForMap(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(content, '$.removalReason')) AS reason,"
                        + " JSON_EXTRACT(content, '$.removedById') AS remover FROM simple_record WHERE id = ?",
                flagId);
        assertThat(content.get("reason")).isEqualTo("Tự gỡ theo trạng thái");
        assertThat(content.get("remover")).isIn(null, "null");
    }
}

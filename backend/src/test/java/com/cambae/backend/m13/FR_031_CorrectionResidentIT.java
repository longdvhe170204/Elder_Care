package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m15.correction.api.spi.CorrectableRecordProvider;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/**
 * FR-031 (000 contracts/module-interfaces.md mục 1.6): hai loại đính chính của M13 cho biết người cao tuổi của bản gốc
 * để dòng nhật ký đính chính tra được theo người cao tuổi: kết quả gọi theo lượt nhắc gọi, kết quả liên hệ trực tiếp
 * theo yêu cầu thông báo của dòng liên hệ.
 */
class FR_031_CorrectionResidentIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    List<CorrectableRecordProvider> providers;

    private CorrectableRecordProvider provider(String type) {
        return providers.stream()
                .filter(p -> p.recordType().equals(type))
                .findFirst()
                .orElseThrow();
    }

    private long insert(String sql, Object... args) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    @Test
    void FR_031_callResult_residentFromCallReminder() {
        long resident = TestUsers.nextId();
        long reminder = insert(
                "INSERT INTO call_reminder(callee_type,callee_id,resident_id,creation_reason,created_at)"
                        + " VALUES ('FAMILY_MEMBER',?,?,'UNACKNOWLEDGED',UTC_TIMESTAMP(6))",
                TestUsers.nextId(),
                resident);
        long result = insert(
                "INSERT INTO call_result(call_reminder_id,called_by,called_at,result,reason,recorded_at)"
                        + " VALUES (?,?,UTC_TIMESTAMP(6),'NOT_REACHED','NO_ANSWER',UTC_TIMESTAMP(6))",
                reminder,
                TestUsers.nextId());

        assertThat(provider("M13.CALL_RESULT").residentId(result)).contains(resident);
    }

    @Test
    void FR_031_directContactResult_residentFromNotificationRequest() {
        long resident = TestUsers.nextId();
        long request = insert(
                "INSERT INTO notification_request(level,resident_id,received_at,intake_result)"
                        + " VALUES ('URGENT',?,UTC_TIMESTAMP(6),'VALID')",
                resident);
        long notification = insert(
                "INSERT INTO notification(request_id,recipient_type,recipient_id,basis,level,created_at)"
                        + " VALUES (?,'FAMILY_MEMBER',?,JSON_OBJECT(),'URGENT',UTC_TIMESTAMP(6))",
                request,
                TestUsers.nextId());
        long contact = insert(
                "INSERT INTO direct_contact(notification_id,created_at) VALUES (?,UTC_TIMESTAMP(6))", notification);
        long result = insert(
                "INSERT INTO direct_contact_result(direct_contact_id,contacted_by,occurred_at,result,recorded_at)"
                        + " VALUES (?,?,UTC_TIMESTAMP(6),'CONTACTED',UTC_TIMESTAMP(6))",
                contact,
                TestUsers.nextId());

        assertThat(provider("M13.DIRECT_CONTACT_RESULT").residentId(result)).contains(resident);
    }
}

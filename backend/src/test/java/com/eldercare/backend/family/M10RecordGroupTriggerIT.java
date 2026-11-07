package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.family.internal.feedback.domain.FeedbackItem;
import com.eldercare.backend.family.internal.feedback.domain.FeedbackOpinion;
import com.eldercare.backend.family.internal.pickup.domain.PickupBlock;
import com.eldercare.backend.family.internal.pickup.domain.PickupOutcome;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRegistrationForm;
import com.eldercare.backend.family.internal.stay.domain.FamilyStay;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * 012 T037 (mở rộng {@code RecordGroupTriggerIT} của 000 cho bảng nhóm 3 của 012; data-model mục 0.1, constitution XII):
 * bảng riêng {@code pickup_record} có trigger chặn sửa, xóa; sau gộp R-15 các bản ghi nhóm 3 khác của 012 (sự kiện bản
 * ghi đón, lần chặn đón, gia hạn ở lại, mục và ý kiến phản hồi, phiếu đăng ký) là dòng {@code record_event} đã xác nhận,
 * trigger của 000 chặn sửa, xóa, kể cả tài khoản quản trị.
 */
class M10RecordGroupTriggerIT extends MySqlIntegrationTestBase {

    /** Loại {@code record_event} nhóm 3 của 012; lệnh ghi đặt {@code confirmed_at} (phiếu đăng ký: xác nhận cùng lệnh). */
    static final List<String> RECORD_EVENT_KINDS = List.of(
            PickupOutcome.KIND.code(),
            PickupBlock.KIND.code(),
            FamilyStay.Extension.KIND.code(),
            FeedbackItem.KIND.code(),
            FeedbackOpinion.KIND.code(),
            FamilyRegistrationForm.KIND.code());

    @Autowired
    JdbcTemplate jdbc;

    private long pickupRecord() throws SQLException {
        try (Connection c = adminConnection();
                Statement s = c.createStatement()) {
            s.executeUpdate(
                    "INSERT INTO pickup_record (resident_id, type, basis, picker_name, id_document_number, at,"
                            + " handover_staff_id, recorded_by, expires_at, created_at) VALUES ("
                            + TestUsers.nextId()
                            + ", 'TEMPORARY_ABSENCE', 'PICKUP_LIST', 'Hồ Văn A', '012345678901', UTC_TIMESTAMP(6), 1, 1,"
                            + " UTC_TIMESTAMP(6) + INTERVAL 1 HOUR, UTC_TIMESTAMP(6))",
                    Statement.RETURN_GENERATED_KEYS);
            try (ResultSet keys = s.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private long confirmedEvent(String kind) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(
                            "INSERT INTO record_event (entity_type, entity_id, module, kind, content, actor_id, by_system,"
                                    + " occurred_at, confirmed_at) VALUES ('M01.RESIDENT', ?, 'M10', ?, '{}', 1, FALSE,"
                                    + " UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))",
                            new String[] {"id"});
                    ps.setLong(1, TestUsers.nextId());
                    ps.setString(2, kind);
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    @Test
    void FR_026_pickupRecord_appAccountCannotUpdateOrDelete() throws SQLException {
        long id = pickupRecord();

        assertThatThrownBy(() -> jdbc.update("UPDATE pickup_record SET picker_name = 'Khác' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM pickup_record WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
    }
}

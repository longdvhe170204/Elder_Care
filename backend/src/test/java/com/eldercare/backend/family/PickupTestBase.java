package com.eldercare.backend.family;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.family.internal.right.domain.SelfReturnFlag;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Nền test quy trình đón (012 US2; FR-021 → FR-028, BR-M10-03): người cao tuổi A có R (người đại diện, liên hệ chính,
 * được phép đón, CCCD {@link #R_DOC}), M (được phép đón, CCCD {@link #M_DOC}), X (quan hệ không có quyền đón). Ghi đón
 * bằng phiên Hành chính (phạm vi toàn viện, quyền mọi lệnh nguồn) trừ khi test cần vai trò khác.
 */
abstract class PickupTestBase extends FamilyCommandTestBase {

    static final String R_DOC = "079100000001";
    static final String M_DOC = "079100000002";
    static final String X_DOC = "079100000003";

    @Autowired
    TestClockControl clock;

    @Autowired
    SimpleRecordApi records;

    long a;
    long rMember;
    long mMember;
    long xMember;
    long r;
    long m;
    long x;
    long handoverStaff;
    private final java.util.Map<Long, String> familySessions = new java.util.HashMap<>();

    /** Phiên cổng của người thân, tạo tài khoản một lần mỗi test. */
    String family(long familyMemberId) {
        return familySessions.computeIfAbsent(familyMemberId, this::familySession);
    }

    @BeforeEach
    void pickupFamily() {
        a = resident(ResidentStatus.IN_RESIDENCE);
        rMember = fixtures.familyMember("Trần R", "0903000001", "CCCD", R_DOC);
        mMember = fixtures.familyMember("Lê M", "0903000002", "CCCD", M_DOC);
        xMember = fixtures.familyMember("Phạm X", "0903000003", "CCCD", X_DOC);
        r = fixtures.relate(rMember, a, 1, true, true, FamilyRight.PICKUP_ALLOWED);
        m = fixtures.relate(mMember, a, 2, false, false, FamilyRight.PICKUP_ALLOWED);
        x = fixtures.relate(xMember, a, 3, false, false);
        handoverStaff = users.staff(Role.CAREGIVER).staffId();
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    ResultActions pickup(String authorization, String type, Long familyMemberId, String documentNumber)
            throws Exception {
        String member = familyMemberId == null ? "" : ", \"familyMemberId\": " + familyMemberId;
        return postAs(authorization, "/api/v1/residents/" + a + "/pickups", body(type, member, documentNumber));
    }

    ResultActions pickupWithException(String authorization, String type, long exceptionId, String documentNumber)
            throws Exception {
        return postAs(
                authorization,
                "/api/v1/residents/" + a + "/pickups",
                body(type, ", \"exceptionId\": " + exceptionId, documentNumber));
    }

    private String body(String type, String picker, String documentNumber) {
        String document = documentNumber == null
                ? ""
                : ", \"presentedIdDocument\": {\"type\": \"CCCD\", \"number\": \"" + documentNumber + "\"}";
        return "{\"type\": \"" + type + "\", \"handoverStaffId\": " + handoverStaff + picker + document + "}";
    }

    /** Yêu cầu ngoại lệ đón do {@code authorization} lập cho X (FR-024). */
    ResultActions createException(String authorization, String fullName, String documentNumber) throws Exception {
        return postAs(
                authorization,
                "/api/v1/residents/" + a + "/pickup-exceptions",
                "{\"fullName\": \"" + fullName + "\", \"idDocumentNumber\": \"" + documentNumber
                        + "\", \"relationship\": \"Hàng xóm\", \"reason\": \"Gia đình nhờ đón hộ\"}");
    }

    ResultActions exceptionCommand(String authorization, long exceptionId, String command, String body)
            throws Exception {
        return postAs(authorization, "/api/v1/pickup-exceptions/" + exceptionId + "/" + command, body);
    }

    /** Số lần chặn đón đã ghi của A ({@code lan_chan_don}; R-15: {@code record_event} gắn hồ sơ). */
    int blocks() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM record_event WHERE kind = 'M10.PICKUP_BLOCK' AND entity_type = 'M01.RESIDENT'"
                        + " AND entity_id = ?",
                Integer.class,
                a);
    }

    String lastBlockReason() {
        return jdbc.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(content, '$.reason')) FROM record_event WHERE kind = 'M10.PICKUP_BLOCK'"
                        + " AND entity_type = 'M01.RESIDENT' AND entity_id = ? ORDER BY id DESC LIMIT 1",
                String.class,
                a);
    }

    int pickupRecords() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM pickup_record WHERE resident_id = ?", Integer.class, a);
    }

    String exceptionStatus(long exceptionId) {
        return jdbc.queryForObject("SELECT status FROM simple_record WHERE id = ?", String.class, exceptionId);
    }

    /** Bật hoặc tắt dấu "được tự về" của A như lệnh yêu cầu (US3). */
    void selfReturn(boolean enabled) {
        tx.executeWithoutResult(s -> records.create(
                SelfReturnFlag.KIND,
                new NewSimpleRecord<>(
                        a,
                        new EntityRef("M01.RESIDENT", a),
                        enabled ? "ON" : "OFF",
                        new SelfReturnFlag(enabled, 0, time.now()),
                        null,
                        null,
                        SelfReturnFlag.key(a),
                        1,
                        false)));
    }

    /** Đặt giờ hệ thống; phiên tạo sau lời gọi này để không hết hạn theo giờ mới. */
    void at(Instant instant) {
        clock.setTo(instant);
    }
}

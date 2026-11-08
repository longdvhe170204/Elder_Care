package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetCommand;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetCommandPort;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus;
import com.eldercare.backend.support.fake.roombedallocation.FakeBedAssets;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * T038, T042 (FR-013a; module-interfaces 000 mục 1.5 {@code failApplication}): lệnh tài sản đưa giường đích sang bảo trì
 * hủy phân bổ tương lai của chuyển theo lịch và làm yêu cầu Đã duyệt thành Áp dụng không thành, nhật ký
 * {@code REQUEST_APPLY_FAILED} có căn cứ, cùng transaction; yêu cầu không còn Đã duyệt thì cả lệnh tài sản rollback.
 */
class FR_013a_ScheduledTransferApplyFailedIT extends StructureTestSupport {
    @Autowired
    BedAssetCommandPort port;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    FakeAllocationInfectionStatus infection;

    @Autowired
    FakeBedAssets assets;

    long resident, current, target, request, future;
    final Basis basis = new Basis("FR-013a", null, 991L, null);

    @BeforeEach
    void scheduledFixture() {
        infection.reset();
        assets.reset();
        resident = TestUsers.nextId();
        current = TestUsers.nextId();
        target = TestUsers.nextId();
        future = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,"
                        + "created_at,created_by) VALUES(?,'[Họ tên NCT]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source)"
                        + " VALUES(?,?,?,'ACTIVE','2026-01-01','BASIC','CURRENT')",
                current,
                resident,
                bed);
        jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G-HL')", target, room);
    }

    private void scheduled(String requestStatus) {
        var keys = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO approval_request(type,status,content,entity_type,entity_id,resident_id,"
                                    + "requester_id,created_at,reason,requested_effective_date,actual_effective_date)"
                                    + " VALUES('M02.RESIDENCY_CHANGE',?,'{}','M01.RESIDENT',?,?,?,UTC_TIMESTAMP(6),"
                                    + "'Chuyển phòng',?,?)",
                            java.sql.Statement.RETURN_GENERATED_KEYS);
                    s.setString(1, requestStatus);
                    s.setString(2, Long.toString(resident));
                    s.setLong(3, resident);
                    s.setLong(4, user.accountId());
                    s.setObject(5, time.today().plusDays(5));
                    s.setObject(6, time.today().plusDays(5));
                    return s;
                },
                keys);
        request = keys.getKey().longValue();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source,"
                        + "basis_type,basis_id,previous_allocation_id) VALUES(?,?,?,'FUTURE',?,'BASIC','CURRENT',"
                        + "'APPROVAL_REQUEST',?,?)",
                future,
                resident,
                target,
                Timestamp.from(time.now().plus(java.time.Duration.ofDays(5))),
                request,
                current);
    }

    private void maintenance() {
        tx.executeWithoutResult(s -> port.applyAssetCommand(target, BedAssetCommand.MAINTENANCE, basis));
    }

    private String allocation(long id) {
        return jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, id);
    }

    private String requestStatus() {
        return jdbc.queryForObject("SELECT status FROM approval_request WHERE id=?", String.class, request);
    }

    @Test
    void FR_013a_damagedBedFailsTheScheduledTransferRequestWithBasis() {
        scheduled("APPROVED");

        maintenance();

        assertThat(allocation(future)).isEqualTo("CANCELLED");
        assertThat(allocation(current)).isEqualTo("ACTIVE");
        assertThat(requestStatus()).isEqualTo("APPLY_FAILED");
        assertThat(jdbc.queryForObject("SELECT closing_reason FROM approval_request WHERE id=?", String.class, request))
                .contains("FR-013a");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action='REQUEST_APPLY_FAILED' AND entity_id=?"
                                + " AND JSON_EXTRACT(basis,'$.ruleCode')='FR-013a'",
                        Integer.class,
                        Long.toString(request)))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, target))
                .isEqualTo("UNDER_MAINTENANCE");
    }

    @Test
    void FR_013a_requestNoLongerApprovedRollsBackTheAssetCommand() {
        scheduled("APPLIED");

        assertThatThrownBy(this::maintenance)
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));

        assertThat(allocation(future)).isEqualTo("FUTURE");
        assertThat(requestStatus()).isEqualTo("APPLIED");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, target))
                .isEqualTo("EMPTY");
    }
}

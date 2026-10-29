package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.TestUsers;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/** T020/T022: kiểm ràng buộc phân bổ bằng SQL thật, không đi qua kiểm tra service. */
class DBR_09_AllocationSchemaIT extends StructureTestSupport {
    private long allocation(long resident, String status, String start, String end) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,expected_end,checked_care_level,care_level_source) VALUES(?,?,?,?,?,?,'BASIC','CONTRACT')",
                id,
                resident,
                bed,
                status,
                start,
                end);
        return id;
    }

    @Test
    void DBR_09_schemaProvidesAllocationPositionReminderAndCorrectedView() {
        assertThat(jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE()", String.class))
                .contains("bed_allocation", "resident_position", "allocation_reminder", "v_current_allocation");
    }

    @Test
    void DBR_09_correctedViewRetainsGeneratedAllocationColumns() {
        long resident = TestUsers.nextId();
        long id = allocation(resident, "ACTIVE", "2026-11-01 00:00:00", "2026-11-03 00:00:00");
        var row = jdbc.queryForMap("SELECT * FROM v_current_allocation WHERE id=?", id);
        assertThat(row).containsKeys("effective_end", "active_bed_id", "active_resident_id", "future_resident_id");
        assertThat(((Number) row.get("active_bed_id")).longValue()).isEqualTo(bed);
        assertThat(((Number) row.get("active_resident_id")).longValue()).isEqualTo(resident);
        assertThat(row.get("future_resident_id")).isNull();
        assertThat(row.get("effective_end").toString()).startsWith("2026-11-03");
    }

    @Test
    void DBR_09_halfOpenIntervalsPermitAdjacencyButRejectOverlapOnInsertAndUpdate() {
        allocation(TestUsers.nextId(), "FUTURE", "2026-11-01 00:00:00", "2026-11-02 00:00:00");
        long next = allocation(TestUsers.nextId(), "FUTURE", "2026-11-02 00:00:00", "2026-11-03 00:00:00");
        assertThatThrownBy(() -> allocation(TestUsers.nextId(), "FUTURE", "2026-11-01 12:00:00", null))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () -> jdbc.update("UPDATE bed_allocation SET start_at='2026-11-01 23:59:59' WHERE id=?", next))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void DBR_09_residentFutureKeyRejectsTwoNonOverlappingReservationsForOneResident() {
        long resident = TestUsers.nextId();
        allocation(resident, "FUTURE", "2026-11-01 00:00:00", "2026-11-02 00:00:00");
        assertThatThrownBy(() -> allocation(resident, "FUTURE", "2026-11-03 00:00:00", null))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void DBR_09_residentActiveKeyRejectsOneResidentInTwoDifferentBeds() {
        long otherBed = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G2')", otherBed, room);
        long activeResident = TestUsers.nextId();
        allocation(activeResident, "ACTIVE", "2026-10-01 00:00:00", null);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'ACTIVE','2026-10-02','BASIC','CURRENT')",
                        activeResident,
                        otherBed))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void DBR_09_activeBedKeyRejectsEvenNonOverlappingActiveIntervals() {
        allocation(TestUsers.nextId(), "ACTIVE", "2026-11-01 00:00:00", "2026-11-02 00:00:00");
        assertThatThrownBy(() -> allocation(TestUsers.nextId(), "ACTIVE", "2026-11-02 00:00:00", null))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void DBR_09_effectiveEndUsesActualThenExpectedThenUnboundedEnd() {
        long id = allocation(TestUsers.nextId(), "ACTIVE", "2026-11-01 00:00:00", "2026-11-03 00:00:00");
        assertThat(jdbc.queryForObject(
                        "SELECT CAST(effective_end AS CHAR) FROM bed_allocation WHERE id=?", String.class, id))
                .startsWith("2026-11-03 00:00:00");
        jdbc.update("UPDATE bed_allocation SET status='CLOSED',end_at='2026-11-02' WHERE id=?", id);
        assertThat(jdbc.queryForObject(
                        "SELECT CAST(effective_end AS CHAR) FROM bed_allocation WHERE id=?", String.class, id))
                .startsWith("2026-11-02 00:00:00");
        long unbounded = allocation(TestUsers.nextId(), "FUTURE", "2026-11-02 00:00:00", null);
        assertThat(jdbc.queryForObject(
                        "SELECT CAST(effective_end AS CHAR) FROM bed_allocation WHERE id=?", String.class, unbounded))
                .startsWith("9999-12-31 00:00:00");
    }

    @Test
    void FR_026_cancelledRowsDoNotOccupyBedButAreImmutableAndCannotBeDeleted() {
        long id = allocation(TestUsers.nextId(), "FUTURE", "2026-11-01 00:00:00", null);
        jdbc.update(
                "UPDATE bed_allocation SET status='CANCELLED',cancellation_reason='Hủy theo yêu cầu' WHERE id=?", id);
        allocation(TestUsers.nextId(), "FUTURE", "2026-11-01 00:00:00", null);
        assertThatThrownBy(() -> jdbc.update("UPDATE bed_allocation SET cancellation_reason='Ghi đè' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM bed_allocation WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_026_triggersRejectEvenAdminChangesButPermitRetentionDelete() throws Exception {
        long id = allocation(TestUsers.nextId(), "FUTURE", "2026-11-01 00:00:00", null);
        jdbc.update("UPDATE bed_allocation SET status='CANCELLED',cancellation_reason='Hủy trong test' WHERE id=?", id);
        try (var admin = adminConnection();
                var command = admin.createStatement()) {
            assertThatThrownBy(() -> command.executeUpdate(
                            "UPDATE bed_allocation SET cancellation_reason='Ghi đè' WHERE id=" + id))
                    .isInstanceOfSatisfying(
                            SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("45000"));
            assertThatThrownBy(() -> command.executeUpdate("DELETE FROM bed_allocation WHERE id=" + id))
                    .isInstanceOfSatisfying(
                            SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("45000"));
        }
        try (var retention = retentionConnection();
                var command = retention.createStatement()) {
            assertThat(command.executeUpdate("DELETE FROM bed_allocation WHERE id=" + id))
                    .isEqualTo(1);
        }
    }

    @Test
    void DBR_09_cancelledInsertDoesNotBlockIntervalAndGeneratedKeysCannotBeAssigned() {
        long id = allocation(TestUsers.nextId(), "ACTIVE", "2026-11-01 00:00:00", null);
        assertThat(jdbc.update(
                        "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,cancellation_reason,checked_care_level,care_level_source) VALUES(?,?,'CANCELLED','2026-11-01','Hủy','BASIC','CONTRACT')",
                        TestUsers.nextId(),
                        bed))
                .isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("UPDATE bed_allocation SET active_bed_id=1 WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_017_checksRejectInvalidEndCancellationAndDoctorWithoutTransferReason() {
        long id = allocation(TestUsers.nextId(), "FUTURE", "2026-11-01 00:00:00", null);
        for (String assignment : new String[] {
            "status='CLOSED'",
            "status='CLOSED',end_at='2026-11-01'",
            "end_at='2026-11-02'",
            "expected_end='2026-11-01'",
            "status='CANCELLED'",
            "cancellation_reason='Sai trạng thái'",
            "prescribing_doctor_id=1"
        }) {
            assertThatThrownBy(() -> jdbc.update("UPDATE bed_allocation SET " + assignment + " WHERE id=?", id))
                    .as(assignment)
                    .isInstanceOf(DataAccessException.class);
        }
        jdbc.update("UPDATE bed_allocation SET status='CLOSED',end_at='2026-11-02' WHERE id=?", id);
        assertThatThrownBy(() -> jdbc.update("UPDATE bed_allocation SET end_at='2026-11-03' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_026_correctedViewAppliesLatestAmendmentIncludingNullEndAndExcludesVoidedRows() {
        long id = allocation(TestUsers.nextId(), "ACTIVE", "2026-11-01 00:00:00", null);
        jdbc.update("UPDATE bed_allocation SET status='CLOSED',end_at='2026-11-03' WHERE id=?", id);
        long correctedBed = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G2')", correctedBed, room);
        String insertCorrection =
                "INSERT INTO correction(record_type,original_record_id,type,corrected_content,reason,actor_id,creation_method,recorded_at,sequence_no) VALUES('M03.BED_ALLOCATION',?,'AMEND_CONTENT',?,'Đính chính trong test',1,'A','2026-11-04',?)";
        jdbc.update(
                insertCorrection,
                id,
                "{\"bedId\":" + correctedBed
                        + ",\"startAt\":\"2026-11-02T00:00:00Z\",\"endAt\":\"2026-11-03T00:00:00Z\"}",
                1);
        jdbc.update(
                insertCorrection,
                id,
                "{\"bedId\":" + correctedBed + ",\"startAt\":\"2026-11-02T07:00:00+07:00\",\"endAt\":null}",
                2);
        var row = jdbc.queryForMap("SELECT bed_id,start_at,end_at,corrected FROM v_current_allocation WHERE id=?", id);
        assertThat(((Number) row.get("bed_id")).longValue()).isEqualTo(correctedBed);
        assertThat(row.get("start_at").toString()).startsWith("2026-11-02");
        assertThat(jdbc.queryForObject(
                        "SELECT CAST(start_at AS CHAR) FROM v_current_allocation WHERE id=?", String.class, id))
                .startsWith("2026-11-02 00:00:00");
        assertThat(row.get("end_at")).isNull();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM v_current_allocation WHERE id=? AND corrected=TRUE", Integer.class, id))
                .isEqualTo(1);
        jdbc.update(
                "INSERT INTO correction(record_type,original_record_id,type,reason,actor_id,creation_method,recorded_at,sequence_no) VALUES('M03.BED_ALLOCATION',?,'VOID','Hủy ghi nhận trong test',1,'A','2026-11-04',3)",
                id);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM v_current_allocation WHERE id=?", Integer.class, id))
                .isZero();
    }
}

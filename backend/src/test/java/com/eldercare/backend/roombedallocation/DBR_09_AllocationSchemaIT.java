package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.TestUsers;
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
}

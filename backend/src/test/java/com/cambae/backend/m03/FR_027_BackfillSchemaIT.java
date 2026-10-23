package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m03.internal.allocation.persistence.M03GovernanceStore;
import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.support.TestUsers;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T034 phần schema: chỉ mục nhập bù bất biến và được dọn cùng phân bổ nguồn. */
class FR_027_BackfillSchemaIT extends StructureTestSupport {
    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    RetentionDatabase retention;

    @Autowired
    M03GovernanceStore governance;

    private long approval() {
        var key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> connection.prepareStatement(
                        "INSERT INTO approval_request(type,status,content,requester_id,created_at,reason,requested_effective_date) VALUES('M03.BED_ALLOCATION_BACKFILL','DRAFT','{}',1,'2026-10-24','Kiểm nhập bù','2026-10-24')",
                        Statement.RETURN_GENERATED_KEYS),
                key);
        return key.getKey().longValue();
    }

    private long allocation(long resident) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source) VALUES(?,?,?,'CLOSED','2026-10-01','2026-10-02','BASIC','CURRENT')",
                id,
                resident,
                bed);
        return id;
    }

    private void insert(long request, long resident, long allocation, long newBed) {
        jdbc.update(
                "INSERT INTO bed_backfill_request(approval_request_id,resident_id,current_allocation_id,new_bed_id,transfer_at) VALUES(?,?,?,?,'2026-10-01 14:00:00')",
                request,
                resident,
                allocation,
                newBed);
    }

    @Test
    void FR_027_backfillIndexExistsWithoutReadingApprovalJson() {
        assertThat(jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE()", String.class))
                .contains("bed_backfill_request");
        long resident = TestUsers.nextId(), request = approval(), allocation = allocation(resident);
        insert(request, resident, allocation, bed);
        var row = jdbc.queryForMap(
                "SELECT resident_id,current_allocation_id,new_bed_id,transfer_at FROM bed_backfill_request WHERE approval_request_id=?",
                request);
        assertThat(((Number) row.get("resident_id")).longValue()).isEqualTo(resident);
        assertThat(((Number) row.get("current_allocation_id")).longValue()).isEqualTo(allocation);
        assertThat(((Number) row.get("new_bed_id")).longValue()).isEqualTo(bed);
        assertThat(row.get("transfer_at").toString()).startsWith("2026-10-01");
    }

    @Test
    void FR_027_oneIndexPerRequestAndReferencesMustExist() {
        long resident = TestUsers.nextId(), request = approval(), allocation = allocation(resident);
        insert(request, resident, allocation, bed);
        assertThatThrownBy(() -> insert(request, resident, allocation, bed)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insert(Long.MAX_VALUE, resident, allocation, bed))
                .isInstanceOf(DataAccessException.class);
        long another = approval();
        assertThatThrownBy(() -> insert(another, resident, Long.MAX_VALUE, bed))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insert(another, resident, allocation, Long.MAX_VALUE))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_027_adminCannotRewriteOrDeleteButRetentionCanRemoveIndex() throws Exception {
        long resident = TestUsers.nextId(), request = approval(), allocation = allocation(resident);
        insert(request, resident, allocation, bed);
        try (var admin = adminConnection();
                var command = admin.createStatement()) {
            assertThatThrownBy(() -> command.executeUpdate(
                            "UPDATE bed_backfill_request SET transfer_at='2026-10-02' WHERE approval_request_id="
                                    + request))
                    .isInstanceOfSatisfying(
                            SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("45000"));
            assertThatThrownBy(() -> command.executeUpdate(
                            "DELETE FROM bed_backfill_request WHERE approval_request_id=" + request))
                    .isInstanceOfSatisfying(
                            SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("45000"));
        }
        try (var connection = retentionConnection();
                var command = connection.createStatement()) {
            assertThat(command.executeUpdate("DELETE FROM bed_backfill_request WHERE approval_request_id=" + request))
                    .isEqualTo(1);
        }
    }

    @Test
    void FR_027_indexInsertRollsBackWithCallerTransaction() {
        long resident = TestUsers.nextId(), request = approval(), allocation = allocation(resident);
        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    insert(request, resident, allocation, bed);
                    throw new IllegalStateException("Lỗi mô phỏng sau khi lập chỉ mục");
                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_backfill_request WHERE approval_request_id=?",
                        Integer.class,
                        request))
                .isZero();
    }

    @Test
    void FR_027_existingRetentionDeletesIndexBeforeClosedAllocation() {
        long resident = TestUsers.nextId(), request = approval(), allocation = allocation(resident);
        insert(request, resident, allocation, bed);
        retention
                .transaction()
                .executeWithoutResult(
                        status -> governance.delete(retention.jdbc(), "bed_allocation", List.of(allocation)));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_backfill_request WHERE approval_request_id=?",
                        Integer.class,
                        request))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_allocation WHERE id=?", Integer.class, allocation))
                .isZero();
    }
}

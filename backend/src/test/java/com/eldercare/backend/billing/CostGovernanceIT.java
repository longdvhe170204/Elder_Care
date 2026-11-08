package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T008 phần bảng hiện có; không coi đây là nghiệm thu lưu giữ hoặc quyền tệp mua hộ. */
@Transactional
class CostGovernanceIT extends MySqlIntegrationTestBase {
    @Autowired
    List<FieldClassificationProvider> classifications;

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    @Autowired
    List<CorrectableRecordProvider> corrections;

    @Autowired
    JdbcTemplate jdbc;

    long resident, statement, item;

    private ScopeAssignmentProvider scope(String type) {
        return scopes.stream()
                .filter(provider -> type.equals(provider.entityType()))
                .findFirst()
                .orElseThrow();
    }

    @BeforeEach
    void fixture() {
        resident = TestUsers.nextId();
        long period = TestUsers.nextId();
        statement = TestUsers.nextId();
        item = TestUsers.nextId();
        long seed = Math.floorMod(resident, 72000);
        var month = YearMonth.of(2000 + (int) seed / 12, 1 + (int) seed % 12);
        var at = Timestamp.from(Instant.parse("2026-11-04T01:00:00Z"));
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_month,period_starts_on,period_ends_on,unique_key,created_at)
                VALUES (?,'PERIOD',?,?,?,?,?)
                """, period, month.toString(), month.atDay(1), month.atEndOfMonth(), "PERIOD:" + month, at);
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at)
                VALUES (?,'STATEMENT',?,?,'REGULAR',?,?)
                """, statement, period, resident, "STATEMENT:" + period + ":" + resident + ":REGULAR", at);
        jdbc.update("""
                INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,description,
                  incurred_on,quantity,unit,unit_price,amount,reason,created_at,created_by)
                VALUES (?,?,?,?,'OTHER','MANUAL','[Kiểm phạm vi chi phí]',?,1,'UNIT',300,300,'Kiểm quyền',?,1)
                """, item, resident, statement, period, month.atDay(1), at);
    }

    @Test
    void DBR_17_costItemsUseAdjustmentsInsteadOfTheGenericCorrectionSystem() {
        assertThat(corrections).noneMatch(provider -> provider.recordType().startsWith("M11."));
    }
}

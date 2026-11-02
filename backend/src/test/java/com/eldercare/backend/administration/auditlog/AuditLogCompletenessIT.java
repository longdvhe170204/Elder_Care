package com.eldercare.backend.administration.auditlog;

import static com.eldercare.backend.administration.sample.CorrectionFixtures.NURSE_A;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.sample.CorrectionFixtures;
import com.eldercare.backend.administration.sample.SampleFloorRecordProvider;
import com.eldercare.backend.administration.sample.SampleService;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SC-001, DBR-23; FR-042, FR-043, FR-047 (000 T061, research R-06): proxy trên DataSource ghi lại mọi câu lệnh; mỗi lệnh
 * thay đổi dữ liệu (nhóm 1, 2, 3 và đính chính) chạy trong một transaction có câu lệnh trên bảng nghiệp vụ và đúng một
 * câu {@code INSERT} vào {@code audit_log}.
 */
@Import({CorrectionFixtures.class, AuditLogCompletenessIT.Counting.class})
class AuditLogCompletenessIT extends MySqlIntegrationTestBase {

    private static final Pattern AUDIT_INSERT = Pattern.compile("(?is)^\\s*insert\\s+into\\s+`?audit_log\\b.*");
    private static final Reason REASON = new Reason("ghi nhầm");

    /** Câu lệnh đã chạy qua DataSource của ứng dụng, theo thứ tự. */
    static class StatementLog {
        private final List<String> statements = new ArrayList<>();

        synchronized void add(String sql) {
            statements.add(sql);
        }

        synchronized List<String> drain() {
            List<String> copy = List.copyOf(statements);
            statements.clear();
            return copy;
        }
    }

    @TestConfiguration
    static class Counting {

        @Bean
        StatementLog statementLog() {
            return new StatementLog();
        }

        @Bean
        static BeanPostProcessor countingDataSource(ObjectProvider<StatementLog> log) {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String name) {
                    if (bean instanceof DataSource dataSource && "dataSource".equals(name)) {
                        return ProxyDataSourceBuilder.create(dataSource)
                                .name("counting")
                                .afterQuery((execution, queries) ->
                                        queries.forEach(q -> log.getObject().add(q.getQuery())))
                                .build();
                    }
                    return bean;
                }
            };
        }
    }

    @Autowired
    SampleService samples;

    @Autowired
    CorrectionApi corrections;

    @Autowired
    StatementLog log;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void resetUser() {
        user.set(CorrectionFixtures.NURSE_A);
    }

    private List<String> statementsOf(Runnable command) {
        log.drain();
        command.run();
        return log.drain();
    }

    private static void assertOneAuditInsertWith(List<String> statements, String changedTable) {
        Pattern change = Pattern.compile("(?is)^\\s*(insert\\s+into|update)\\s+`?" + changedTable + "\\b.*");
        assertThat(statements)
                .as("câu lệnh trên " + changedTable)
                .anyMatch(sql -> change.matcher(sql).matches());
        assertThat(statements.stream().filter(sql -> AUDIT_INSERT.matcher(sql).matches()))
                .as("INSERT audit_log")
                .hasSize(1);
    }

    @Test
    void SC_001_catalogChange_hasOneAuditInsert() {
        List<String> sql = statementsOf(() -> samples.createCatalogItem("Mục " + UUID.randomUUID()));

        assertOneAuditInsertWith(sql, "sample_catalog");
    }

    @Test
    void SC_001_group2Commands_eachHaveOneAuditInsert() {
        long catalog = samples.createCatalogItem("Mục " + UUID.randomUUID());
        long[] caseId = new long[1];

        assertOneAuditInsertWith(
                statementsOf(() -> caseId[0] = samples.createCase(catalog, NURSE_A.accountId())), "sample_case");
        assertOneAuditInsertWith(
                statementsOf(() -> samples.addCaseVersion(caseId[0], LocalDate.of(2026, 1, 1), null, REASON)),
                "sample_case_version");
        assertOneAuditInsertWith(statementsOf(() -> samples.cancelCase(caseId[0], REASON)), "sample_case");
    }

    @Test
    void SC_001_correction_hasOneAuditInsert() {
        jdbc.update(
                "INSERT INTO sample_floor_record (occurred_at, actor_id, floor_area_id, note)"
                        + " VALUES (UTC_TIMESTAMP(6), ?, ?, 'HA 160/90')",
                NURSE_A.accountId(),
                CorrectionFixtures.FLOOR);
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        user.set(NURSE_A);

        List<String> sql = statementsOf(() -> corrections.create(
                SampleFloorRecordProvider.TYPE, id, CorrectionType.AMEND_CONTENT, Map.of("note", "HA 130/90"), REASON));

        assertOneAuditInsertWith(sql, "correction");
    }
}

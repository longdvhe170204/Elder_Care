package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.m01.internal.health.service.CatalogService;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.support.FakePermissions;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 001 T013 (DBR-23, SC-001; FR-047): proxy trên DataSource ghi lại mọi câu lệnh; mỗi lệnh của M01 chạy trong một
 * transaction có câu lệnh trên bảng nghiệp vụ và đúng một câu {@code INSERT} vào {@code audit_log}. Hiện kiểm các lệnh
 * danh mục; lệnh gắn hồ sơ người cao tuổi (có {@code resident_id} trong nhật ký) thêm vào đây cùng phase của từng user
 * story.
 */
@Import({NhatKyDayDuM01IT.Fixtures.class, NhatKyDayDuM01IT.Counting.class})
class NhatKyDayDuM01IT extends MySqlIntegrationTestBase {

    private static final Pattern AUDIT_INSERT = Pattern.compile("(?is)^\\s*insert\\s+into\\s+`?audit_log\\b.*");
    private static final Reason REASON = new Reason("Dùng thử");
    private static final CurrentUser MANAGER = FakePermissions.user(1, Role.NURSING_HOME_MANAGER);

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
    static class Fixtures {

        @Bean
        SwitchableUser currentUser() {
            return new SwitchableUser();
        }

        @Bean
        FakePermissions permissions() {
            FakePermissions permissions = new FakePermissions().account(MANAGER);
            for (Operation operation : new Operation[] {Operation.VIEW, Operation.CREATE, Operation.UPDATE}) {
                permissions.grant(Role.NURSING_HOME_MANAGER, new PermissionKey(Function.HEALTH_CATALOGS, operation));
            }
            return permissions;
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
                                .name("counting-m01")
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
    CatalogService catalogs;

    @Autowired
    StatementLog log;

    @Autowired
    SwitchableUser user;

    private List<String> statementsOf(Runnable command) {
        user.set(MANAGER);
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
    void DBR_23_catalogCommands_eachHaveOneAuditInsert() {
        String name = "Gluten " + UUID.randomUUID();
        long[] id = new long[1];

        assertOneAuditInsertWith(
                statementsOf(() -> id[0] = catalogs.create("allergens", new CatalogService.Input(name, "FOOD", null))
                        .id()),
                "allergen");
        assertOneAuditInsertWith(
                statementsOf(() -> catalogs.update(
                        "allergens", id[0], "\"0\"", new CatalogService.Input(name + " 2", "FOOD", null))),
                "allergen");
        assertOneAuditInsertWith(statementsOf(() -> catalogs.deactivate("allergens", id[0], REASON)), "allergen");
        assertOneAuditInsertWith(statementsOf(() -> catalogs.reactivate("allergens", id[0], REASON)), "allergen");
    }

    @Test
    void DBR_23_readOnlyList_writesNoAuditAndNoData() {
        List<String> sql = statementsOf(() -> catalogs.list("allergens", null, java.util.Optional.empty()));

        assertThat(sql.stream().filter(s -> AUDIT_INSERT.matcher(s).matches())).isEmpty();
        assertThat(sql.stream().filter(s -> s.toLowerCase().matches("(?s)^\\s*(insert|update|delete)\\b.*")))
                .isEmpty();
    }
}

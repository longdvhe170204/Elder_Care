package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.dto.EndCase;
import com.eldercare.backend.admission.api.dto.ResidencyEndingView;
import com.eldercare.backend.admission.api.dto.SettlementInfoView;
import com.eldercare.backend.billing.api.dto.FinalPeriodConditionView;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData.PaymentInstructions;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeRecords;
import com.eldercare.backend.billing.balance.internal.document.domain.StatementType;
import com.eldercare.backend.billing.balance.internal.document.service.PaymentNoticeService;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.settlement.service.SettlementService;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.admission.FakeResidencyEndingReads;
import com.eldercare.backend.support.fake.billing.FakeFinalPeriodCosts;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

/**
 * 017 T040, T041 (phần B của Phase 7–10; SC-010, DBR-37, DBR-38, FR-029): hai lần phát hành phiếu và hai lần xác nhận
 * quyết toán đồng thời chỉ ra một kết quả hợp lệ; bảng quyền của phiếu, quyết toán, xuất; dòng {@code record_event} của
 * phiếu, bảng quyết toán, lần xuất không sửa, không xóa, khóa duy nhất giữ DBR-37, DBR-38.
 */
class T040_ConcurrencyAndPermissionsIT extends BalanceTestBase {

    @Autowired
    PaymentNoticeService notices;

    @Autowired
    SettlementService settlements;

    @Autowired
    FakeResidencyEndingReads endings;

    @Autowired
    FakeFinalPeriodCosts finalPeriods;

    @AfterEach
    void resetFakes() {
        endings.reset();
    }

    @Test
    void DBR_37_twoConcurrentIssues_oneNotice_andRecordsAreLocked() throws Exception {
        long a = sentForSignature(TestUsers.nextId(), null);
        long statement = TestUsers.nextId();
        PaymentNoticeData data = data(PaymentNoticeRecords.code(YearMonth.of(2026, 10), statement));
        as(accountant);

        List<Object> outcomes = concurrently(
                () -> notices.publish(BalanceQueries.target(a), a, statement, data, null),
                () -> notices.publish(BalanceQueries.target(a), a, statement, data, null));

        assertThat(outcomes.stream().filter(o -> !(o instanceof Throwable))).hasSize(1);
        assertThat(outcomes.stream().filter(o -> o instanceof Throwable))
                .singleElement()
                .satisfies(e -> assertThat(rootBusiness(e)).isEqualTo("ALREADY_ISSUED"));
        assertThat(notices.list(BalanceQueries.target(a), a)).hasSize(1);

        // DBR-37, FR-029: dòng đã xác nhận không sửa, không xóa; khóa duy nhất cùng bảng không thêm lần hai
        String key = PaymentNoticeRecords.key(statement);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM record_event WHERE unique_key = ?", Integer.class, key))
                .isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("UPDATE record_event SET content = '{}' WHERE unique_key = ?", key))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM record_event WHERE unique_key = ?", key))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at,"
                                + " confirmed_at, unique_key) SELECT entity_type, entity_id, module, kind, content,"
                                + " by_system, occurred_at, confirmed_at, unique_key FROM record_event WHERE unique_key = ?",
                        key))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void DBR_38_twoConcurrentSettlementConfirmations_versionsStayDistinct() throws Exception {
        long a = sentForSignature(TestUsers.nextId(), null);
        finalPeriods.condition(a, new FinalPeriodConditionView(true, List.of()));
        endings.preparing(
                new ResidencyEndingView(TestUsers.nextId(), a, EndCase.CONTRACT_EXPIRED, time.today(), null),
                new SettlementInfoView(null, null, null, time.today(), EndCase.CONTRACT_EXPIRED, null));
        as(accountant);

        List<Object> outcomes = concurrently(
                () -> settlements.confirm(BalanceQueries.target(a), a),
                () -> settlements.confirm(BalanceQueries.target(a), a));

        assertThat(outcomes.stream().filter(o -> !(o instanceof Throwable)))
                .as("ít nhất một lần xác nhận")
                .isNotEmpty();
        assertThat(outcomes.stream().filter(o -> o instanceof Throwable))
                .allSatisfy(e -> assertThat(rootBusiness(e)).startsWith("SETTLEMENT_"));
        List<Integer> versions = jdbc.queryForList(
                "SELECT CAST(JSON_UNQUOTE(JSON_EXTRACT(content, '$.version')) AS UNSIGNED) FROM record_event"
                        + " WHERE kind = 'M11.SETTLEMENT_STATEMENT' AND entity_id = ? ORDER BY 1",
                Integer.class,
                a);
        assertThat(versions).isNotEmpty().doesNotHaveDuplicates().startsWith(1);
        assertThat(versions.getLast()).isEqualTo(versions.size());
    }

    @Test
    void SC_010_permissionsOfNoticeSettlementAndExport_byRole() throws Exception {
        long a = sentForSignature(TestUsers.nextId(), null);
        finalPeriods.condition(a, new FinalPeriodConditionView(true, List.of()));
        String notices = "/api/v1/residents/" + a + "/payment-notices";
        String settlement = "/api/v1/residents/" + a + "/settlement";
        getAs(accountant, notices).andExpect(status().isOk());
        getAs(manager, notices).andExpect(status().isOk());
        getAs(office, notices).andExpect(status().isForbidden());
        getAs(nurse, notices).andExpect(status().isForbidden());

        getAs(manager, settlement).andExpect(status().isOk());
        getAs(accountant, settlement).andExpect(status().isOk());
        getAs(office, settlement).andExpect(status().isForbidden());
        getAs(nurse, settlement).andExpect(status().isForbidden());

        // lập lệnh xuất và xác nhận quyết toán: chỉ Kế toán
        String report = "{\"from\":\"" + time.today() + "\",\"to\":\"" + time.today() + "\",\"format\":\"PDF\"}";
        for (var who : List.of(manager, office, nurse)) {
            as(who);
            mvc.perform(post("/api/v1/balance-exports/income-expense-report")
                            .with(org.springframework.security.test.web.servlet.request
                                    .SecurityMockMvcRequestPostProcessors.csrf())
                            .header("Idempotency-Key", java.util.UUID.randomUUID())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(report))
                    .andExpect(status().isForbidden());
            postAs(who, "/api/v1/residents/" + a + "/settlement/confirm", "").andExpect(status().isForbidden());
        }
        as(office);
        mvc.perform(get("/api/v1/balance-exports/1/file")).andExpect(status().isForbidden());
    }

    @SafeVarargs
    private static List<Object> concurrently(Callable<Object>... tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.length);
        try {
            java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                try {
                    results.add(future.get());
                } catch (java.util.concurrent.ExecutionException e) {
                    results.add(e.getCause());
                }
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static String rootBusiness(Object failure) {
        Throwable t = (Throwable) failure;
        while (t != null && !(t instanceof BusinessException)) {
            t = t.getCause();
        }
        return t == null ? failure.toString() : ((BusinessException) t).errorCode();
    }

    private static PaymentNoticeData data(String code) {
        return new PaymentNoticeData(
                code,
                java.time.LocalDate.of(2026, 11, 3),
                "Trần Thị Kế Toán",
                "Nguyễn Thị Ánh",
                null,
                "10/2026",
                StatementType.REGULAR,
                List.of(),
                List.of(),
                1_000_000,
                2_000_000,
                1_000_000,
                new PaymentInstructions("Vietcombank", "0011004000123", "VIEN DUONG LAO BINH AN", "NCT7K2M9X4A"));
    }
}

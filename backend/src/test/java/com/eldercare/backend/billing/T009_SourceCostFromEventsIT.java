package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.dto.ContractContentView;
import com.eldercare.backend.billing.internal.source.domain.BillingSourceKey;
import com.eldercare.backend.billing.internal.source.domain.IndexedBillingSource;
import com.eldercare.backend.billing.internal.source.domain.SourceCostSelection;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import com.eldercare.backend.billing.internal.source.service.CostSourceLookup;
import com.eldercare.backend.billing.internal.source.service.SourceCostCommands;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * 010 T009, T013 (FR-005, FR-008, FR-012, FR-019, DBR-15, BR-M11-01): nguồn trong chỉ mục lấy loại, đơn vị, dịch vụ từ danh
 * mục 004 (A duyệt Q1 → Q3 ngày 2026-11-10) rồi lập khoản theo phiên bản đơn giá; thuốc giữ đơn vị dùng, không quy đổi;
 * không xác định được dịch vụ duy nhất thì khoản thiếu đơn giá; lập lại cùng đơn vị bị bỏ qua, kể cả hai lệnh đồng thời;
 * nguồn ngoài thời gian tính phí (FR-011: trước ngày bắt đầu hợp đồng, sau mốc dừng tính phí của 004) bị chặn. Hợp đồng và
 * mốc dừng của 004 đặt bằng spy trên API công khai, không tạo hợp đồng thật.
 */
@Transactional
@Import(CostWorkflowIT.Users.class)
class T009_SourceCostFromEventsIT extends MySqlIntegrationTestBase {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 5);

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser current;

    @Autowired
    BillingSourceIndexStore index;

    @Autowired
    CostSourceLookup lookup;

    @Autowired
    SourceCostCommands commands;

    @MockitoSpyBean
    ContractReadApi contracts;

    @MockitoSpyBean
    ResidencyDayApi residencyDays;

    long resident;
    long medication;
    long diaper;

    @BeforeEach
    void fixture() {
        current.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        resident = TestUsers.nextId();
        medication = TestUsers.nextId();
        // Hợp đồng hiệu lực từ 01/01/2026, không đăng ký dịch vụ nên giá lấy theo phiên bản đơn giá.
        doReturn(Optional.of(new ContractContentView(
                        TestUsers.nextId(),
                        1,
                        LocalDate.of(2026, 1, 1),
                        null,
                        JsonMapper.builder().build().readTree("{}"),
                        Map.of())))
                .when(contracts)
                .contentAtDate(eq(resident), any());
        doReturn(Optional.empty()).when(residencyDays).billingStopDate(resident);
        diaper = service("Tã người lớn " + UUID.randomUUID(), "CONSUMABLE", "gói", null, 20_000);
        service("Thuốc viện " + UUID.randomUUID(), "FACILITY_SUPPLIED_MEDICATION", "viên", medication, 1_500);
    }

    private long service(String name, String type, String unit, Long medicationId, Integer price) {
        jdbc.update(
                "INSERT INTO catalog_item (catalog_type, module, name, status, attributes)"
                        + " VALUES ('M02.SERVICE', 'M02', ?, 'ACTIVE', ?)",
                name,
                "{\"type\":\"%s\",\"unit\":\"%s\",\"billingPeriod\":\"OCCURRENCE\",\"careLevelCode\":null,\"roomTypeId\":null,\"medicationId\":%s}"
                        .formatted(type, unit, medicationId));
        long id = jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M02.SERVICE' AND name=? ORDER BY id DESC LIMIT 1",
                Long.class,
                name);
        if (price != null)
            jdbc.update(
                    "INSERT INTO price_version (service_id, unit_price, effective_from, effective_to, referenced)"
                            + " VALUES (?, ?, '2000-01-01', NULL, FALSE)",
                    id,
                    price);
        return id;
    }

    private void indexed(
            String type, String id, String unit, Long serviceId, Long medicationId, String qty, String usage) {
        index.insertIfAbsent(
                new IndexedBillingSource(
                        type,
                        id,
                        unit,
                        resident,
                        DAY,
                        Instant.parse("2026-10-05T02:00:00Z"),
                        serviceId,
                        medicationId,
                        new BigDecimal(qty),
                        usage),
                time.now());
    }

    @Test
    void DBR_15_secondCreationOfSameUnit_isSkipped() {
        indexed("CARE_TASK_RESULT", "x-" + resident, "UNIT", diaper, null, "1", null);
        var units = List.of(new BillingSourceKey("x-" + resident, "UNIT"));
        commands.create(new SourceCostSelection(resident, "CARE_TASK_RESULT", null, units, null, null));

        var again = commands.create(new SourceCostSelection(resident, "CARE_TASK_RESULT", null, units, null, null));

        assertThat(again.succeededIds()).isEmpty();
        assertThat(again.skipped())
                .singleElement()
                .extracting(s -> s.errorCode())
                .isEqualTo("COST_ITEM_ALREADY_EXISTS");
    }

    private SourceCostSelection careUnit(String id) {
        return new SourceCostSelection(
                resident, "CARE_TASK_RESULT", null, List.of(new BillingSourceKey(id, "UNIT")), null, null);
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void DBR_15_twoConcurrentCommandsForTheSameUnit_createOneItem() throws Exception {
        String id = "p-" + resident;
        indexed("CARE_TASK_RESULT", id, "UNIT", diaper, null, "1", null);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> create = () -> {
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Không mở cổng bắt đầu");
                return commands.create(careUnit(id)).succeededIds().size();
            };
            var first = executor.submit(create);
            var second = executor.submit(create);
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(1, 0);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cost_item WHERE source_type='CARE_TASK_RESULT' AND source_id=? AND status<>'CANCELLED'",
                        Long.class,
                        id))
                .isEqualTo(1);
    }
}

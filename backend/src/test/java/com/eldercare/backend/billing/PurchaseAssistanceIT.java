package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.billing.internal.purchase.domain.PurchaseDetails;
import com.eldercare.backend.billing.internal.purchase.service.PurchaseAssistanceService;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@org.springframework.test.context.event.RecordApplicationEvents
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@Import(CostWorkflowIT.Users.class)
class PurchaseAssistanceIT extends MySqlIntegrationTestBase {
    @Autowired
    PurchaseAssistanceService service;

    @Autowired
    SimpleRecordApi records;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @MockitoSpyBean
    SwitchableUser current;

    @Autowired
    org.springframework.test.web.servlet.MockMvc mvc;

    @Autowired
    org.springframework.test.context.event.ApplicationEvents events;

    @Autowired
    com.eldercare.backend.billing.internal.item.service.CostItemService items;

    @Autowired
    com.eldercare.backend.administration.retention.api.RetentionDatabase retentionDatabase;

    @Autowired
    com.eldercare.backend.common.file.FileApi files;

    @MockitoSpyBean
    com.eldercare.backend.resident.api.ResidentApi residents;

    @MockitoSpyBean
    com.eldercare.backend.billing.internal.purchase.domain.PurchaseBalancePort balances;

    @MockitoSpyBean
    AuditLogApi audit;

    @MockitoSpyBean
    com.eldercare.backend.admission.api.spi.RepresentativePort representatives;

    @MockitoSpyBean
    com.eldercare.backend.notification.api.spi.FamilyNotificationPort familyNotifications;

    /** 010 T033 (2026-11-11): quan hệ người thân, người đại diện thật của 012 thay bản giả của 002. */
    @Autowired
    com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore relationshipHistory;

    @Autowired
    org.springframework.transaction.support.TransactionTemplate transactions;

    private com.eldercare.backend.family.RelationshipFixtures relationships() {
        return new com.eldercare.backend.family.RelationshipFixtures(
                jdbc, transactions, relationshipHistory, time.now().minus(java.time.Duration.ofDays(10)));
    }

    @Autowired
    com.eldercare.backend.billing.internal.purchase.service.PurchaseReceiptPermission receiptPermission;

    @Autowired
    com.eldercare.backend.administration.shared.api.SharedRowRetention sharedRows;

    @Autowired
    com.eldercare.backend.billing.internal.purchase.persistence.PurchaseRetention retention;

    @Autowired
    com.eldercare.backend.billing.internal.purchase.service.PurchaseReminderTask reminders;

    long resident;
    TestUsers.TestUser office, manager;

    @BeforeEach
    void setup() {
        var users = new TestUsers(jdbc, time);
        office = users.staff(Role.OFFICE_STAFF);
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        current.set(office.user());
        resident = TestUsers.nextId();
    }

    long create(long amount) {
        return service.create(
                resident,
                null,
                "Đồ dùng cá nhân",
                BigDecimal.ONE,
                amount,
                "STAFF",
                office.user().staffId().orElseThrow(),
                new Reason("Theo yêu cầu trực tiếp"));
    }

    @Test
    void DBR_23_auditFailureRollsBackCreation() {
        doThrow(new IllegalStateException("audit unavailable")).when(audit).write(any());
        org.springframework.test.context.transaction.TestTransaction.flagForCommit();
        org.springframework.test.context.transaction.TestTransaction.end();
        assertThatThrownBy(() -> create(300000)).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE kind=? AND resident_id=?",
                        Long.class,
                        PurchaseDetails.KIND.code(),
                        resident))
                .isZero();
    }

    @Autowired
    com.eldercare.backend.billing.internal.purchase.service.PurchaseRecordingService recorder;

    @Test
    void FR_023_twoRepresentativesRaceOnlyOneDecisionCommits() throws Exception {
        var users = new TestUsers(jdbc, time);
        var family = relationships();
        var first = users.family(family.familyMember("Đại diện A " + TestUsers.nextId(), "0902000002"));
        var second = users.family(family.familyMember("Đại diện B " + TestUsers.nextId(), "0902000003"));
        for (var user : java.util.List.of(first, second))
            family.relate(user.user().familyMemberId().orElseThrow(), resident, 1, true, false);
        long id = create(800000);
        service.send(id);
        var target = service.target(id);
        org.springframework.test.context.transaction.TestTransaction.flagForCommit();
        org.springframework.test.context.transaction.TestTransaction.end();
        var userByThread = ThreadLocal.withInitial(() -> office.user());
        doAnswer(i -> userByThread.get().accountId()).when(current).accountId();
        doAnswer(i -> userByThread.get().type()).when(current).type();
        doAnswer(i -> userByThread.get().familyMemberId()).when(current).familyMemberId();
        doAnswer(i -> userByThread.get().staffId()).when(current).staffId();
        doAnswer(i -> userByThread.get().roles()).when(current).roles();
        doAnswer(i -> userByThread.get().channel()).when(current).channel();
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for (var user : java.util.List.of(first, second))
                futures.add(pool.submit(() -> {
                    userByThread.set(user.user());
                    ready.countDown();
                    try {
                        if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS))
                            throw new AssertionError("Chưa bắt đầu quyết định đồng thời");
                        service.agree(id, target);
                        return true;
                    } catch (com.eldercare.backend.common.error.BusinessException expected) {
                        return false;
                    } finally {
                        userByThread.remove();
                    }
                }));
            assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int winners = 0;
            for (var future : futures) if (future.get(20, java.util.concurrent.TimeUnit.SECONDS)) winners++;
            assertThat(winners).isEqualTo(1);
        } finally {
            start.countDown();
        }
        assertThat(records.require(PurchaseDetails.KIND, id).status()).isEqualTo("PURCHASE_ALLOWED");
        assertThat(service.decisions(id)).hasSize(1);
    }

    @Autowired
    com.eldercare.backend.billing.internal.item.service.ManualCostService manual;

    @Autowired
    com.eldercare.backend.administration.account.api.spi.StaffProfileApi staffProfiles;

    private long receipt() {
        long file = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO file(id,uuid,original_name,mime_type,size_bytes,sha256,uploaded_by,uploaded_at) VALUES (?,?,?,'application/pdf',1,?,?,?)",
                file,
                java.util.UUID.randomUUID().toString(),
                "receipt.pdf",
                "0".repeat(64),
                office.user().accountId(),
                java.sql.Timestamp.from(time.now()));
        return file;
    }
}

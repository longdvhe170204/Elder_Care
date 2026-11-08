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
@Import({
    CostWorkflowIT.Users.class,
    com.eldercare.backend.support.fake.administration.FakeFamilyRelationshipsConfig.class,
    com.eldercare.backend.support.fake.billing.FakePurchaseBalance.class
})
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

    @Autowired
    com.eldercare.backend.support.fake.administration.FakeFamilyRelationships relationships;

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
    void BR_M11_07_lowAmountAllowedAndCancelledRecordIsLocked() {
        long id = create(300000);
        service.send(id);
        assertThat(records.require(PurchaseDetails.KIND, id).status()).isEqualTo("PURCHASE_ALLOWED");
        service.cancel(id, new Reason("Không còn nhu cầu"));
        assertThat(records.require(PurchaseDetails.KIND, id).lockedAt()).isNotNull();
        assertThatThrownBy(() -> service.send(id)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void Q_259_rejectAdditionalKeepsOriginalAmountAndHistory() {
        long id = create(300000);
        service.send(id);
        service.requestAdditional(id, 800000, new Reason("Thay đổi nhu cầu"));
        current.set(manager.user());
        service.reject(id, new Reason("Giữ mức cũ"), service.target(id));
        var row = records.require(PurchaseDetails.KIND, id);
        assertThat(row.status()).isEqualTo("PURCHASE_ALLOWED");
        assertThat(row.content().consent().allowedAmount()).isEqualTo(300000);
        assertThat(service.decisions(id)).hasSize(2);
    }

    @Test
    void FR_017_managerCannotCreateAndOfficeCannotAgree() {
        long id = create(800000);
        service.send(id);
        assertThatThrownBy(() -> service.agree(id, service.target(id)))
                .isInstanceOf(com.eldercare.backend.common.error.PermissionDeniedException.class);
        current.set(manager.user());
        assertThatThrownBy(() -> create(300000))
                .isInstanceOf(com.eldercare.backend.common.error.PermissionDeniedException.class);
        service.agree(id, service.target(id));
        assertThatThrownBy(() -> service.agree(id, service.target(id)))
                .isInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
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

    @Test
    void FR_024_purchaseCreatesOneDraftCostWithConsentSnapshot() {
        long id = create(300000);
        service.send(id);
        long file = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO file(id,uuid,original_name,mime_type,size_bytes,sha256,uploaded_by,uploaded_at) VALUES (?,?,?,'application/pdf',1,?,?,?)",
                file,
                java.util.UUID.randomUUID().toString(),
                "receipt.pdf",
                "0".repeat(64),
                office.user().accountId(),
                java.sql.Timestamp.from(time.now()));
        recorder.record(id, 310000, time.today(), file, false, null);
        assertThat(records.require(PurchaseDetails.KIND, id).status()).isEqualTo("PURCHASED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cost_item WHERE purchase_assistance_id=? AND origin='PURCHASE_ASSISTANCE' AND status='DRAFT'",
                        Long.class,
                        id))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT flags FROM cost_item WHERE purchase_assistance_id=?", String.class, id))
                .contains("EXCEEDS_CONSENT");
        long receiptId = jdbc.queryForObject("SELECT owner_id FROM file WHERE id=?", Long.class, file);
        assertThat(receiptPermission.canDownload(receiptId)).isTrue();
        assertThat(files.attachedTo("M11.PURCHASE_ASSISTANCE_RECEIPT", receiptId))
                .hasSize(1);
        current.set(new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user());
        assertThat(receiptPermission.canDownload(receiptId)).isFalse();
        current.set(office.user());
        assertThat(sharedRows.rowsFor(new com.eldercare.backend.administration.shared.api.dto.EntityRef(
                        PurchaseDetails.KIND.code(), id)))
                .contains(new com.eldercare.backend.administration.shared.api.dto.EntityRef(
                        "M11.PURCHASE_ASSISTANCE_RECEIPT", receiptId));
        assertThat(retention.referencesTo(id)).hasSize(1);
        assertThatThrownBy(() -> recorder.record(id, 310000, time.today(), file, false, null))
                .isInstanceOf(RuntimeException.class);
        long item = jdbc.queryForObject("SELECT id FROM cost_item WHERE purchase_assistance_id=?", Long.class, id);
        var selection =
                new com.eldercare.backend.billing.internal.item.domain.CostItemSelection(java.util.List.of(item), null);
        items.check(selection);
        current.set(manager.user());
        assertThat(items.approve(selection, "Chấp nhận phần vượt có chứng từ").succeededIds())
                .containsExactly(item);
        var notice = events.stream(com.eldercare.backend.billing.internal.period.domain.CostNotificationEvent.class)
                .flatMap(event -> event.notificationRequests().stream())
                .filter(n -> n.eventType().equals("PURCHASE_EXCESS_APPROVED"))
                .findFirst()
                .orElseThrow();
        assertThat(notice.toString()).contains("300000", "310000", "Chấp nhận phần vượt có chứng từ");
    }

    @Autowired
    com.eldercare.backend.billing.internal.purchase.service.PurchaseRecordingService recorder;

    @Test
    void FR_023a_onlySnapshottedStillActiveRepresentativeCanDecide() {
        long member = TestUsers.nextId();
        var user = new TestUsers(jdbc, time).family(member);
        relationships.representative(relationships.relate(member, resident));
        doReturn(true).when(representatives).isActiveRepresentative(member, resident, time.today());
        doReturn(java.util.List.of(new com.eldercare.backend.notification.api.spi.NotifiableRelationshipView(
                        member,
                        false,
                        true,
                        java.util.Set.of(),
                        java.util.Optional.of(user.accountId()),
                        true,
                        java.util.Optional.empty())))
                .when(familyNotifications)
                .activeRelationships(resident);
        long id = create(800000);
        service.send(id);
        current.set(user.user());
        assertThat(service.list(resident, null)).hasSize(1);
        doReturn(false).when(representatives).isActiveRepresentative(member, resident, time.today());
        assertThatThrownBy(() -> service.agree(id, service.target(id)))
                .isInstanceOf(com.eldercare.backend.common.error.PermissionDeniedException.class);
        assertThat(service.list(resident, null)).isEmpty();
        doReturn(true).when(representatives).isActiveRepresentative(member, resident, time.today());
        service.agree(id, service.target(id));
        assertThat(records.require(PurchaseDetails.KIND, id).status()).isEqualTo("PURCHASE_ALLOWED");
    }

    @Test
    void FR_024_excessIsBlockedWithoutChangingRequestOrCreatingCost() {
        long id = create(800000);
        service.send(id);
        current.set(manager.user());
        service.agree(id, service.target(id));
        current.set(office.user());
        assertThatThrownBy(() -> recorder.record(id, 1000000, time.today(), TestUsers.nextId(), false, null))
                .isInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
        assertThat(records.require(PurchaseDetails.KIND, id).status()).isEqualTo("PURCHASE_ALLOWED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE purchase_assistance_id=?", Long.class, id))
                .isZero();
    }

    @Test
    void FR_023a_remindersRepeatOnlyForNewConsentRound() {
        long id = create(800000);
        service.send(id);
        var at = time.now().plus(java.time.Duration.ofDays(5));
        reminders.run(at);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind='M11.PURCHASE_REMINDER' AND entity_id=?",
                        Long.class,
                        id))
                .isEqualTo(2);
        reminders.run(at);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind='M11.PURCHASE_REMINDER' AND entity_id=?",
                        Long.class,
                        id))
                .isEqualTo(2);
        current.set(manager.user());
        service.agree(id, service.target(id));
        current.set(office.user());
        service.requestAdditional(id, 900000, new Reason("Cần bổ sung"));
        reminders.run(at);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind='M11.PURCHASE_REMINDER' AND entity_id=?",
                        Long.class,
                        id))
                .isEqualTo(4);
    }

    @Test
    void FR_023_twoRepresentativesRaceOnlyOneDecisionCommits() throws Exception {
        var users = new TestUsers(jdbc, time);
        var first = users.family(TestUsers.nextId());
        var second = users.family(TestUsers.nextId());
        for (var user : java.util.List.of(first, second)) {
            long member = user.user().familyMemberId().orElseThrow();
            relationships.representative(relationships.relate(member, resident));
            doReturn(true).when(representatives).isActiveRepresentative(member, resident, time.today());
        }
        doReturn(java.util.List.of(first, second).stream()
                        .map(user -> new com.eldercare.backend.notification.api.spi.NotifiableRelationshipView(
                                user.user().familyMemberId().orElseThrow(),
                                false,
                                true,
                                java.util.Set.of(),
                                java.util.Optional.of(user.accountId()),
                                true,
                                java.util.Optional.<String>empty()))
                        .toList())
                .when(familyNotifications)
                .activeRelationships(resident);
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

    @Test
    void FR_023_httpRejectsInvalidStatusAndReplaysSendWithoutChangingVersion() throws Exception {
        String bearer = TestUsers.bearer(new TestUsers(jdbc, time).session(office.accountId()));
        long id = create(300000);
        var key = java.util.UUID.randomUUID();
        for (int n = 0; n < 2; n++)
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                    "/api/v1/purchase-assistance-requests/{id}/send", id)
                            .header("Authorization", bearer)
                            .header("Idempotency-Key", key))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                            .isOk());
        assertThat(records.require(PurchaseDetails.KIND, id).version()).isEqualTo(1);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/api/v1/purchase-assistance-requests")
                        .param("status", "INVALID")
                        .header("Authorization", bearer))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isUnprocessableEntity());
    }

    @Test
    void FR_051_purchaseRetentionFollowsResidentExpiryAndPaginates() {
        long first = create(300000), second = create(400000);
        var ended = time.now().minus(java.time.Duration.ofDays(500));
        doReturn(java.util.List.of(resident)).when(residents).byStatus(anySet());
        doReturn(java.util.Optional.of(ended)).when(residents).finalStatusAt(resident);
        var page = retention.expiredCandidates(time.now(), 1).toList();
        assertThat(page).hasSize(1);
        assertThat(page.getFirst().id()).isEqualTo(first);
        assertThat(retention
                        .expiredCandidates(time.now(), page.getFirst(), 1)
                        .map(c -> c.id())
                        .toList())
                .containsExactly(second);
        assertThat(retention.expiredCandidates(ended, 10).toList()).isEmpty();
        assertThat(retention.referencing(
                        new com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef(
                                "M01.RESIDENT", resident)))
                .hasSize(2);
    }

    @Test
    void BR_M11_15_balanceWarningRefreshesWhileAwaitingDecisionWithoutBlockingConsent() {
        doReturn(true).when(balances).insufficientBalance(eq(resident), eq(800000L), any());
        long id = create(800000);
        service.send(id);
        assertThat(service.list(resident, null).getFirst().content().insufficientBalance())
                .isTrue();
        doReturn(false).when(balances).insufficientBalance(eq(resident), eq(800000L), any());
        assertThat(service.list(resident, null).getFirst().content().insufficientBalance())
                .isFalse();
        doReturn(true).when(balances).insufficientBalance(eq(resident), eq(800000L), any());
        current.set(manager.user());
        service.agree(id, service.target(id));
        assertThat(records.require(PurchaseDetails.KIND, id).status()).isEqualTo("PURCHASE_ALLOWED");
        assertThat(service.decisions(id).getFirst().insufficientBalance()).isTrue();
    }

    @Test
    void FR_023a_familyReceivesMediumNoticeAndOfficeIsWarnedWhenPortalRepresentativesDisappear() {
        long member = TestUsers.nextId();
        doReturn(java.util.List.of(new com.eldercare.backend.notification.api.spi.NotifiableRelationshipView(
                        member,
                        false,
                        true,
                        java.util.Set.of(),
                        java.util.Optional.of(TestUsers.nextId()),
                        true,
                        java.util.Optional.empty())))
                .when(familyNotifications)
                .activeRelationships(resident);
        long id = create(800000);
        service.send(id);
        var notice = events.stream(com.eldercare.backend.billing.api.events.PurchaseAssistanceSent.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(n -> n.recipientGroups()
                        .contains(
                                new com.eldercare.backend.common.notification.RecipientGroup.Representative(resident)))
                .findFirst()
                .orElseThrow();
        assertThat(notice.level()).contains(com.eldercare.backend.common.notification.NotificationLevel.MEDIUM);
        doReturn(java.util.List.of()).when(familyNotifications).activeRelationships(resident);
        reminders.run(time.now());
        reminders.run(time.now());
        assertThat(events.stream(com.eldercare.backend.billing.internal.period.domain.CostNotificationEvent.class)
                        .flatMap(e -> e.notificationRequests().stream())
                        .filter(n -> n.eventType().equals("PURCHASE_NO_PORTAL_REPRESENTATIVE")
                                && n.residentId().orElse(-1L) == resident)
                        .count())
                .isEqualTo(1);
    }

    @Test
    void FR_051_retentionDeletesOnlyRequestedRecordAndRollsBackAsOneChain() {
        long id = create(300000), kept = create(400000);
        service.send(id);
        service.requestAdditional(id, 800000, new Reason("Bổ sung nhu cầu"));
        service.cancel(id, new Reason("Không còn nhu cầu"));
        var parent = new com.eldercare.backend.administration.shared.api.dto.EntityRef(PurchaseDetails.KIND.code(), id);
        assertThat(sharedRows.rowsFor(parent)).isNotEmpty();
        org.springframework.test.context.transaction.TestTransaction.flagForCommit();
        org.springframework.test.context.transaction.TestTransaction.end();
        try {
            retentionDatabase.transaction().executeWithoutResult(tx -> {
                sharedRows.deleteFor(java.util.List.of(parent));
                retention.delete(java.util.List.of(id));
                tx.setRollbackOnly();
            });
            assertThat(records.require(PurchaseDetails.KIND, id).status()).isEqualTo("CANCELLED");
            assertThat(sharedRows.rowsFor(parent)).isNotEmpty();
            retentionDatabase.transaction().executeWithoutResult(tx -> {
                sharedRows.deleteFor(java.util.List.of(parent));
                retention.delete(java.util.List.of(id));
            });
            assertThatThrownBy(() -> records.require(PurchaseDetails.KIND, id))
                    .isInstanceOf(com.eldercare.backend.common.error.NotFoundException.class);
            assertThat(records.require(PurchaseDetails.KIND, kept).status()).isEqualTo("DRAFT");
        } finally {
            retentionDatabase.transaction().executeWithoutResult(tx -> {
                sharedRows.deleteFor(java.util.List.of(parent));
                retention.delete(java.util.List.of(id, kept));
            });
        }
    }
}

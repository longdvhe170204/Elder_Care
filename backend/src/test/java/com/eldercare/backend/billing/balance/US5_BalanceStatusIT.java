package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.billing.api.dto.CostStatementType;
import com.eldercare.backend.billing.api.events.CostStatementFinalized;
import com.eldercare.backend.billing.balance.internal.status.domain.BalanceNotice;
import com.eldercare.backend.billing.balance.scheduling.BalanceStatusJob;
import com.eldercare.backend.billing.internal.purchase.domain.PurchaseBalancePort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 017 T019 (US5 #1 → #7; FR-016 → FR-021; BR-M11-13, 15; SC-005, SC-012; quickstart Q8 → Q10, Q35, Q36): job hằng ngày
 * tính tình trạng, báo người đại diện (phần chung, phần chi phí) mức Trung bình và Kế toán mức Nhẹ khi chuyển Sắp hết
 * tiền, Còn nợ; chạy lại cùng ngày không báo trùng; nhắc lại sau CFG-M11-05, Quản lý viện thêm khi còn nợ kéo dài; nộp
 * tiền báo ngay "đã đủ" hoặc "đã ghi nhận" theo tình trạng sau khi nộp; dừng nhắc chỉ cho hồ sơ trạng thái cuối còn nợ;
 * dấu mua hộ "số dư không đủ".
 */
class US5_BalanceStatusIT extends BalanceTestBase {

    @Autowired
    BalanceStatusJob job;

    /** Bản cài của 017 (FakeM11Config có bản giả @Primary của cổng này cho test 010). */
    @Autowired
    List<PurchaseBalancePort> purchasePorts;

    @Test
    void Q8_Q9_Q10_Q35_Q36_dailyStatus_reminders_paymentNotices_andStopReminders() throws Exception {
        LocalDate start = time.today().plusDays(1);
        try {
            day(start);
            long contract = TestUsers.nextId();
            long a = sentForSignature(contract, null);
            activate(contract);
            cash(a, 6_000_000);
            costs.total(a, 2_400_000);
            for (int d = 0; d < 40; d++) costs.dailyFee(a, start.plusDays(0).plusDays(d), 300_000);

            job.runNow();
            assertThat(ledger(a, "status")).isEqualTo("RUNNING_LOW");
            assertThat(ledger(a, "days_covered")).isEqualTo("12");
            List<NotificationRequest> first = notices(a);
            assertThat(first)
                    .extracting(NotificationRequest::eventType)
                    .containsExactlyInAnyOrder("M11_BALANCE_RUNNING_LOW", "M11_BALANCE_RUNNING_LOW");
            NotificationRequest family = forGroup(first, new RecipientGroup.Representative(a));
            assertThat(family.level()).contains(NotificationLevel.MEDIUM);
            assertThat(family.content())
                    .extracting(c -> c.type())
                    .containsExactly(InformationType.GENERAL, InformationType.COSTS);
            assertThat(forGroup(first, new RecipientGroup.FacilityRole(Role.ACCOUNTANT))
                            .level())
                    .contains(NotificationLevel.LOW);
            job.runNow();
            assertThat(notices(a))
                    .as("chạy lại cùng ngày không báo trùng (SC-012)")
                    .hasSize(2);

            day(start.plusDays(6));
            job.runNow();
            assertThat(notices(a)).as("chưa qua CFG-M11-05").hasSize(2);
            day(start.plusDays(7));
            job.runNow();
            assertThat(notices(a)).as("nhắc lại sau 7 ngày").hasSize(4);

            costs.total(a, 7_000_000);
            day(start.plusDays(8));
            job.runNow();
            assertThat(ledger(a, "status")).isEqualTo("IN_DEBT");
            assertThat(notices(a).stream().filter(n -> n.eventType().equals("M11_BALANCE_IN_DEBT")))
                    .hasSize(2);
            day(start.plusDays(15));
            job.runNow();
            assertThat(notices(a).stream().filter(n -> n.eventType().equals("M11_BALANCE_IN_DEBT_MANAGER")))
                    .singleElement()
                    .satisfies(n -> assertThat(n.recipientGroups())
                            .containsExactly(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)));

            cash(a, 10_000_000);
            assertThat(ledger(a, "status")).isEqualTo("NORMAL");
            assertThat(notices(a).stream().filter(n -> n.eventType().equals("M11_BALANCE_PAYMENT_SUFFICIENT")))
                    .singleElement()
                    .satisfies(
                            n -> assertThat(n.recipientGroups()).containsExactly(new RecipientGroup.Representative(a)));

            costs.total(a, 15_000_000);
            day(start.plusDays(16));
            job.runNow();
            assertThat(ledger(a, "status")).isEqualTo("RUNNING_LOW");
            cash(a, 200_000);
            assertThat(ledger(a, "status")).isEqualTo("RUNNING_LOW");
            assertThat(notices(a).stream().filter(n -> n.eventType().equals("M11_BALANCE_PAYMENT_RECORDED")))
                    .hasSize(1);
            getAs(accountant, "/api/v1/balance-statuses?status=RUNNING_LOW")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.residentId == " + a + ")].daysCovered")
                            .value(4));
            getAs(office, "/api/v1/balance-statuses").andExpect(status().isForbidden());
            PurchaseBalancePort purchases = purchasePorts.stream()
                    .filter(p -> p.getClass().getName().endsWith(".internal.status.service.PurchaseBalance"))
                    .findFirst()
                    .orElseThrow();
            assertThat(purchases.insufficientBalance(a, 1_500_000, time.now())).isTrue();
            assertThat(purchases.insufficientBalance(a, 1_000_000, time.now())).isFalse();

            postAs(manager, "/api/v1/residents/" + a + "/balance/stop-reminders", "{\"reason\":\"Đã xin ý kiến\"}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("NOT_FINAL_STATUS"));
            long b = sentForSignature(TestUsers.nextId(), null);
            as(accountant);
            tx.executeWithoutResult(s -> publisher.publishEvent(new CostStatementFinalized(
                    TestUsers.nextId(),
                    CostStatementType.REGULAR,
                    TestUsers.nextId(),
                    b,
                    900_000,
                    time.now(),
                    List.of())));
            residents.changeStatus(b, ResidentStatus.RESIDENCY_ENDED, time.now());
            postAs(accountant, "/api/v1/residents/" + b + "/balance/stop-reminders", "{\"reason\":\"Đã xin ý kiến\"}")
                    .andExpect(status().isForbidden());
            postAs(
                            manager,
                            "/api/v1/residents/" + b + "/balance/stop-reminders",
                            "{\"reason\":\"Gia đình không còn liên lạc\"}")
                    .andExpect(status().isOk());
            day(start.plusDays(17));
            job.runNow();
            assertThat(ledger(b, "status")).isEqualTo("IN_DEBT");
            assertThat(notices(b)).as("đã dừng nhắc thì không báo (FR-020)").isEmpty();
        } finally {
            clock.reset();
        }
    }

    private void day(LocalDate date) {
        clock.setTo(date.atTime(2, 0).atZone(BusinessTime.ZONE).toInstant());
    }

    private void cash(long resident, long amount) throws Exception {
        postAs(
                        accountant,
                        "/api/v1/residents/" + resident + "/cash-payments",
                        "{\"ledger\":\"BALANCE\",\"amount\":" + amount
                                + ",\"payerName\":\"Lê Văn Hải\",\"receivedAt\":\"" + time.now() + "\"}")
                .andExpect(status().isCreated());
    }

    private String ledger(long resident, String column) {
        return jdbc.queryForObject(
                "SELECT " + column + " FROM resident_balance WHERE resident_id = ?", String.class, resident);
    }

    private List<NotificationRequest> notices(long resident) {
        return events.stream(BalanceNotice.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(n -> n.residentId().equals(java.util.Optional.of(resident)))
                .toList();
    }

    private static NotificationRequest forGroup(List<NotificationRequest> notices, RecipientGroup group) {
        return notices.stream()
                .filter(n -> n.recipientGroups().contains(group))
                .findFirst()
                .orElseThrow();
    }
}

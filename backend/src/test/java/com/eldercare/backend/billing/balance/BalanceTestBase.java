package com.eldercare.backend.billing.balance;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.dto.ContractView;
import com.eldercare.backend.admission.api.dto.DepositAmountView;
import com.eldercare.backend.admission.api.events.ContractStatusChanged;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationships;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationshipsConfig;
import com.eldercare.backend.support.fake.admission.FakeContractSchedules;
import com.eldercare.backend.support.fake.admission.FakeM02Config;
import com.eldercare.backend.support.fake.admission.FakeResidencyEndingReadsConfig;
import com.eldercare.backend.support.fake.billing.FakeCostForBalance;
import com.eldercare.backend.support.fake.billing.FakeM11Config;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * IT của 017 Phase 3–6 (A): lệnh thật, MySQL, quyền thật của 002; hợp đồng 004 và chi phí 010 bằng bản giả chung, quan
 * hệ người thân bằng {@link FakeFamilyRelationships}. Sự kiện hợp đồng phát trong transaction với người dùng là người
 * gửi ký (Hành chính), như lệnh của 004.
 */
@AutoConfigureMockMvc
@WithMockUser
@RecordApplicationEvents
@Import({
    SwitchableUserConfig.class,
    FakeM01Config.class,
    FakeM02Config.class,
    FakeM11Config.class,
    FakeFamilyRelationshipsConfig.class,
    FakeResidencyEndingReadsConfig.class
})
abstract class BalanceTestBase extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    ApplicationEvents events;

    @Autowired
    FakeContractSchedules contracts;

    @Autowired
    FakeCostForBalance costs;

    @Autowired
    FakeFamilyRelationships relationships;

    @Autowired
    FakeResidents residents;

    @Autowired
    TestClockControl clock;

    TestUsers users;
    TestUsers.TestUser accountant, office, manager, nurse;

    @BeforeEach
    void balanceFixture() {
        users = new TestUsers(jdbc, time);
        accountant = users.staff(Role.ACCOUNTANT);
        office = users.staff(Role.OFFICE_STAFF);
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        nurse = users.staff(Role.NURSING_STAFF);
        costs.reset();
    }

    void as(TestUsers.TestUser who) {
        user.set(who.user());
    }

    /**
     * Hợp đồng nội trú của một người cao tuổi mới được gửi ký (004 phát {@code ContractStatusChanged} → Chờ ký); khoản cọc
     * {@code deposit} (rỗng: không yêu cầu cọc). Trả mã người cao tuổi.
     */
    long sentForSignature(long contractId, Long deposit) {
        long resident = resident();
        contracts
                .contract(new ContractView(
                        contractId,
                        "HD-" + contractId,
                        resident,
                        ResidenceType.LONG_TERM_RESIDENTIAL,
                        LocalDate.now(),
                        null,
                        null,
                        null,
                        ContractStatus.PENDING_SIGNATURE,
                        null))
                .deposit(new DepositAmountView(
                        contractId, deposit == null, deposit == null ? BigDecimal.ZERO : BigDecimal.valueOf(deposit)));
        changeContract(contractId, resident, ContractStatus.DRAFT, ContractStatus.PENDING_SIGNATURE);
        return resident;
    }

    /** Người cao tuổi Đang lưu trú mới trong {@link FakeResidents}. */
    long resident() {
        long id = TestUsers.nextId();
        residents.resident(id, "Nguyễn Văn " + id, null, ResidentStatus.IN_RESIDENCE);
        return id;
    }

    /** Hợp đồng đã gửi ký chuyển Hiệu lực (người cao tuổi được tính tình trạng số dư, FR-016). */
    void activate(long contractId) {
        var c = contracts.contract(contractId).orElseThrow();
        contracts.contract(new ContractView(
                c.id(),
                c.contractNumber(),
                c.residentId(),
                c.residenceType(),
                c.startDate(),
                c.endDate(),
                c.currentEndDate(),
                c.careLevel(),
                ContractStatus.ACTIVE,
                null));
    }

    void changeContract(long contractId, long resident, ContractStatus from, ContractStatus to) {
        as(office);
        tx.executeWithoutResult(
                s -> publisher.publishEvent(new ContractStatusChanged(contractId, resident, from, to, null)));
    }

    ResultActions getAs(TestUsers.TestUser who, String url) throws Exception {
        as(who);
        return mvc.perform(get(url));
    }

    ResultActions postAs(TestUsers.TestUser who, String url, String body) throws Exception {
        as(who);
        return mvc.perform(post(url)
                .with(csrf())
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    String paymentCode(long resident) {
        return jdbc.queryForObject(
                "SELECT payment_code FROM resident_balance WHERE resident_id = ?", String.class, resident);
    }
}

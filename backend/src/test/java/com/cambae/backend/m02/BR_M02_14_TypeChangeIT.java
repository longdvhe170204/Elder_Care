package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.events.AddendumApplied;
import com.cambae.backend.m02.internal.registration.domain.RegistrationType;
import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import com.cambae.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 018 T011 ({@code BR_M02_14_ChuyenLoaiIT}; US3 #4, #6, quickstart Q14, Q16, Q17; FR-001a, FR-007, BR-M02-14, Q-215,
 * Q-306, Q-336): phụ lục gia hạn hợp đồng nội trú của người chỉ có Thông báo lưu trú ghi nghĩa vụ và nhắc Hành chính lập
 * (không tự tạo); loại khi lập theo tổng thời gian mới (60 ngày → Đăng ký tạm trú, 21 ngày → Thông báo lưu trú); khai báo
 * mới Đã xác nhận thì Thông báo lưu trú cũ Được thay thế. Phụ lục đổi bán trú sang nội trú ghi mốc từ ngày hiệu lực.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(FakeFileConfig.class)
class BR_M02_14_TypeChangeIT extends RegistrationTestBase {

    private static final AtomicLong ADDENDA = new AtomicLong(917_000_000L);

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    TestClockControl clock;

    @Autowired
    FileApi files;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private long evidence() {
        return files.upload(
                "ket-qua.pdf", FileTypes.PDF, "%PDF-1.4 ket qua".getBytes(StandardCharsets.UTF_8), office.accountId());
    }

    private long contractOf(long residentId) {
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM contract WHERE resident_id = ? AND status = 'ACTIVE'", Long.class, residentId);
    }

    /** Phụ lục gia hạn đã áp dụng: ngày kết thúc hiện hành của hợp đồng thành {@code newEnd}, phát {@code AddendumApplied}. */
    private long extend(long residentId, LocalDate newEnd) {
        long contractId = contractOf(residentId);
        jdbc.update("UPDATE contract SET current_end_date = ? WHERE id = ?", newEnd, contractId);
        long addendumId = ADDENDA.incrementAndGet();
        transaction.executeWithoutResult(status -> publisher.publishEvent(
                new AddendumApplied(addendumId, contractId, residentId, Set.of("DURATION"), today())));
        return addendumId;
    }

    /** Người nội trú ngắn ngày 14 ngày có Thông báo lưu trú Đã xác nhận; đồng hồ sang ngày sau để nhắc lập không trùng ngày. */
    private ResidenceRegistration shortTermConfirmed() {
        long residentId = readyWith(ResidenceType.SHORT_TERM_RESIDENTIAL, today().plusDays(13));
        completeAdmission(residentId);
        awaitObligations(residentId, 1);
        ResidenceRegistration registration = createFor(residentId);
        assertThat(registration.type()).isEqualTo(RegistrationType.STAY_NOTIFICATION);
        confirm(registration, null, evidence());
        clock.setTo(time.now().plus(Duration.ofDays(1)));
        return registration;
    }

    @Test
    void Q14_extendingAShortStayTo60Days_remindsTheOffice_andTheNewRegistrationIsATemporaryResidenceRegistration() {
        ResidenceRegistration old = shortTermConfirmed();
        long residentId = old.residentId();
        LocalDate start = today().minusDays(1);

        long addendumId = extend(residentId, start.plusDays(59));

        Map<String, Object> obligation = awaitObligations(residentId, 2).getFirst();
        assertThat(obligation.get("trigger_type")).isEqualTo("RENEWAL_ADDENDUM");
        assertThat(((Number) obligation.get("source_id")).longValue()).isEqualTo(addendumId);
        assertThat(dateOf(obligation.get("due_at")))
                .as("lúc áp dụng + CFG-M02-12")
                .isEqualTo(today().plusDays(1));
        assertThat(reminderCount("CREATE", residentId))
                .as(
                        "nhắc lập lúc vào ở và lúc gia hạn (hai ngày khác nhau; listener chạy luồng khác nên đếm khóa nhắc)")
                .isEqualTo(2);
        assertThat(registrationCount(residentId)).as("không tự tạo (Q-306)").isEqualTo(1);

        ResidenceRegistration next = createFor(residentId);
        assertThat(next.type()).isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        confirm(next, start.plusDays(59), evidence());
        assertThat(statusOf(old.id())).isEqualTo("SUPERSEDED");
        assertThat(statusOf(next.id())).isEqualTo("CONFIRMED");
    }

    @Test
    void Q16_extendingTo21Days_keepsAStayNotification_forTheNewEndDate() {
        ResidenceRegistration old = shortTermConfirmed();
        long residentId = old.residentId();

        extend(residentId, today().minusDays(1).plusDays(20));
        awaitObligations(residentId, 2);

        ResidenceRegistration next = createFor(residentId);
        assertThat(next.type()).isEqualTo(RegistrationType.STAY_NOTIFICATION);
        confirm(next, null, evidence());
        assertThat(statusOf(old.id())).isEqualTo("SUPERSEDED");
    }

    @Test
    void FR_007_extendingTheContractOfSomeoneWithAConfirmedTemporaryResidenceRegistration_recordsNothing()
            throws Exception {
        long residentId = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, today().plusDays(99));
        completeAdmission(residentId);
        awaitObligations(residentId, 1);
        confirm(createFor(residentId), today().plusDays(99), evidence());

        extend(residentId, today().plusDays(199));
        Thread.sleep(1500);

        assertThat(obligations(residentId)).hasSize(1);
    }

    @Test
    void FR_007_extendingBeforeAnyRegistration_recordsNothing_theAdmissionObligationCoversIt() throws Exception {
        long residentId = readyWith(ResidenceType.SHORT_TERM_RESIDENTIAL, today().plusDays(13));
        completeAdmission(residentId);
        awaitObligations(residentId, 1);

        extend(residentId, today().plusDays(59));
        Thread.sleep(1500);

        assertThat(obligations(residentId)).hasSize(1);
        assertThat(createFor(residentId).type())
                .as("loại tính theo hợp đồng lúc lập")
                .isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
    }

    @Test
    void Q17_anAddendumMovingADayCareResidentToResidentialCare_recordsAnObligationFromItsEffectiveDate() {
        LocalDate start = today().minusDays(5);
        long residentId = admissionResident();
        confirmedAssessment(residentId, com.cambae.backend.m01.api.dto.CareLevel.BASIC);
        long contractId =
                contract(residentId, ContractStatus.ACTIVE, ResidenceType.DAY_CARE, start, start.plusDays(99));
        completeAdmission(residentId);
        LocalDate effective = today().plusDays(3);
        version(contractId, 1, start, ResidenceType.DAY_CARE, start.plusDays(99));
        version(contractId, 2, effective, ResidenceType.SHORT_TERM_RESIDENTIAL, effective.plusDays(59));
        jdbc.update("UPDATE contract SET current_end_date = ? WHERE id = ?", effective.plusDays(59), contractId);
        long addendumId = ADDENDA.incrementAndGet();

        transaction.executeWithoutResult(status -> publisher.publishEvent(new AddendumApplied(
                addendumId, contractId, residentId, Set.of("RESIDENCE_TYPE", "DURATION"), effective)));

        Map<String, Object> obligation = awaitObligations(residentId, 1).getFirst();
        assertThat(obligation.get("trigger_type")).isEqualTo("RESIDENTIAL_EFFECTIVE");
        assertThat(((Number) obligation.get("source_id")).longValue()).isEqualTo(addendumId);
        assertThat(dateOf(obligation.get("trigger_at"))).isEqualTo(effective);
        assertThat(dateOf(obligation.get("due_at")))
                .as("hạn tính từ ngày hiệu lực phần nội trú, kể cả ở tương lai (Q-336)")
                .isEqualTo(effective.plusDays(1));
        assertThat(createFor(residentId).type())
                .as("60 ngày tính từ ngày hiệu lực phần nội trú")
                .isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
    }

    /** Phiên bản nội dung hợp đồng (004) dựng bằng SQL cho phụ lục đổi loại lưu trú. */
    private void version(long contractId, int sequence, LocalDate from, ResidenceType type, LocalDate end) {
        jdbc.update(
                "INSERT INTO contract_content_version (contract_id, sequence_no, effective_from, content, residence_type,"
                        + " care_level, end_date) VALUES (?, ?, ?, ?, ?, 'BASIC', ?)",
                contractId,
                sequence,
                from,
                "{\"residenceType\":\"%s\",\"careLevel\":\"BASIC\",\"endDate\":\"%s\",\"services\":[]}"
                        .formatted(type.name(), end),
                type.name(),
                end);
    }
}

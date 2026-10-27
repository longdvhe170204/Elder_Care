package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import com.cambae.backend.m02.internal.registration.domain.SubmissionChannel;
import com.cambae.backend.m02.internal.registration.service.RegistrationOverdueReminders;
import com.cambae.backend.m02.internal.registration.service.ResidenceRegistrationService;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.support.TestUsers.TestUser;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * Nền test của 018 US1 (T006): người cao tuổi đi qua Hoàn tất tiếp nhận thật của 001, 004; hợp đồng dựng bằng SQL với ngày
 * kết thúc tùy ý (nội trú ngắn ngày 14 ngày). Job khai báo tắt ở profile test (chờ A, BL-90), việc nhắc quá hạn gọi thẳng
 * {@link RegistrationOverdueReminders}.
 */
@TestPropertySource(
        properties = {"eldercare.jobs.m02-registration.cron=-", "eldercare.jobs.m02-registration.on-startup=false"})
abstract class RegistrationTestBase extends M02TestBase {

    @Autowired
    protected ResidenceRegistrationService registrations;

    @Autowired
    protected RegistrationOverdueReminders overdueReminders;

    @Autowired
    protected TransactionTemplate transaction;

    protected TestUser manager;

    private final List<Long> ownContracts = new ArrayList<>();

    @BeforeEach
    void registrationUsers() {
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        user.set(office.user());
    }

    @AfterEach
    void cancelOwnContracts() {
        ownContracts.forEach(id -> jdbc.update("UPDATE contract SET status = 'CANCELLED' WHERE id = ?", id));
        ownContracts.clear();
    }

    /** Hợp đồng đã ký {@code status} với ngày kết thúc {@code end} ({@code null}: không có); trả mã hợp đồng. */
    protected long contract(
            long residentId, ContractStatus status, ResidenceType type, LocalDate start, LocalDate end) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO contract (resident_id, residence_type, start_date, end_date, current_end_date,"
                                    + " care_level, residency_fee_billing_period, status, signed_date,"
                                    + " signing_representative_id, scan_file_id, created_by, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, 'BASIC', 'MONTH', ?, ?, 1, 1, 1, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, residentId);
                    statement.setString(2, type.name());
                    statement.setObject(3, start);
                    statement.setObject(4, end);
                    statement.setObject(5, end);
                    statement.setString(6, status.name());
                    statement.setObject(7, start);
                    statement.setObject(8, LocalDateTime.now());
                    return statement;
                },
                keys);
        long id = keys.getKey().longValue();
        jdbc.update(
                "UPDATE contract SET contract_number = ? WHERE id = ?",
                "HD-%d-%06d".formatted(start.getYear(), id),
                id);
        ownContracts.add(id);
        return id;
    }

    /** Hồ sơ Đang tiếp nhận có đánh giá mức BASIC và hợp đồng Hiệu lực {@code type} từ hôm nay tới {@code end}. */
    protected long readyWith(ResidenceType type, LocalDate end) {
        long residentId = admissionResident();
        confirmedAssessment(residentId, CareLevel.BASIC);
        contract(residentId, ContractStatus.ACTIVE, type, today(), end);
        return residentId;
    }

    /** Hoàn tất tiếp nhận thật (lệnh 001, tác động 004 phát {@code AdmissionCompleted}). */
    protected void completeAdmission(long residentId) {
        user.set(office.user());
        statusService.completeAdmission(
                ResidentStatusService.target(residentId),
                new Reason("Hoàn tất thử"),
                JsonNodeFactory.instance.objectNode());
    }

    /** Lập khai báo bằng lệnh của Hành chính. */
    protected ResidenceRegistration createFor(long residentId) {
        user.set(office.user());
        return registrations.createRegistration(ResidenceRegistrationService.targetForResident(residentId), residentId);
    }

    /** Ghi nộp rồi ghi được chấp nhận (US2); {@code expiry} chỉ với Đăng ký tạm trú, {@code evidenceFileId} đã tải lên. */
    protected void confirm(ResidenceRegistration registration, LocalDate expiry, long evidenceFileId) {
        user.set(office.user());
        Target target = registrations.targetOf(registration.id());
        registrations.recordSubmitted(
                target,
                registration.id(),
                new ResidenceRegistrationService.Submission(
                        today(), SubmissionChannel.IN_PERSON, "Công an phường X", null));
        registrations.recordAccepted(
                target, registration.id(), new ResidenceRegistrationService.Acceptance(evidenceFileId, expiry));
    }

    /** Trạng thái khai báo. */
    protected String statusOf(long registrationId) {
        return jdbc.queryForObject(
                "SELECT status FROM residence_registration WHERE id = ?", String.class, registrationId);
    }

    /** Nghĩa vụ khai báo của người cao tuổi (bảng kỹ thuật), mới nhất trước. */
    protected List<Map<String, Object>> obligations(long residentId) {
        return jdbc.queryForList(
                "SELECT * FROM registration_obligation WHERE resident_id = ? ORDER BY id DESC", residentId);
    }

    /** Chờ listener sau commit ghi đủ {@code count} nghĩa vụ. */
    protected List<Map<String, Object>> awaitObligations(long residentId, int count) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(obligations(residentId)).hasSize(count));
        return obligations(residentId);
    }

    protected int registrationCount(long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM residence_registration WHERE resident_id = ?", Integer.class, residentId);
    }

    protected int reminderCount(String type, long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM registration_reminder WHERE type = ? AND resident_id = ?",
                Integer.class,
                type,
                residentId);
    }

    /** Ngày theo Asia/Ho_Chi_Minh của cột {@code DATETIME(6)} lưu UTC (DBR-25). */
    protected static LocalDate dateOf(Object dateTime) {
        return ((LocalDateTime) dateTime)
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(BusinessTime.ZONE)
                .toLocalDate();
    }
}

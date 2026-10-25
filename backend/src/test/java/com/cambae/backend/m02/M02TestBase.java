package com.cambae.backend.m02;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m01.AssessmentTestBase;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService;
import com.cambae.backend.m02.internal.transition.service.M02DailyRunner;
import com.cambae.backend.support.fake.m01.FakeStatusTransitionParticipant;
import com.cambae.backend.support.fake.m02.FakeDepositStatuses;
import com.cambae.backend.support.fake.m02.FakeM02Config;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Nền chung cho test của 004 dùng bean tham gia lệnh trạng thái thật của M02 cùng M01 thật. Khác {@code
 * StatusTestBase} của 001: không gỡ {@code m02StatusParticipant}. Một bản giả {@link FakeStatusTransitionParticipant}
 * đứng cho 003 (phân bổ giường) và 012 (điều kiện DBR_02); hợp đồng dựng bằng {@link ContractFixtures} (bảng thật, từ Phase 5), đặt cọc lấy từ bản giả vì 017 chưa có.
 * Người dùng là Hành chính ({@code office}) trừ khi test đổi.
 */
@Import({FakeM02Config.class, M02TestBase.Fakes.class})
public abstract class M02TestBase extends AssessmentTestBase {

    @TestConfiguration
    static class Fakes {

        @Bean
        ContractFixtures contractFixtures(org.springframework.jdbc.core.JdbcTemplate jdbc) {
            return new ContractFixtures(jdbc);
        }

        @Bean
        @Qualifier("otherModules")
        FakeStatusTransitionParticipant otherModules() {
            return new FakeStatusTransitionParticipant()
                    .interestedIn(
                            com.cambae.backend.m01.api.dto.StatusCommand.COMPLETE_ADMISSION,
                            com.cambae.backend.m01.api.dto.StatusCommand.CANCEL_ADMISSION);
        }
    }

    @Autowired
    protected ResidentStatusService statusService;

    @Autowired
    protected ContractFixtures contracts;

    @Autowired
    protected FakeDepositStatuses deposits;

    @Autowired
    @Qualifier("otherModules")
    protected FakeStatusTransitionParticipant otherModules;

    @Autowired
    protected M02DailyRunner dailyRunner;

    @BeforeEach
    void resetM02Fakes() {
        contracts.reset();
        deposits.reset();
        otherModules.reset();
        otherModules.interestedIn(
                com.cambae.backend.m01.api.dto.StatusCommand.COMPLETE_ADMISSION,
                com.cambae.backend.m01.api.dto.StatusCommand.CANCEL_ADMISSION);
    }

    /** Hồ sơ mới ở Đang tiếp nhận. */
    protected long admissionResident() {
        long id = newResident();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        return id;
    }

    /** Đánh giá đầu vào Đã xác nhận với mức {@code level}; người dùng hiện tại trở lại là Hành chính. */
    protected void confirmedAssessment(long residentId, CareLevel level) {
        confirmed(residentId, AssessmentType.INITIAL, level, "Nhận xét đầu vào", "Lưu ý");
        user.set(office.user());
    }

    /** Đủ điều kiện (a) → (f): đánh giá Đã xác nhận mức BASIC, hợp đồng Hiệu lực mức BASIC bắt đầu hôm nay. */
    protected long readyToAdmit(long residentId) {
        confirmedAssessment(residentId, CareLevel.BASIC);
        return contracts.active(residentId, CareLevel.BASIC, time.today());
    }

    protected LocalDate today() {
        return time.today();
    }

    protected ResidentStatus currentStatus(long residentId) {
        return ResidentStatus.valueOf(
                jdbc.queryForObject("SELECT status FROM resident WHERE id = ?", String.class, residentId));
    }

    /** Lỗi nghiệp vụ của lệnh, hoặc null nếu lệnh thành công. */
    protected static BusinessException failure(Runnable command) {
        try {
            command.run();
            return null;
        } catch (BusinessException e) {
            return e;
        }
    }

    /** Mã điều kiện chưa thỏa trong thân lỗi {@code UNMET_CONDITIONS}. */
    @SuppressWarnings("unchecked")
    protected static List<String> unmetCodes(BusinessException e) {
        return ((List<Condition>) e.properties().get("unmetConditions"))
                .stream().map(Condition::code).toList();
    }

    /** Thân lập lượt đăng ký (api-004 {@code AdmissionApplicationInput}). */
    protected String applicationBody(long residentId) {
        return """
                {"residentId":%d,"applicationDate":"%s","contactPerson":"Nguyễn Thị Liên Hệ",
                 "needs":"Cần chăm sóc ban ngày","desiredResidenceType":"LONG_TERM_RESIDENTIAL",
                 "expectedCareLevel":"BASIC","desiredAdmissionDate":"%s","note":"Ghi chú đăng ký"}""".formatted(residentId, today(), today().plusDays(7));
    }

    /** Lượt đăng ký mở của người cao tuổi (tạo thẳng qua dịch vụ HTTP của test gọi lại), trả mã lượt. */
    protected Long applicationIdOf(long residentId) {
        return jdbc.queryForObject(
                "SELECT id FROM admission_application WHERE resident_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                residentId);
    }
}

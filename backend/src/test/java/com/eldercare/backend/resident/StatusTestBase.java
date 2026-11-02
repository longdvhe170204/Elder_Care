package com.eldercare.backend.resident;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.resident.internal.status.service.ResidentStatusService;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.support.fake.resident.FakeAbsences;
import com.eldercare.backend.support.fake.resident.FakeStatusTransitionParticipant;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import com.eldercare.backend.support.fake.roombedallocation.FakeM03Config;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * Nền chung cho test trạng thái người cao tuổi của 001 US4 (T043 → T046): hai bản giả {@link
 * StatusTransitionParticipant} đứng cho 003, 004, 005, 006, 007, 010, 011, 012, 013 (đặt lại trước mỗi test) và bản giả
 * {@link FakeAbsences} của 004. Participant thật của 003 ({@code AllocationStatusParticipant}) cần nguồn lưu trú của 004
 * ({@code AllocationResidencySource}), ở đây dùng bản giả của 003 ({@link FakeM03Config}) để đặt dữ liệu theo test. Hồ sơ dựng bằng
 * {@link #residentIn}: lịch sử trạng thái luôn khớp trạng thái hiện tại.
 */
@Import({StatusTestBase.Fakes.class, FakeM03Config.class})
public abstract class StatusTestBase extends AssessmentTestBase {

    @TestConfiguration
    static class Fakes {

        /**
         * Test của 001 kiểm hành vi của M01 với các bản giả; bean thật của 004 (kiểm {@code details}, điều kiện hợp đồng) không tham gia để
         * các test này không phải dựng thân lệnh của 004. Tích hợp thật của hai module ở {@code AdmissionRealBedIT}, {@code
         * M02StatusParticipantIT}.
         */
        @Bean
        static BeanDefinitionRegistryPostProcessor withoutM02Participant() {
            return registry -> {
                if (registry.containsBeanDefinition("m02StatusParticipant")) {
                    registry.removeBeanDefinition("m02StatusParticipant");
                }
            };
        }

        @Bean
        FakeStatusTransitionParticipant participantOne() {
            return new FakeStatusTransitionParticipant();
        }

        @Bean
        FakeStatusTransitionParticipant participantTwo() {
            return new FakeStatusTransitionParticipant();
        }

        @Bean
        @Primary
        FakeAbsences fakeAbsences() {
            return new FakeAbsences();
        }
    }

    @Autowired
    protected ResidentStatusService statusService;

    @Autowired
    @Qualifier("participantOne")
    protected FakeStatusTransitionParticipant one;

    @Autowired
    @Qualifier("participantTwo")
    protected FakeStatusTransitionParticipant two;

    @Autowired
    protected FakeAbsences absences;

    @Autowired
    protected ResidentStatusApi statusApi;

    @Autowired
    private FakeAllocationResidencySource residency;

    @Autowired
    private TransactionTemplate transactions;

    @BeforeEach
    void resetParticipants() {
        one.reset();
        two.reset();
    }

    /** Lệnh của module khác gọi {@code Propagation.MANDATORY}, nên test mở transaction thay module nguồn. */
    protected <T> T inTransaction(java.util.function.Supplier<T> body) {
        return transactions.execute(status -> body.get());
    }

    protected static Basis sourceRecord(long id) {
        return new Basis(null, null, id, null);
    }

    /** Thân lệnh không có chi tiết. */
    protected static JsonNode noDetails() {
        return JsonNodeFactory.instance.objectNode();
    }

    protected static JsonNode details(String field, long value) {
        var node = JsonNodeFactory.instance.objectNode();
        node.put(field, value);
        return node;
    }

    protected static Reason reason() {
        return new Reason("Lý do thử");
    }

    /**
     * Hồ sơ mới ở Đang tiếp nhận, chỉ có dòng lịch sử Tạo hồ sơ. Loại lưu trú Bán trú ở nguồn giả của 003 để participant
     * của 003 không thêm điều kiện, tác động về giường (test này kiểm phần của M01; phần giường do 003 kiểm).
     */
    protected long admissionResident() {
        long id = newResident();
        jdbc.update("UPDATE resident SET status = 'ADMISSION_IN_PROGRESS' WHERE id = ?", id);
        residency.resident(id, ResidencyType.DAY_CARE, null, null, null, null);
        return id;
    }

    /** Hồ sơ ở trạng thái cho trước; thêm một dòng lịch sử hệ thống để dòng mới nhất luôn khớp trạng thái hiện tại. */
    protected long residentIn(ResidentStatus status) {
        long id = admissionResident();
        moveTo(id, status);
        return id;
    }

    protected void moveTo(long residentId, ResidentStatus status) {
        ResidentStatus from = currentStatus(residentId);
        jdbc.update("UPDATE resident SET status = ? WHERE id = ?", status.name(), residentId);
        jdbc.update(
                "INSERT INTO resident_status_history (resident_id, from_status, to_status, command, occurred_at,"
                        + " performed_by, by_system) VALUES (?, ?, ?, 'SEED', ?, ?, TRUE)",
                residentId,
                from.name(),
                status.name(),
                java.sql.Timestamp.from(time.now()),
                office.accountId());
    }

    protected ResidentStatus currentStatus(long residentId) {
        return ResidentStatus.valueOf(
                jdbc.queryForObject("SELECT status FROM resident WHERE id = ?", String.class, residentId));
    }

    protected int historyCount(long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM resident_status_history WHERE resident_id = ?", Integer.class, residentId);
    }

    /** FR-048: trạng thái hiện tại luôn bằng dòng lịch sử mới nhất. */
    protected ResidentStatus latestHistoryStatus(long residentId) {
        return ResidentStatus.valueOf(jdbc.queryForObject(
                "SELECT to_status FROM resident_status_history WHERE resident_id = ? ORDER BY id DESC LIMIT 1",
                String.class,
                residentId));
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
        return ((List<com.eldercare.backend.resident.api.dto.Condition>)
                        e.properties().get("unmetConditions"))
                .stream()
                        .map(com.eldercare.backend.resident.api.dto.Condition::code)
                        .toList();
    }
}

package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.resident.internal.status.service.ResidentStatusService;
import com.eldercare.backend.support.ResidentHistoryRows;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * 001 T073 (FR-045, FR-047; research H-02 mục 4): chạy lại hai kịch bản của {@code FR_045_ListConditionsIT},
 * {@code FR_047_RollbackEffectsIT} với mọi participant thật của 003 → 013 (không có {@code StatusTestBase.Fakes},
 * {@code FakeM03Config}): lệnh chưa đủ điều kiện liệt kê điều kiện của nhiều module trong một lần, không đổi dữ liệu; một
 * participant lỗi sau các participant thật (Ghi nhận quay về) thì cả lệnh rollback. Hồ sơ dựng thẳng ở trạng thái cần,
 * không có dữ liệu ở module khác.
 */
@Import(T073_RealParticipantsIT.FailsLast.class)
class T073_RealParticipantsIT extends AssessmentTestBase {

    /** Participant cuối chuỗi, chỉ lỗi khi được bật, để mọi participant thật đã áp tác động trước nó. */
    @TestConfiguration
    static class FailsLast {
        static volatile Long failFor;

        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE)
        StatusTransitionParticipant failsLast() {
            return new StatusTransitionParticipant() {
                @Override
                public Set<StatusCommand> interestedCommands() {
                    return Set.of(StatusCommand.RECORD_RETURN);
                }

                @Override
                public List<Condition> checkConditions(CommandContext context) {
                    return List.of();
                }

                @Override
                public void applyEffects(TransitionResult result) {
                    if (Long.valueOf(result.residentId()).equals(failFor))
                        throw new IllegalStateException("module cuối lỗi");
                }
            };
        }
    }

    @Autowired
    ResidentStatusService statusService;

    private long inResidence() {
        return seeded(ResidentStatus.IN_RESIDENCE);
    }

    private long seeded(ResidentStatus status) {
        long id = newResident();
        jdbc.update("UPDATE resident SET status = ? WHERE id = ?", status.name(), id);
        ResidentHistoryRows.insertStatus(
                jdbc,
                null,
                id,
                "ADMISSION_IN_PROGRESS",
                status.name(),
                "SEED",
                java.sql.Timestamp.from(time.now().minusSeconds(3600)),
                office.accountId(),
                true,
                null);
        return id;
    }

    private String status(long id) {
        return jdbc.queryForObject("SELECT status FROM resident WHERE id = ?", String.class, id);
    }

    private int history(long id) {
        return historyRows(id);
    }

    @Test
    void FR_045_realParticipants_listConditionsOfSeveralModulesAtOnce_stateUntouched() {
        long id = inResidence();
        user.set(office.user());
        int rows = history(id);

        BusinessException e = null;
        try {
            var details = JsonNodeFactory.instance.objectNode();
            details.put("residencyEndingId", 999_999_999L);
            details.put("effectiveAt", time.now().toString());
            statusService.endResidency(target(id), new Reason("Kết thúc thử"), details);
        } catch (BusinessException ex) {
            e = ex;
        }

        assertThat(e).isNotNull();
        assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e.errorCode()).isEqualTo("UNMET_CONDITIONS");
        @SuppressWarnings("unchecked")
        List<Condition> unmet = (List<Condition>) e.properties().get("unmetConditions");
        assertThat(unmet.stream().map(Condition::module).filter(m -> m != null).distinct())
                .as("điều kiện của nhiều module trong một lần: %s", unmet)
                .hasSizeGreaterThanOrEqualTo(2);
        assertThat(status(id)).isEqualTo("IN_RESIDENCE");
        assertThat(history(id)).isEqualTo(rows);
    }
}

package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 001 T044 (FR-047; research H-02 mục 3): tác động của mọi module chạy trong cùng transaction với việc đổi trạng thái;
 * một module ném lỗi thì cả lệnh rollback (trạng thái, lịch sử, tác động đã áp, nhật ký), thành công thì có sự kiện.
 */
@Import(FR_047_RollbackEffectsIT.Effect.class)
class FR_047_RollbackEffectsIT extends StatusTestBase {

    /** Tác động có thật trong cơ sở dữ liệu, chạy trước bản giả (không có {@code @Order}): chứng minh rollback. */
    @TestConfiguration
    static class Effect {

        @Bean
        @Order(1)
        StatusTransitionParticipant writesANote(JdbcTemplate jdbc) {
            return new StatusTransitionParticipant() {
                @Override
                public Set<StatusCommand> interestedCommands() {
                    return Set.of(StatusCommand.GRANT_TEMPORARY_ABSENCE);
                }

                @Override
                public List<Condition> checkConditions(CommandContext context) {
                    return List.of();
                }

                @Override
                public void applyEffects(TransitionResult result) {
                    jdbc.update("UPDATE resident SET special_notes = 'EFFECT' WHERE id = ?", result.residentId());
                }
            };
        }
    }

    private String note(long id) {
        return jdbc.queryForObject("SELECT special_notes FROM resident WHERE id = ?", String.class, id);
    }

    private long auditRows(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE resident_id = ?", Long.class, id);
    }

    @Test
    void FR_047_oneParticipantFails_wholeCommandRollsBack() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        two.failOnApply(new IllegalStateException("module khác lỗi"));
        user.set(office.user());
        int rows = historyCount(id);
        long audits = auditRows(id);

        assertThatThrownBy(() -> statusService.grantTemporaryAbsence(target(id), reason(), noDetails()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(historyCount(id)).isEqualTo(rows);
        assertThat(note(id)).as("tác động đã áp trước đó cũng bị hoàn lại").isNull();
        assertThat(auditRows(id)).isEqualTo(audits);
        assertThat(events.stream(ResidentStatusChanged.class).filter(e -> e.residentId() == id))
                .isEmpty();
    }
}

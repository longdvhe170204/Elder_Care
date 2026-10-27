package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m01.api.spi.StatusTransitionParticipant;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * 004 T070 (research T-03; spec 001 H-03): tác động của M02 lên bảy lệnh trạng thái chạy trong transaction của lệnh. Gọi ngoài
 * transaction thì bị từ chối ở cả bảy lệnh (mỗi tác động khóa {@code resident_residency} bằng {@code Propagation.MANDATORY}),
 * nên lệnh của 001 không thể để lại tác động dở của M02: lỗi ở M02 làm cả lệnh rollback.
 */
class M02StatusParticipantIT extends MySqlIntegrationTestBase {

    @Autowired
    List<StatusTransitionParticipant> participants;

    @Autowired
    JdbcTemplate jdbc;

    private StatusTransitionParticipant m02() {
        return participants.stream()
                .filter(p -> p.getClass().getSimpleName().equals("M02StatusParticipant"))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void sevenCommands_areHandled_andEachEffectRequiresTheCommandTransaction() {
        StatusTransitionParticipant participant = m02();
        assertThat(participant.interestedCommands())
                .containsExactlyInAnyOrder(
                        StatusCommand.COMPLETE_ADMISSION,
                        StatusCommand.CANCEL_ADMISSION,
                        StatusCommand.GRANT_TEMPORARY_ABSENCE,
                        StatusCommand.TRANSFER_OUT,
                        StatusCommand.RECORD_RETURN,
                        StatusCommand.END_RESIDENCY,
                        StatusCommand.RECORD_DEATH);
        int before = count("resident_residency");

        for (StatusCommand command : participant.interestedCommands()) {
            TransitionResult result = new TransitionResult(
                    Long.MAX_VALUE,
                    ResidentStatus.IN_RESIDENCE,
                    ResidentStatus.IN_RESIDENCE,
                    command,
                    1L,
                    Instant.now(),
                    JsonNodeFactory.instance.objectNode());
            assertThatThrownBy(() -> participant.applyEffects(result))
                    .as(command.name())
                    .isInstanceOf(IllegalTransactionStateException.class);
        }

        assertThat(count("resident_residency")).isEqualTo(before);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}

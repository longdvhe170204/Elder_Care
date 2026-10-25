package com.cambae.backend.m01.api.spi;

import com.cambae.backend.m01.api.dto.CommandContext;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.ConditionWaiver;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m01.api.dto.Warning;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * SPI: module khác cài để tham gia điều kiện và tác động của lệnh đổi trạng thái (research H-02). Cài: 003, 004, 005,
 * 006, 007, 010, 011, 012, 013. Thứ tự gọi {@link #applyEffects} theo {@code @Order}: 004, 003, 005, 006, 011, 010, 012,
 * 013.
 */
public interface StatusTransitionParticipant {

    Set<StatusCommand> interestedCommands();

    /** Chỉ đọc; rỗng nghĩa là thỏa. */
    List<Condition> checkConditions(CommandContext context);

    /** Chỉ 004. */
    default Optional<ConditionWaiver> waiveConditions(CommandContext context) {
        return Optional.empty();
    }

    /** Lỗi thì 422. */
    default void validateDetails(StatusCommand command, JsonNode details) {}

    /** {@code Propagation.MANDATORY}; ném lỗi là rollback cả lệnh (FR-047). */
    void applyEffects(TransitionResult result);

    /** Cảnh báo không chặn, trả kèm điều kiện và phản hồi lệnh (003 FR-024a). */
    default List<Warning> warnings(CommandContext context) {
        return List.of();
    }
}

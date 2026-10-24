package com.cambae.backend.support.fake.m01;

import com.cambae.backend.m01.api.dto.CommandContext;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.ConditionWaiver;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m01.api.spi.StatusTransitionParticipant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Bản giả {@link StatusTransitionParticipant} cho 003, 004, 005, 006, 007, 010, 011, 012, 013 (CHK035): mặc định Đạt;
 * test đặt điều kiện chưa thỏa, hoặc cho {@code applyEffects} ném lỗi để chứng minh rollback (FR-047).
 */
public class FakeStatusTransitionParticipant implements StatusTransitionParticipant {

    private final Set<StatusCommand> commands = EnumSet.allOf(StatusCommand.class);
    private final List<Condition> unmet = new ArrayList<>();
    private final List<TransitionResult> applied = new ArrayList<>();
    private RuntimeException failure;
    private ConditionWaiver waiver;

    public FakeStatusTransitionParticipant interestedIn(StatusCommand... only) {
        commands.clear();
        commands.addAll(Set.of(only));
        return this;
    }

    public FakeStatusTransitionParticipant unmet(Condition condition) {
        unmet.add(condition);
        return this;
    }

    /** Giả lập 004: yêu cầu ngoại lệ đã duyệt miễn các điều kiện ghi trong {@code waiver}. */
    public FakeStatusTransitionParticipant waive(ConditionWaiver waiver) {
        this.waiver = waiver;
        return this;
    }

    /** Đưa về mặc định (bean dùng chung giữa các test trong một context). */
    public FakeStatusTransitionParticipant reset() {
        commands.clear();
        commands.addAll(EnumSet.allOf(StatusCommand.class));
        unmet.clear();
        applied.clear();
        failure = null;
        waiver = null;
        return this;
    }

    public FakeStatusTransitionParticipant failOnApply(RuntimeException error) {
        this.failure = error;
        return this;
    }

    /** Các kết quả đã áp tác động, theo thứ tự gọi. */
    public List<TransitionResult> applied() {
        return List.copyOf(applied);
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.copyOf(commands);
    }

    @Override
    public List<Condition> checkConditions(CommandContext context) {
        return List.copyOf(unmet);
    }

    @Override
    public Optional<ConditionWaiver> waiveConditions(CommandContext context) {
        return Optional.ofNullable(waiver);
    }

    @Override
    public void applyEffects(TransitionResult result) {
        if (failure != null) {
            throw failure;
        }
        applied.add(result);
    }
}

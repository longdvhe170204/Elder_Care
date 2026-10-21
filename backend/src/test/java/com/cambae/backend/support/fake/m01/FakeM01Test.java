package com.cambae.backend.support.fake.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m01.api.dto.CommandContext;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.OpenAbsence;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidenceTypePeriod;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.TransitionResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 001 T002: bản giả các SPI M01 khai báo cho module khác cài. */
class FakeM01Test {

    private static final Instant T = Instant.parse("2026-10-21T01:00:00Z");

    private static CommandContext context(StatusCommand command) {
        return new CommandContext(1, ResidentStatus.IN_RESIDENCE, command, null, T, null);
    }

    private static TransitionResult result(StatusCommand command) {
        return new TransitionResult(1, ResidentStatus.IN_RESIDENCE, ResidentStatus.HOSPITALIZED, command, 9, T, null);
    }

    @Test
    void CHK035_participant_passesByDefaultAndReportsConfiguredUnmetConditions() {
        FakeStatusTransitionParticipant participant = new FakeStatusTransitionParticipant();
        assertThat(participant.checkConditions(context(StatusCommand.TRANSFER_OUT)))
                .isEmpty();

        Condition unmet = new Condition("BED_NOT_RELEASED", "Giường chưa trả", "BR-M03-01", false);
        participant.unmet(unmet);

        assertThat(participant.checkConditions(context(StatusCommand.TRANSFER_OUT)))
                .containsExactly(unmet);
    }

    @Test
    void CHK035_participant_interestedOnlyInConfiguredCommands() {
        FakeStatusTransitionParticipant participant =
                new FakeStatusTransitionParticipant().interestedIn(StatusCommand.RECORD_DEATH);

        assertThat(participant.interestedCommands()).containsExactly(StatusCommand.RECORD_DEATH);
    }

    @Test
    void FR_047_participant_failOnApplyThrowsSoCallerCanRollBack() {
        FakeStatusTransitionParticipant participant =
                new FakeStatusTransitionParticipant().failOnApply(new IllegalStateException("effect failed"));

        assertThatThrownBy(() -> participant.applyEffects(result(StatusCommand.TRANSFER_OUT)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(participant.applied()).isEmpty();
    }

    @Test
    void FR_047_participant_recordsAppliedResultsInOrder() {
        FakeStatusTransitionParticipant participant = new FakeStatusTransitionParticipant();

        participant.applyEffects(result(StatusCommand.TRANSFER_OUT));
        participant.applyEffects(result(StatusCommand.RECORD_DEATH));

        assertThat(participant.applied())
                .extracting(TransitionResult::command)
                .containsExactly(StatusCommand.TRANSFER_OUT, StatusCommand.RECORD_DEATH);
    }

    @Test
    void FR_036a_careLevelChangeRequest_assessmentSupersedesOpenRequests() {
        FakeCareLevelChangeRequests requests =
                new FakeCareLevelChangeRequests().open(1, 70).open(1, 71);

        assertThat(requests.hasOpenRequest(1)).isTrue();
        assertThat(requests.supersedeByAssessment(1, 5)).containsExactly(70L, 71L);
        assertThat(requests.hasOpenRequest(1)).isFalse();
        assertThat(requests.supersedeByAssessment(1, 6)).isEmpty();
    }

    @Test
    void FR_052_contract_residenceTypeFollowsPeriodsWithExclusiveEnd() {
        FakeContracts contracts = new FakeContracts()
                .period(
                        1,
                        new ResidenceTypePeriod(
                                ResidenceType.SHORT_TERM_RESIDENTIAL,
                                LocalDate.of(2026, 10, 1),
                                Optional.of(LocalDate.of(2026, 10, 15))))
                .period(
                        1,
                        new ResidenceTypePeriod(
                                ResidenceType.LONG_TERM_RESIDENTIAL, LocalDate.of(2026, 10, 15), Optional.empty()));

        assertThat(contracts.currentResidenceType(1, LocalDate.of(2026, 10, 14)))
                .contains(ResidenceType.SHORT_TERM_RESIDENTIAL);
        assertThat(contracts.currentResidenceType(1, LocalDate.of(2026, 10, 15)))
                .contains(ResidenceType.LONG_TERM_RESIDENTIAL);
        assertThat(contracts.currentResidenceType(1, LocalDate.of(2026, 9, 30))).isEmpty();
        assertThat(contracts.residenceTypeHistory(1)).hasSize(2);
    }

    @Test
    void FR_050_absence_openUntilClosed() {
        FakeAbsences absences = new FakeAbsences().open(1, new OpenAbsence(3, T.plusSeconds(3600)));

        assertThat(absences.open(1)).map(OpenAbsence::absenceId).contains(3L);

        absences.close(1);

        assertThat(absences.open(1)).isEmpty();
    }
}

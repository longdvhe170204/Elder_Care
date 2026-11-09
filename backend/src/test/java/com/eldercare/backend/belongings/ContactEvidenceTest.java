package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.belongings.internal.returns.domain.ContactChannel;
import com.eldercare.backend.belongings.internal.returns.domain.ContactEvidence;
import com.eldercare.backend.belongings.internal.returns.domain.ContactEvidence.Attempt;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 013 T016 ({@code DuBangChungLienHeTest}; FR-017a (a), CFG-M12-06, Q-157; quickstart Q19): bằng chứng liên hệ. */
class ContactEvidenceTest {

    private static Attempt attempt(long member, ContactChannel channel, String result) {
        return new Attempt(member, channel, result);
    }

    @Test
    void everyRepresentative_needsEnoughAttemptsOverEnoughChannels() {
        var enough = List.of(
                attempt(1, ContactChannel.PHONE_CALL, "không nghe máy"),
                attempt(1, ContactChannel.TEXT_MESSAGE, "chưa trả lời"),
                attempt(2, ContactChannel.PHONE_CALL, "máy bận"),
                attempt(2, ContactChannel.PORTAL_NOTIFICATION, "chưa đọc"));

        assertThat(ContactEvidence.sufficient(List.of(1L, 2L), enough, 2, 2)).isTrue();
        assertThat(ContactEvidence.sufficient(List.of(1L, 2L, 3L), enough, 2, 2))
                .as("người đại diện thứ ba chưa được liên hệ")
                .isFalse();
    }

    @Test
    void sameChannelTwice_isNotEnough() {
        var attempts = List.of(
                attempt(1, ContactChannel.PHONE_CALL, "không nghe máy"),
                attempt(1, ContactChannel.PHONE_CALL, "không nghe máy lần hai"));

        assertThat(ContactEvidence.sufficient(List.of(1L), attempts, 2, 2)).isFalse();
    }

    @Test
    void attemptWithoutResult_isNotCounted() {
        var attempts = List.of(
                attempt(1, ContactChannel.PHONE_CALL, "không nghe máy"), attempt(1, ContactChannel.TEXT_MESSAGE, " "));

        assertThat(ContactEvidence.sufficient(List.of(1L), attempts, 2, 2)).isFalse();
    }

    @Test
    void noEffectiveRepresentative_hasNoGroundA() {
        assertThat(ContactEvidence.sufficient(List.of(), List.of(), 2, 2)).isFalse();
    }
}

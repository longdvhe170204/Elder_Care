package com.cambae.backend.support.fake.m13;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.notification.FamilyRight;
import com.cambae.backend.m13.api.spi.FamilyContactView;
import com.cambae.backend.m13.api.spi.NotifiableRelationshipView;
import com.cambae.backend.m13.api.spi.SmsResult;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 009 T003: bản giả trả đúng dữ liệu test đặt; nhà cung cấp tin nhắn giả ghi lại tin và chạy theo kịch bản. */
class FakeM13Test {

    @Test
    void familyNotifications_returnWhatTestSet_andEmptyOtherwise() {
        FakeFamilyNotifications fake = new FakeFamilyNotifications();
        NotifiableRelationshipView m = new NotifiableRelationshipView(
                7L, true, false, Set.of(FamilyRight.RECEIVE_URGENT), Optional.of(70L), true, Optional.of("0901"));
        fake.relate(1L, m)
                .callOrder(1L, List.of(7L))
                .contact(new FamilyContactView(7L, "M", Optional.of("0901"), Optional.of(70L), true));

        assertThat(fake.activeRelationships(1L)).containsExactly(m);
        assertThat(fake.callOrder(1L)).containsExactly(7L);
        assertThat(fake.contact(7L)).isPresent();
        assertThat(fake.activeRelationships(2L)).isEmpty();
        assertThat(fake.contact(8L)).isEmpty();
    }

    @Test
    void smsProvider_recordsSent_andFollowsScript() {
        FakeSmsProvider sms = new FakeSmsProvider();
        sms.thenReturn(SmsResult.of(SmsResult.Outcome.TEMPORARY_FAILURE));

        assertThat(sms.send("0901", "a").outcome()).isEqualTo(SmsResult.Outcome.TEMPORARY_FAILURE);
        assertThat(sms.send("0901", "b").outcome()).isEqualTo(SmsResult.Outcome.SENT);
        assertThat(sms.sent()).extracting(FakeSmsProvider.SentSms::content).containsExactly("a", "b");
    }
}

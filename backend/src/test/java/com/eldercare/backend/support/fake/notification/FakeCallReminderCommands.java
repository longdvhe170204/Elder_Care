package com.eldercare.backend.support.fake.notification;

import com.eldercare.backend.notification.api.CallReminderCommandApi;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Bản giả {@link CallReminderCommandApi} (009 T019): chỉ ghi lời gọi, không dừng lượt nào. */
public class FakeCallReminderCommands implements CallReminderCommandApi {

    /** Một lời gọi: người thân, người cao tuổi. */
    public record Call(long familyMemberId, long residentId) {}

    private final List<Call> calls = new ArrayList<>();

    @Override
    public void calleeEligibilityChanged(long familyMemberId, long residentId) {
        calls.add(new Call(familyMemberId, residentId));
    }

    public List<Call> calls() {
        return List.copyOf(calls);
    }

    public void reset() {
        calls.clear();
    }

    /** {@code @Import(FakeCallReminderCommands.Config.class)} để thay bản thật trong context của test. */
    @TestConfiguration
    public static class Config {
        @Bean
        @Primary
        FakeCallReminderCommands fakeCallReminderCommands() {
            return new FakeCallReminderCommands();
        }
    }
}

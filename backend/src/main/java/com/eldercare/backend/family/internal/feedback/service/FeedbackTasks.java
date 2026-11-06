package com.eldercare.backend.family.internal.feedback.service;

import com.eldercare.backend.family.internal.feedback.persistence.FeedbackStore;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import java.time.Instant;
import java.util.function.ToIntFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Việc theo phút của phản hồi (research F-13; FR-063, FR-064; SC-005 ≤ 5 phút): quá hạn chưa Đã phản hồi thì gắn dấu,
 * leo thang Quản lý viện một lần mỗi hạn; Đã phản hồi quá CFG-M10-02 thì tự đóng. Mỗi phản hồi một transaction.
 */
@Configuration
class FeedbackTasks {

    @Bean
    M10TimedTask feedbackEscalation(FeedbackStore store, FeedbackService feedback) {
        return task("FEEDBACK_ESCALATION", at -> (int) store.dueForEscalation(at).stream()
                .filter(id -> feedback.escalateIfDue(id, at))
                .count());
    }

    @Bean
    M10TimedTask feedbackAutoClose(FeedbackStore store, FeedbackRules rules, FeedbackService feedback) {
        return task("FEEDBACK_AUTO_CLOSE", at -> (int) store.repliedBefore(at.minus(rules.autoClose())).stream()
                .filter(id -> feedback.autoCloseIfDue(id, at))
                .count());
    }

    private static M10TimedTask task(String name, ToIntFunction<Instant> run) {
        return new M10TimedTask() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Cadence cadence() {
                return Cadence.MINUTE;
            }

            @Override
            public int run(Instant scheduledAt, Instant actualAt) {
                return run.applyAsInt(actualAt);
            }
        };
    }
}

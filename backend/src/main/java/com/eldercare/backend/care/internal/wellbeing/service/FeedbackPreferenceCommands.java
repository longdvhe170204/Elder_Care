package com.eldercare.backend.care.internal.wellbeing.service;

import com.eldercare.backend.care.api.FeedbackPreferenceCommandApi;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.events.OpinionRecordedFromFeedback;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T033: dùng cùng lệnh và luật sở thích, sự kiện nằm trong transaction của caller 012. */
@Service
public class FeedbackPreferenceCommands implements FeedbackPreferenceCommandApi {
    private final PreferenceService preferences;
    private final ApplicationEventPublisher events;

    public FeedbackPreferenceCommands(PreferenceService preferences, ApplicationEventPublisher events) {
        this.preferences = preferences;
        this.events = events;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long recordFromFeedback(
            long residentId,
            long feedbackId,
            Long groupId,
            String description,
            String level,
            OpinionSource source,
            boolean confirmReplacement) {
        long id = preferences.recordFeedback(
                PreferenceService.target(residentId),
                residentId,
                feedbackId,
                groupId,
                description,
                level,
                source,
                confirmReplacement);
        events.publishEvent(new OpinionRecordedFromFeedback(
                feedbackId,
                OpinionRecordedFromFeedback.SOURCE_ACTIVITY_MODULE,
                OpinionRecordedFromFeedback.ITEM_INTEREST,
                id,
                residentId,
                source));
        return id;
    }
}

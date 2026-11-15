package com.eldercare.backend.notification.internal.urgent.service;

import com.eldercare.backend.notification.api.CallReminderCommandApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Bản cài {@link CallReminderCommandApi} (009 T019). */
@Service
@Transactional(propagation = Propagation.MANDATORY)
class CallReminderCommands implements CallReminderCommandApi {

    private final CallResultService results;

    CallReminderCommands(CallResultService results) {
        this.results = results;
    }

    @Override
    public void calleeEligibilityChanged(long familyMemberId, long residentId) {
        results.stopIfNotEligible(familyMemberId, residentId);
    }
}

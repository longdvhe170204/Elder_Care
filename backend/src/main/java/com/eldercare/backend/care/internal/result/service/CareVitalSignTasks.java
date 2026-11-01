package com.eldercare.backend.care.internal.result.service;

import com.eldercare.backend.care.api.VitalSignTaskApi;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service("careVitalSignTasks")
@Transactional(propagation = Propagation.MANDATORY)
public class CareVitalSignTasks implements VitalSignTaskApi {
    private final CareResultService commands;

    public CareVitalSignTasks(CareResultService commands) {
        this.commands = commands;
    }

    @Override
    public void confirmed(long taskId, long vitalSignRecordId, Instant measuredAt, long measuredById) {
        commands.confirmed(taskId, vitalSignRecordId, measuredAt, measuredById);
    }
}

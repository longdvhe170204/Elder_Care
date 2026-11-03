package com.eldercare.backend.care.internal.checklist.domain;

import com.eldercare.backend.care.api.spi.*;
import com.eldercare.backend.care.internal.catalog.domain.NonPerformanceReasonType;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.resident.api.dto.IdentityView;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.util.*;

public record CareChecklist(
        long shiftId,
        Instant loadedAt,
        List<CareTask> tasks,
        Map<Long, IdentityView> identities,
        Map<Long, LocationView> locations,
        Map<Long, Optional<String>> careNotes,
        List<ChecklistRiskAgreement> agreements,
        List<NonPerformanceReasonType> reasons,
        List<MedicationChecklistPort.DoseView> medicationDoses,
        List<AlertReadPort.AlertView> openAlerts,
        ShiftStaffPort.HandoverTasksView handoverTasks) {}

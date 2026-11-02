package com.eldercare.backend.resident.internal.status.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.StatusPeriod;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/** Lệnh đổi trạng thái từ module khác (014, 007, 004) và xem trước điều kiện cho 004 (research H-03). */
@Service
class ResidentStatusApiImpl implements ResidentStatusApi {

    private final ResidentStatusService status;
    private final com.eldercare.backend.resident.internal.status.persistence.ResidentStatusHistoryRepository histories;

    ResidentStatusApiImpl(
            ResidentStatusService status,
            com.eldercare.backend.resident.internal.status.persistence.ResidentStatusHistoryRepository histories) {
        this.status = status;
        this.histories = histories;
    }

    @Override
    public TransitionResult execute(
            long residentId,
            StatusCommand command,
            CurrentUser user,
            Optional<Reason> reason,
            JsonNode details,
            Basis basis) {
        return status.executeFromModule(residentId, command, user, reason, details, basis);
    }

    @Override
    public List<Condition> previewConditions(long residentId, StatusCommand command) {
        return status.preview(residentId, command);
    }

    @Override
    public List<StatusPeriod> statusPeriods(long residentId, Set<ResidentStatus> statuses, Instant from, Instant to) {
        if (statuses.isEmpty() || !from.isBefore(to)) return List.of();
        var entries = histories.findByResidentIdOrderByIdAsc(residentId).stream()
                .sorted(java.util.Comparator.comparing(
                        com.eldercare.backend.resident.internal.status.domain.ResidentStatusHistory::getOccurredAt))
                .toList();
        var result = new java.util.ArrayList<StatusPeriod>();
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            Instant end = i + 1 < entries.size() ? entries.get(i + 1).getOccurredAt() : null;
            if (statuses.contains(entry.getToStatus())
                    && entry.getOccurredAt().isBefore(to)
                    && (end == null || end.isAfter(from))
                    && (end == null || end.isAfter(entry.getOccurredAt())))
                result.add(new StatusPeriod(entry.getToStatus(), entry.getOccurredAt(), Optional.ofNullable(end)));
        }
        return List.copyOf(result);
    }
}

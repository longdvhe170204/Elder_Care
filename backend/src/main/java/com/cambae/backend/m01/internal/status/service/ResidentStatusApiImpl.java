package com.cambae.backend.m01.internal.status.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FileView;
import com.cambae.backend.m01.api.ResidentStatusApi;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.StatusPeriod;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/** Lệnh đổi trạng thái từ module khác (014, 007, 004) và xem trước điều kiện cho 004 (research H-03). */
@Service
class ResidentStatusApiImpl implements ResidentStatusApi {

    private final ResidentStatusService status;
    private final FileApi files;

    ResidentStatusApiImpl(ResidentStatusService status, FileApi files) {
        this.status = status;
        this.files = files;
    }

    @Override
    public List<FileView> hospitalDocuments(long historyId) {
        return files.attachedTo(ResidentStatusService.HOSPITAL_DOCUMENT_OWNER, historyId);
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
    @Transactional(readOnly = true)
    public List<StatusPeriod> statusPeriods(long residentId, Set<ResidentStatus> statuses, Instant from, Instant to) {
        return status.periods(residentId, statuses, from, to);
    }
}

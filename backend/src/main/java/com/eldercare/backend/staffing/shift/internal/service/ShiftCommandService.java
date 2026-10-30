package com.eldercare.backend.staffing.shift.internal.service;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.staffing.shift.internal.service.port.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Ranh giới transaction cho port 015; REST gọi PostPublicationService trực tiếp, không gọi port. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ShiftCommandService implements ShiftCommandPort {
    private final PostPublicationService commands;

    public ShiftCommandService(PostPublicationService commands) {
        this.commands = commands;
    }

    public List<PublicationWarning> addToShift(long shift, long staff, ShiftEntryBasis basis, boolean confirmed) {
        return commands.addFromRequest(shift, staff, basis, confirmed);
    }

    public void leaveShift(long shift, long staff, ShiftDepartureBasis basis) {
        commands.leaveFromRequest(shift, staff, basis);
    }

    public void changeLead(long shift, long staff, Basis basis) {
        commands.leadFromRequest(shift, staff, basis);
    }

    public void markMissingLead(long shift, Basis basis) {
        commands.missingFromRequest(shift, basis);
    }
}

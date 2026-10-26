package com.cambae.backend.m09.assignment.internal.service;

import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.m09.assignment.internal.domain.AssignmentInput;
import com.cambae.backend.m09.assignment.internal.service.port.AssignmentCommandPort;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class AssignmentCommandService implements AssignmentCommandPort {
    private final AssignmentService commands;

    public AssignmentCommandService(AssignmentService commands) {
        this.commands = commands;
    }

    public List<UnmetCondition> validate(long shift, AssignmentInput input) {
        return commands.validateFromRequest(shift, input);
    }

    public long create(long shift, AssignmentInput input, Basis basis) {
        return commands.createFromRequest(shift, input, basis);
    }

    public void cancel(long assignment, Basis basis) {
        commands.cancelFromRequest(assignment, basis);
    }
}

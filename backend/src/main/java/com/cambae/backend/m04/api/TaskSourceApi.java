package com.cambae.backend.m04.api;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m04.api.dto.*;
import com.cambae.backend.m04.api.spi.TaskSourcePort.SourceType;
import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.*;

/** T003: contracts 005 mục 1.2–1.10a; hành vi do story tương ứng cài. */
@Transactional(propagation = Propagation.MANDATORY)
public interface TaskSourceApi {
    void sourceCancelled(SourceType type, long sourceId, Reason reason);

    void sourceChanged(SourceType type, long sourceId);
}

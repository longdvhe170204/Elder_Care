package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.*;
import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.*;

/** T003: contracts 005 mục 1.2–1.10a; hành vi do story tương ứng cài. */
@Transactional(propagation = Propagation.MANDATORY)
public interface VitalSignTaskApi {
    void confirmed(long taskId, long vitalSignRecordId, Instant measuredAt, long measuredById);
}

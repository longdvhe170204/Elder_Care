package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.*;
import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.*;

/** T003: contracts 005 mục 1.2–1.10a; hành vi do story tương ứng cài. */
public interface HandoverTaskApi {
    List<HandoverTaskView> unclosed(long floorAreaId, long shiftId);

    Map<Long, TaskStateView> currentStates(Collection<Long> taskIds);

    @Transactional(propagation = Propagation.MANDATORY)
    void recordedInHandover(long handoverId, Collection<Long> taskIds);
}

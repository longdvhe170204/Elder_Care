package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.common.Reason;
import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.*;

/** T003: contracts 005 mục 1.2–1.10a; hành vi do story tương ứng cài. */
@Transactional(propagation = Propagation.MANDATORY)
public interface TaskSourceApi {
    void sourceCancelled(SourceType type, long sourceId, Reason reason);

    /** Căn cứ bị hủy cho riêng một người cao tuổi (bữa của người đó bị bỏ vì vắng, 011 FR-042); như hàm trên. */
    void sourceCancelled(SourceType type, long sourceId, long residentId, Reason reason);

    void sourceChanged(SourceType type, long sourceId);
}

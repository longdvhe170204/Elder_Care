package com.eldercare.backend.archfixture.datagroup;

import com.eldercare.backend.common.data.DataGroup;
import java.time.Instant;

/** Mẫu đúng cho {@code DataGroupArchTest}: entity nhiều nhóm có trường {@code lockedAt} (research R-05). */
@DataGroup(DataGroup.Group.MULTIPLE)
public class MultipleGroupWithLockedAt {

    private Instant lockedAt;

    Instant lockedAt() {
        return lockedAt;
    }
}

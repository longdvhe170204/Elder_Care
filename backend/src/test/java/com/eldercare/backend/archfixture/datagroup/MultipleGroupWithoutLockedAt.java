package com.eldercare.backend.archfixture.datagroup;

import com.eldercare.backend.common.data.DataGroup;

/** Mẫu sai cho {@code DataGroupArchTest}: entity nhiều nhóm thiếu trường {@code lockedAt} (research R-05). */
@DataGroup(DataGroup.Group.MULTIPLE)
public class MultipleGroupWithoutLockedAt {

    private String status;

    String status() {
        return status;
    }
}

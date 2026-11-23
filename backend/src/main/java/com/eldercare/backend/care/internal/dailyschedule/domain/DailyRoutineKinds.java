package com.eldercare.backend.care.internal.dailyschedule.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/** 005 FR-056: danh mục nhóm 1 của M04; phiên bản cũ phải còn để hiện lịch các ngày trước nên không cho xóa. */
public final class DailyRoutineKinds {
    public static final SharedKind<DailyRoutineAttributes> DAILY_ROUTINES =
            SharedKind.of("M04.DAILY_ROUTINE", DailyRoutineAttributes.class).notDeletable();

    private DailyRoutineKinds() {}
}

package com.cambae.backend.m15.account.api.spi;

import com.cambae.backend.m15.account.api.CurrentUser;

/** FR-053: 002 hỏi 008 về quyền đọc lý do vắng, không truy cập bảng nội bộ của M09. */
public interface StaffAbsenceApi {
    boolean canViewReason(CurrentUser user, long absenceId);
}

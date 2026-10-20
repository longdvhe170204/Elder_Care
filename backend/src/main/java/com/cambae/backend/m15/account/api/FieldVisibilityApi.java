package com.cambae.backend.m15.account.api;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.dto.Visibility;

/** Quyền xem theo nhóm thông tin (mục 19.3; FR-049, BR-M15-11). 000 khai báo trước (000 T025, contracts/module-interfaces.md mục 2); feature 002 cài bản thật. */
public interface FieldVisibilityApi {

    Visibility canView(CurrentUser user, InfoGroup group, Target target);
}

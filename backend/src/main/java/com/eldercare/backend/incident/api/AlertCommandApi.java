package com.eldercare.backend.incident.api;

import com.eldercare.backend.administration.account.api.CurrentUser;

/** Tiếp nhận như REST trong transaction của 009 (FR-026a, hợp đồng mục 1.6). */
public interface AlertCommandApi {
    void acknowledge(long alertId, CurrentUser user);
}

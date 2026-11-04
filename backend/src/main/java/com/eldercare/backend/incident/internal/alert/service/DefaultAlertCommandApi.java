package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.api.AlertCommandApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản thật "Tiếp nhận" cảnh báo cho thao tác "Xác nhận và tiếp nhận" của 009 (FR-026a, Q-102; module-interfaces 007
 * mục 1.6): cùng kiểm tra như REST, bắt buộc chạy trong transaction của lệnh gọi.
 */
@Service
public class DefaultAlertCommandApi implements AlertCommandApi {
    private final AlertCommandService alerts;

    public DefaultAlertCommandApi(AlertCommandService alerts) {
        this.alerts = alerts;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void acknowledge(long alertId, CurrentUser user) {
        alerts.receive(alertId, user);
    }
}

/**
 * Giao diện công khai của M13 (009 contracts/module-interfaces.md mục 1.2): tra cứu thông báo theo bản ghi nguồn cho
 * 000, 007, 012, 016. Gửi thông báo không qua gói này mà qua sự kiện cài
 * {@link com.cambae.backend.common.notification.HasNotificationRequests} (research B-02).
 */
@NamedInterface("notification-api")
package com.cambae.backend.m13.api;

import org.springframework.modulith.NamedInterface;

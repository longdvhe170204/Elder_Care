/**
 * Giao diện công khai của M13 (009 contracts/module-interfaces.md mục 1.2): tra cứu thông báo theo bản ghi nguồn cho
 * 000, 007, 012, 016. Gửi thông báo không qua gói này mà qua sự kiện cài
 * {@link com.eldercare.backend.common.notification.HasNotificationRequests} (research B-02).
 */
@NamedInterface("notification-api")
package com.eldercare.backend.notification.api;

import org.springframework.modulith.NamedInterface;

/**
 * SPI M13 khai báo (009 contracts/module-interfaces.md mục 2.1): {@link FamilyNotificationPort} do 012 cài;
 * {@link SmsProvider} là cổng tới nhà cung cấp tin nhắn, chỉ M13 cài và gọi (Q-06); {@link SourceRecordAccessPort} do module
 * nguồn cài để hộp thông báo biết người xem mở được bản ghi nguồn không.
 */
@NamedInterface("notification-spi")
package com.eldercare.backend.notification.api.spi;

import org.springframework.modulith.NamedInterface;

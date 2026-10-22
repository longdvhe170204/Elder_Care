package com.cambae.backend.m13.api.events;

import com.cambae.backend.common.notification.SourceRecord;

/** Mọi lượt nhắc gọi của thông báo Khẩn cấp không liên lạc được ({@code ThongBaoKhongLienLacDuoc}; FR-044). */
public record NotificationUnreachable(long notificationId, SourceRecord sourceRecord) {}

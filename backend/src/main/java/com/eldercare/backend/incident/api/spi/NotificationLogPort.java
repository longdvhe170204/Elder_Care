package com.eldercare.backend.incident.api.spi;

import com.eldercare.backend.incident.api.dto.IncidentNotificationView;
import java.util.List;

/** NhatKyThongBaoPort (007 mục 2.1), do 009 cài; chỉ đọc nhật ký theo đúng loại và mã nguồn. */
public interface NotificationLogPort {
    List<IncidentNotificationView> bySource(String sourceRecordType, long sourceRecordId);
}

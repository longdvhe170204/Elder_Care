package com.eldercare.backend.incident.api.spi;

import com.eldercare.backend.incident.api.dto.EmergencyFamilyContactView;
import com.eldercare.backend.incident.api.dto.VisitTraceView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** NguoiThanKhanCapPort (007 mục 2.1), do 012 cài; chỉ đọc dữ liệu nguồn. */
public interface EmergencyFamilyPort {
    Optional<EmergencyFamilyContactView> primaryContact(long residentId);

    /** Giữ thứ tự liên hệ do 012 cung cấp. */
    List<EmergencyFamilyContactView> representatives(long residentId);

    /** Lượt thăm Đã vào hoặc Đã ra có thời gian chồng khoảng [from, to). */
    List<VisitTraceView> visitsInRange(long residentId, Instant from, Instant to);
}

package com.eldercare.backend.incident.api.spi;

import com.eldercare.backend.incident.api.dto.SosTripView;
import java.util.Optional;

/** ChuyenDiSosPort (007 mục 2.1; FR-047e, 014 FR-068a), do 014 cài; chỉ đọc. Thiếu bean thì SOS chỉ báo nhóm FR-047a. */
public interface SosTripPort {
    /** Chuyến đang diễn ra của người cao tuổi đang ở Hoạt động bên ngoài; rỗng khi không có. */
    Optional<SosTripView> ongoing(long residentId);
}

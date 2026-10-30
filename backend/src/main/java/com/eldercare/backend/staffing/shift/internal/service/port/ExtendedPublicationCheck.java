package com.eldercare.backend.staffing.shift.internal.service.port;

import java.util.List;

/** SPI nội bộ N-07: 015 cài, 008 gọi khi xem trước/công bố; không có bean thì bỏ qua phần kiểm tra của 015. */
public interface ExtendedPublicationCheck {

    List<PublicationWarning> check(long scheduleId);
}

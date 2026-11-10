package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.ResidentActivitySessionView;
import java.time.LocalDate;
import java.util.List;

/** FR-068a: 012 đọc buổi của tầng và toàn viện, không có dữ liệu đánh giá sức khỏe. */
public interface ResidentActivityReadApi {
    List<ResidentActivitySessionView> upcoming(long residentId, LocalDate from, LocalDate to);
}

package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.SourceTaskView;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Đọc công việc theo căn cứ (005 mục 1.6a); quyền xem do module giữ căn cứ kiểm trước khi gọi. */
public interface SourceTaskReadApi {
    /** Công việc chưa hủy của một căn cứ, theo thời điểm dự kiến. */
    List<SourceTaskView> bySource(Set<SourceType> types, long sourceId);

    /** Các căn cứ còn ít nhất một công việc chưa đóng (Chưa đến hạn, Đến hạn, Quá hạn). */
    Set<Long> withOpenTask(Set<SourceType> types, Collection<Long> sourceIds);
}

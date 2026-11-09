package com.eldercare.backend.roombedallocation.api.spi;

import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementSourceType;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import java.util.List;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 contracts mục 2.1 (YeuCauMuaSamPort): M03 khai, M11 cài, vì M11 → M02 → M03 nên M03 không gọi M11 trực tiếp
 * (research W-02). Thiếu bean thì lệnh ghi tăng tài sản mua mới, kết quả bảo trì thuê ngoài có chi phí bị chặn.
 */
public interface ProcurementRequestPort {
    /** FR-008c: yêu cầu Đã duyệt, cùng bộ phận, loại phù hợp (ASSET | SERVICE); rỗng là dùng được. */
    List<UnmetCondition> checkUsable(long requestId, long departmentId, ProcurementType type);

    /** DBR-36: một nguồn gắn đúng một yêu cầu; lỗi khi yêu cầu không còn Đã duyệt hoặc nguồn đã gắn. */
    @Transactional(propagation = Propagation.MANDATORY)
    void linkSource(long requestId, ProcurementSourceType sourceType, long sourceId, long amount);
}

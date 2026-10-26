package com.cambae.backend.m02.api;

import com.cambae.backend.m02.api.dto.ResidencyEndingView;
import com.cambae.backend.m02.api.dto.SettlementInfoView;
import java.util.Optional;

/** Đọc hồ sơ kết thúc cho 017 (tên logic {@code HoSoKetThucDocApi}; contracts mục 1.5b; Q-379). Chỉ đọc. */
public interface ResidencyEndingReadApi {

    /** Hồ sơ kết thúc Đang chuẩn bị của người cao tuổi. */
    Optional<ResidencyEndingView> preparing(long residentId);

    /**
     * Bảng quyết toán (017 FR-034): loại lưu trú, hợp đồng, ngày bắt đầu, ngày kết thúc dự kiến, trường hợp ({@code
     * DEATH} kèm ngày qua đời). Rỗng khi không có hồ sơ kết thúc Đang chuẩn bị, danh sách việc sau qua đời đang mở, hoặc
     * hồ sơ đã ở trạng thái cuối.
     */
    Optional<SettlementInfoView> settlementInfo(long residentId);

    /** Ngoại lệ đã duyệt cho ý tiền của điều kiện (h), căn cứ hồ sơ kết thúc Đang chuẩn bị hoặc danh sách việc sau qua đời (FR-071a). */
    boolean hasApprovedSettlementException(long residentId);
}

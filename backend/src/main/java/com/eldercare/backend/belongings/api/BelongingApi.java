package com.eldercare.backend.belongings.api;

import com.eldercare.backend.belongings.api.dto.BelongingEndingConditionView;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import java.util.List;

/** Đọc đồ gửi ({@code DoGuiApi}; 013 contracts mục 1.2): 004 hiển thị căn cứ điều kiện kết thúc, 016. */
public interface BelongingApi {

    /** FR-025: điều kiện (b) kết thúc lưu trú, tính lúc hỏi. */
    BelongingEndingConditionView endingCondition(long residentId);

    /** SC-008: trạng thái, người giữ, vị trí, bản ghi gần nhất; không gồm đồ bị loại (FR-011a). */
    List<BelongingView> ofResident(long residentId);
}

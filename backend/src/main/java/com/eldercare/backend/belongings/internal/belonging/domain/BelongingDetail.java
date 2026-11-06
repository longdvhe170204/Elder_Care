package com.eldercare.backend.belongings.internal.belonging.domain;

import com.eldercare.backend.belongings.api.dto.BelongingView;
import java.util.List;

/** Chi tiết đồ gửi và lịch sử bàn giao của mọi đồ cùng lần tiếp nhận (FR-011, FR-012). */
public record BelongingDetail(BelongingView belonging, List<HandoverView> handovers) {}

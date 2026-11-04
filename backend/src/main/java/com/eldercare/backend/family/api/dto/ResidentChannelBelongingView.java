package com.eldercare.backend.family.api.dto;

import java.util.Optional;

/**
 * Đồ gửi đang giữ trên kênh người cao tuổi ({@code DoGuiDangGiuView}; FR-083 (e)): tên, trạng thái (Đang giữ, Đang được
 * sử dụng, mã của 013), ảnh. Không có lịch sử bàn giao.
 */
public record ResidentChannelBelongingView(long belongingId, String name, String status, Optional<Long> photoFileId) {}

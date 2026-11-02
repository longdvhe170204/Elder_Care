/**
 * DTO HTTP của yêu cầu phê duyệt (schema {@code ApprovalRequest}, {@code Actor}) dùng chung cho mọi phần trả yêu cầu
 * phê duyệt trong phản hồi, ví dụ 202 của đính chính cách C (000 FR-026). Ngoại lệ có chủ đích với luật "DTO chỉ phục vụ
 * HTTP thuộc {@code web/}" vì được trao đổi giữa các phần (chủ dự án duyệt 2026-10-21).
 * Gói cũng chứa record, hằng trong chữ ký của {@code ApprovalRequestApi} (000 T110).
 */
@NamedInterface("approval-dto")
package com.eldercare.backend.administration.approval.api.dto;

import org.springframework.modulith.NamedInterface;
